package com.jandi.band_backend.image;

import com.jandi.band_backend.JandiBandBackendApplication;
import com.jandi.band_backend.security.jwt.JwtTokenProvider;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserRepository;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.springframework.boot.SpringApplication;

import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** Disposable Docker fixture for host-to-API verification. Never packaged in the application JAR. */
public final class LocalStorageSmokeServer {
    private record StoredObject(byte[] body, String contentType) {}

    public static void main(String[] args) throws Exception {
        Map<String, StoredObject> objects = new ConcurrentHashMap<>();
        objects.put("/test-bucket/club-photo/rhythmeet.webp",
                new StoredObject(new byte[]{1, 2, 3}, "image/webp"));
        MockWebServer storage = new MockWebServer();
        storage.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                String key = request.getRequestUrl().uri().getPath();
                return switch (request.getMethod()) {
                    case "PUT" -> {
                        objects.put(key, new StoredObject(request.getBody().readByteArray(),
                                request.getHeader("Content-Type")));
                        yield new MockResponse().setResponseCode(200).addHeader("ETag", "\"smoke\"");
                    }
                    case "GET", "HEAD" -> {
                        StoredObject object = objects.get(key);
                        yield object == null ? new MockResponse().setResponseCode(404)
                                : new MockResponse().addHeader("Content-Type", object.contentType())
                                        .setBody(new Buffer().write(object.body()));
                    }
                    case "DELETE" -> {
                        objects.remove(key);
                        yield new MockResponse().setResponseCode(204);
                    }
                    default -> new MockResponse().setResponseCode(405);
                };
            }
        });
        storage.start(InetAddress.getByName("0.0.0.0"), 9091);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { storage.close(); } catch (Exception ignored) { /* process is stopping */ }
        }));
        var options = new ArrayList<>(List.of(
                "--spring.profiles.active=test",
                "--server.port=8080",
                "--image.storage.endpoint=http://127.0.0.1:9091",
                "--image.storage.region=auto",
                "--image.storage.path-style=true",
                "--image.storage.public-url=http://localhost:19091/test-bucket",
                "--image.storage.legacy-public-urls=https://old-images.example.com"));
        if (List.of(args).contains("--external-r2")) {
            String bucket = System.getenv("IMAGE_STORAGE_BUCKET");
            if (bucket == null || !bucket.endsWith("-dev")) {
                throw new IllegalArgumentException("External smoke requires a dedicated development bucket");
            }
            options.removeIf(value -> value.startsWith("--image.storage."));
            for (String field : List.of("ACCESS_KEY", "SECRET_KEY", "ENDPOINT", "REGION", "PATH_STYLE",
                    "BUCKET", "PUBLIC_URL", "LEGACY_PUBLIC_URLS")) {
                String value = System.getenv("IMAGE_STORAGE_" + field);
                if (value == null || value.isBlank()) {
                    throw new IllegalArgumentException("Missing storage smoke configuration: " + field);
                }
                options.add("--image.storage." + field.toLowerCase().replace('_', '-') + "=" + value);
            }
        }
        var context = SpringApplication.run(JandiBandBackendApplication.class, options.toArray(String[]::new));
        Users admin = new Users();
        admin.setKakaoOauthId("local-storage-smoke");
        admin.setNickname("Storage smoke");
        admin.setAdminRole(Users.AdminRole.ADMIN);
        admin.setIsRegistered(true);
        context.getBean(UserRepository.class).saveAndFlush(admin);
        String token = context.getBean(JwtTokenProvider.class).generateAccessToken(admin.getKakaoOauthId());
        Path tokenFile = Path.of("/tmp/storage-smoke-token");
        Files.createFile(tokenFile, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        Files.writeString(tokenFile, token);
        System.out.println("Storage smoke fixture ready; test-only token stored in /tmp/storage-smoke-token");
    }
}
