package com.jandi.band_backend.image;

import com.jandi.band_backend.config.ImageStorageProperties;
import com.jandi.band_backend.config.R2Config;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class R2ClientCompatibilityTest {
    @ParameterizedTest
    @org.junit.jupiter.params.provider.NullSource
    @ValueSource(strings = {""})
    void rejectsMissingEndpointBeforeAnyRequest(String endpoint) {
        var properties = new ImageStorageProperties();
        properties.setAccessKey("test-access-key");
        properties.setSecretKey("test-secret-key");
        properties.setEndpoint(endpoint);
        assertThatThrownBy(() -> new R2Config().s3Client(properties))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {24, 1048576})
    void customEndpointReceivesSignedUnchunkedUploadAndDeleteWithoutAclCalls(int size) throws Exception {
        try (var server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse().setResponseCode(200));
            server.enqueue(new MockResponse().setResponseCode(204));
            var properties = properties(server);
            try (var client = new R2Config().s3Client(properties)) {
                var service = new R2Service(client, properties,
                        new ImageUrls("https://images.example.com", "club-photo/rhythmeet.webp"));
                byte[] content = ImageTestFiles.bytes("png", size);
                var openStreams = new AtomicInteger();
                var file = new MockMultipartFile("file", "image.png", "image/png", content) {
                    @Override
                    public InputStream getInputStream() throws IOException {
                        openStreams.incrementAndGet();
                        return new FilterInputStream(super.getInputStream()) {
                            private boolean closed;
                            @Override public boolean markSupported() { return false; }
                            @Override public void reset() throws IOException { throw new IOException("Cannot reset multipart stream"); }
                            @Override public void close() throws IOException {
                                if (!closed) { closed = true; openStreams.decrementAndGet(); }
                                super.close();
                            }
                        };
                    }
                };
                String url = service.uploadImage(file, "club-photo");
                assertThat(openStreams).hasValue(0);
                var upload = server.takeRequest(3, TimeUnit.SECONDS);
                assertThat(upload).isNotNull();
                assertThat(upload.getMethod()).isEqualTo("PUT");
                assertThat(upload.getPath()).startsWith("/test-bucket/club-photo/");
                assertThat(upload.getHeader("Content-Type")).isEqualTo("image/png");
                assertThat(upload.getHeader("Content-Length")).isEqualTo(String.valueOf(content.length));
                assertThat(upload.getHeader("Transfer-Encoding")).isNull();
                assertThat(upload.getHeader("Content-Encoding")).isNotEqualTo("aws-chunked");
                assertThat(upload.getHeader("Authorization")).contains("/auto/s3/aws4_request");
                assertThat(upload.getBody().readByteArray()).isEqualTo(content);
                service.deleteImage(url);
                var deletion = server.takeRequest(3, TimeUnit.SECONDS);
                assertThat(deletion).isNotNull();
                assertThat(deletion.getMethod()).isEqualTo("DELETE");
                assertThat(deletion.getPath()).isEqualTo(upload.getPath());
                assertThat(server.getRequestCount()).isEqualTo(2);
            }
        }
    }

    @Test
    void storageAccessDeniedRemainsAFailure() throws Exception {
        try (var server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse().setResponseCode(403).setHeader("Content-Type", "application/xml")
                    .setBody("<Error><Code>AccessDenied</Code><Message>denied</Message></Error>"));
            var properties = properties(server);
            try (var client = new R2Config().s3Client(properties)) {
                var service = new R2Service(client, properties,
                        new ImageUrls("https://images.example.com", "club-photo/rhythmeet.webp"));
                assertThatThrownBy(() -> service.uploadImage(
                        new MockMultipartFile("file", "image.jpg", "image/jpeg", ImageTestFiles.bytes("jpeg", 2)), "photo"))
                        .isInstanceOfSatisfying(S3Exception.class, e -> assertThat(e.statusCode()).isEqualTo(403));
                assertThat(server.getRequestCount()).isEqualTo(1);
            }
        }
    }

    private ImageStorageProperties properties(MockWebServer server) {
        var properties = new ImageStorageProperties();
        properties.setAccessKey("test-access-key");
        properties.setSecretKey("test-secret-key");
        properties.setBucket("test-bucket");
        properties.setRegion("auto");
        properties.setEndpoint(server.url("/").toString());
        properties.setPathStyle(true);
        return properties;
    }
}
