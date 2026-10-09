package com.jandi.band_backend.image;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jandi.band_backend.JandiBandBackendApplication;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Docker 전용 OAuth 제공자 응답 및 스토리지 호출 기록. 운영 JAR에 포함되지 않음. */
public final class ImageApiFlowServer {
    private static final Set<String> uploadedKeys = ConcurrentHashMap.newKeySet();
    private static final AtomicBoolean failNextPut = new AtomicBoolean();
    private static final AtomicBoolean failNextDelete = new AtomicBoolean();
    private static final AtomicInteger tokenCalls = new AtomicInteger();
    private static final AtomicInteger userCalls = new AtomicInteger();
    private static final ObjectMapper json = new ObjectMapper();

    public static void main(String[] args) throws Exception {
        if (!"rhythmeet-dev".equals(System.getenv("IMAGE_STORAGE_BUCKET"))) {
            throw new IllegalArgumentException("Only the development R2 bucket is allowed");
        }
        MockWebServer provider = new MockWebServer();
        provider.setDispatcher(new Dispatcher() {
            @Override public MockResponse dispatch(RecordedRequest request) {
                try {
                    String path = request.getRequestUrl().encodedPath();
                    if (path.equals("/fixture/status")) {
                        return response(Map.of("keys", uploadedKeys, "tokenCalls", tokenCalls.get(),
                                "userCalls", userCalls.get()));
                    }
                    if (path.equals("/fixture/fail-next-put")) {
                        failNextPut.set(true);
                        return response(Map.of("armed", true));
                    }
                    if (path.equals("/fixture/fail-next-delete")) {
                        failNextDelete.set(true);
                        return response(Map.of("armed", true));
                    }
                    if (path.equals("/oauth/token")) {
                        tokenCalls.incrementAndGet();
                        String code = form(request.getBody().readUtf8(), "code");
                        if (!code.matches("image-test-[a-z0-9-]+")) {
                            return response(Map.of("error", "invalid_grant")).setResponseCode(400);
                        }
                        return response(Map.of("access_token", code, "refresh_token", "provider-test-only",
                                "expires_in", 3600));
                    }
                    if (path.equals("/v2/user/me")) {
                        userCalls.incrementAndGet();
                        String id = request.getHeader("Authorization").replace("Bearer ", "");
                        return response(Map.of("id", id, "kakao_account", Map.of("profile", Map.of(
                                "nickname", id, "profile_image_url", "https://example.invalid/kakao-profile.png"))));
                    }
                    if (path.equals("/v1/user/unlink")) {
                        return response(Map.of("id", form(request.getBody().readUtf8(), "target_id")));
                    }
                    return new MockResponse().setResponseCode(404);
                } catch (Exception e) {
                    return new MockResponse().setResponseCode(500);
                }
            }
        });
        provider.start(InetAddress.getByName("0.0.0.0"), 9092);
        var options = new ArrayList<>(List.of(
                "--spring.profiles.active=test", "--server.port=8080",
                "--spring.datasource.url=jdbc:mysql://image-api-mysql:3306/image_api_test",
                "--spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
                "--spring.datasource.username=image_test", "--spring.datasource.password=image-test-only",
                "--spring.jpa.hibernate.ddl-auto=create-drop", "--spring.sql.init.mode=never",
                "--spring.servlet.multipart.max-file-size=10MB", "--spring.servlet.multipart.max-request-size=11MB",
                "--kakao.token-url=http://127.0.0.1:9092/oauth/token",
                "--kakao.user-info-url=http://127.0.0.1:9092/v2/user/me",
                "--kakao.user-unlink-url=http://127.0.0.1:9092/v1/user/unlink"));
        for (String field : List.of("ACCESS_KEY", "SECRET_KEY", "ENDPOINT", "REGION", "PATH_STYLE",
                "BUCKET", "PUBLIC_URL")) {
            String value = System.getenv("IMAGE_STORAGE_" + field);
            if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing test storage field: " + field);
            options.add("--image.storage." + field.toLowerCase().replace('_', '-') + "=" + value);
        }
        SpringApplication.run(new Class<?>[]{JandiBandBackendApplication.class, StorageRecorder.class},
                options.toArray(String[]::new));
        System.out.println("Image API fixture ready: isolated MySQL/Redis, simulated Kakao, real development R2");
    }

    private static String form(String body, String key) {
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts[0].equals(key)) return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
        }
        return "";
    }

    private static MockResponse response(Object body) throws Exception {
        return new MockResponse().addHeader("Content-Type", "application/json")
                .setBody(json.writeValueAsString(body));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class StorageRecorder {
        @Bean static BeanPostProcessor recordTestUploads() {
            return new BeanPostProcessor() {
                @Override public Object postProcessAfterInitialization(Object bean, String name) {
                    if (!(bean instanceof S3Client client)) return bean;
                    return Proxy.newProxyInstance(S3Client.class.getClassLoader(), new Class<?>[]{S3Client.class},
                            (proxy, method, args) -> {
                                if (method.getName().equals("putObject") && args[0] instanceof PutObjectRequest put) {
                                    uploadedKeys.add(put.key());
                                    if (failNextPut.getAndSet(false)) {
                                        throw S3Exception.builder().statusCode(503).message("Injected test upload failure").build();
                                    }
                                }
                                if (method.getName().equals("deleteObject") && failNextDelete.getAndSet(false)) {
                                    throw S3Exception.builder().statusCode(503).message("Injected test deletion failure").build();
                                }
                                try { return method.invoke(client, args); }
                                catch (InvocationTargetException e) { throw e.getCause(); }
                            });
                }
            };
        }
    }
}
