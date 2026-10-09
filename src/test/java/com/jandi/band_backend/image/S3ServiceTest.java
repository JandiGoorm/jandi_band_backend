package com.jandi.band_backend.image;

import com.jandi.band_backend.config.ImageStorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3ServiceTest {
    @Mock private S3Client client;
    private S3Service service;

    @BeforeEach
    void setUp() {
        var properties = new ImageStorageProperties();
        properties.setBucket("test-bucket");
        service = new S3Service(client, properties, new ImageUrls("https://images.example.com",
                List.of("https://old-s3.example.com"), "club-photo/rhythmeet.webp"));
    }

    @ParameterizedTest
    @CsvSource({
            "jpg,image/jpeg,12,profile",
            "jpeg,image/jpeg,0,images",
            "png,image/png,10485760,large",
            "gif,image/gif,24,club/gallery",
            "webp,image/webp,24,club-photo"
    })
    void uploadsExactBytesAndMetadata(String extension, String contentType, int size, String directory) throws Exception {
        byte[] content = new byte[size];
        var file = new MockMultipartFile("file", "photo." + extension, contentType, content);
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenAnswer(invocation -> {
            PutObjectRequest request = invocation.getArgument(0);
            RequestBody body = invocation.getArgument(1);
            assertThat(request.bucket()).isEqualTo("test-bucket");
            assertThat(request.key()).matches(directory + "/[a-f0-9-]{36}\\." + extension);
            assertThat(request.contentType()).isEqualTo(contentType);
            assertThat(request.contentLength()).isEqualTo(size);
            try (var input = body.contentStreamProvider().newStream()) {
                assertThat(input.readAllBytes()).isEqualTo(content);
            }
            return null;
        });

        assertThat(service.uploadImage(file, directory))
                .startsWith("https://images.example.com/" + directory + "/").endsWith("." + extension);
        verify(client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verifyNoMoreInteractions(client);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"no-extension", "trailing.", "file.jpg/other", "file.j?g"})
    void rejectsInvalidFilenamesBeforeStorage(String filename) {
        var file = new MockMultipartFile("file", filename, "image/jpeg", new byte[1]);
        assertThatThrownBy(() -> service.uploadImage(file, "profile")).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(client);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "../photos", "/photos", "photos//nested", "photos/./nested", "photos\\nested"})
    void rejectsUnsafeDirectoriesBeforeStorage(String directory) {
        var file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[1]);
        assertThatThrownBy(() -> service.uploadImage(file, directory)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(client);
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://images.example.com", "https://old-s3.example.com"})
    void deletesRecognizedUrlsFromActiveBucket(String base) {
        service.deleteImage(base + "/club/photo%20%2B%ED%95%9C%EA%B8%80.jpg");
        verify(client).deleteObject(argThat((DeleteObjectRequest request) ->
                request.bucket().equals("test-bucket") && request.key().equals("club/photo +한글.jpg")));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "https://k.kakaocdn.net/profile.jpg",
            "https://images.example.com.evil.test/photo.jpg", "https://images.example.com/../photo.jpg",
            "https://images.example.com/photo.jpg?other=1", "https://images.example.com/club-photo/rhythmeet.webp",
            "https://old-s3.example.com/club-photo/rhythmeet.webp"})
    void neverDeletesForeignOrSharedDefaultImages(String url) {
        service.deleteImage(url);
        verifyNoInteractions(client);
    }

    @Test
    void preservesOriginalStorageFailures() {
        var failure = new IllegalStateException("storage unavailable");
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(failure);
        var file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[1]);
        assertThatThrownBy(() -> service.uploadImage(file, "profile")).isSameAs(failure);
        when(client.deleteObject(any(DeleteObjectRequest.class))).thenThrow(failure);
        assertThatThrownBy(() -> service.deleteImage("https://images.example.com/profile/photo.jpg")).isSameAs(failure);
    }
}
