package com.jandi.band_backend.image;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3Service 단위 테스트")
class S3ServiceTest {

    @Mock
    private AmazonS3Client amazonS3Client;

    @InjectMocks
    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        // @Value 주입 대신 리플렉션으로 값 설정
        ReflectionTestUtils.setField(s3Service, "bucket", "test-bucket");
        ReflectionTestUtils.setField(s3Service, "s3Url", "https://test-cdn.example.com");
    }

    @Test
    @DisplayName("이미지 업로드 성공 - URL에 디렉토리명과 UUID가 포함된다")
    void uploadImage_Success_ReturnsUrlWithDirectoryAndUuid() throws IOException {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "original.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );
        when(amazonS3Client.doesBucketExistV2(anyString())).thenReturn(true);

        // When
        String url = s3Service.uploadImage(file, "profile");

        // Then
        assertThat(url).startsWith("https://test-cdn.example.com/profile/");
        assertThat(url).endsWith(".jpg");
        assertThat(url).matches(".*profile/[a-f0-9\\-]{36}\\.jpg"); // UUID 형식 검증
        
        verify(amazonS3Client).putObject(any(PutObjectRequest.class));
    }

    @Test
    @DisplayName("이미지 업로드 시 올바른 Content-Type이 설정된다")
    void uploadImage_SetsCorrectContentType() throws IOException {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.png",
                "image/png",
                "test content".getBytes()
        );
        when(amazonS3Client.doesBucketExistV2(anyString())).thenReturn(true);

        // When
        s3Service.uploadImage(file, "images");

        // Then
        verify(amazonS3Client).putObject(argThat(request -> {
            assertThat(request.getMetadata().getContentType()).isEqualTo("image/png");
            assertThat(request.getMetadata().getContentLength()).isEqualTo(file.getSize());
            return true;
        }));
    }

    @Test
    @DisplayName("이미지 업로드 시 올바른 버킷과 키가 사용된다")
    void uploadImage_UsesCorrectBucketAndKey() throws IOException {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "content".getBytes()
        );
        when(amazonS3Client.doesBucketExistV2(anyString())).thenReturn(true);

        // When
        s3Service.uploadImage(file, "club");

        // Then
        verify(amazonS3Client).putObject(argThat(request -> {
            assertThat(request.getBucketName()).isEqualTo("test-bucket");
            assertThat(request.getKey()).startsWith("club/");
            return true;
        }));
    }

    @Test
    @DisplayName("확장자가 없는 파일은 예외를 발생시킨다")
    void uploadImage_NoExtension_ThrowsException() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test",
                "text/plain",
                "content".getBytes()
        );

        // When & Then
        assertThatThrownBy(() -> s3Service.uploadImage(file, "test"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("잘못된 형식의 파일입니다");

        verify(amazonS3Client, never()).putObject(any(PutObjectRequest.class));
    }

    @Test
    @DisplayName("다양한 이미지 확장자를 지원한다")
    void uploadImage_SupportsVariousExtensions() throws IOException {
        // Given
        String[] extensions = {".jpg", ".jpeg", ".png", ".gif", ".webp"};
        when(amazonS3Client.doesBucketExistV2(anyString())).thenReturn(true);

        for (String ext : extensions) {
            // When
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test" + ext,
                    "image/jpeg",
                    "content".getBytes()
            );
            String url = s3Service.uploadImage(file, "images");

            // Then
            assertThat(url).endsWith(ext);
        }

        verify(amazonS3Client, times(extensions.length)).putObject(any(PutObjectRequest.class));
    }

    @Test
    @DisplayName("파일 삭제 성공 - 올바른 키로 삭제 요청한다")
    void deleteImage_Success_DeletesWithCorrectKey() {
        // Given
        String fileUrl = "https://test-cdn.example.com/profile/12345678-1234-1234-1234-123456789abc.jpg";

        // When
        s3Service.deleteImage(fileUrl);

        // Then
        verify(amazonS3Client).deleteObject(argThat(request -> {
            assertThat(request.getBucketName()).isEqualTo("test-bucket");
            assertThat(request.getKey()).isEqualTo("profile/12345678-1234-1234-1234-123456789abc.jpg");
            return true;
        }));
    }

    @Test
    @DisplayName("파일 삭제 시 URL에서 S3 URL 부분을 제거한다")
    void deleteImage_RemovesS3UrlFromFileUrl() {
        // Given
        String fileUrl = "https://test-cdn.example.com/club/image.jpg";

        // When
        s3Service.deleteImage(fileUrl);

        // Then
        verify(amazonS3Client).deleteObject(argThat(request ->
                request.getKey().equals("club/image.jpg")
        ));
    }

    @Test
    @DisplayName("중첩된 디렉토리 구조도 정상 처리된다")
    void uploadImage_NestedDirectory_Works() throws IOException {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "content".getBytes()
        );
        when(amazonS3Client.doesBucketExistV2(anyString())).thenReturn(true);

        // When
        String url = s3Service.uploadImage(file, "club/gallery/2024");

        // Then
        assertThat(url).contains("club/gallery/2024/");
        verify(amazonS3Client).putObject(argThat(request ->
                request.getKey().startsWith("club/gallery/2024/")
        ));
    }

    @Test
    @DisplayName("S3 업로드 실패 시 RuntimeException을 던진다")
    void uploadImage_S3Fails_ThrowsRuntimeException() {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "content".getBytes()
        );
        when(amazonS3Client.doesBucketExistV2(anyString())).thenReturn(true);
        when(amazonS3Client.putObject(any(PutObjectRequest.class)))
                .thenThrow(new RuntimeException("S3 upload failed"));

        // When & Then
        assertThatThrownBy(() -> s3Service.uploadImage(file, "test"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to upload file to S3");
    }

    @Test
    @DisplayName("빈 파일도 업로드 가능하다")
    void uploadImage_EmptyFile_Works() throws IOException {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );
        when(amazonS3Client.doesBucketExistV2(anyString())).thenReturn(true);

        // When
        String url = s3Service.uploadImage(file, "test");

        // Then
        assertThat(url).isNotEmpty();
        verify(amazonS3Client).putObject(argThat(request ->
                request.getMetadata().getContentLength() == 0
        ));
    }

    @Test
    @DisplayName("대용량 파일도 Content-Length가 정확히 설정된다")
    void uploadImage_LargeFile_SetsCorrectContentLength() throws IOException {
        // Given
        byte[] largeContent = new byte[10 * 1024 * 1024]; // 10MB
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "large.jpg",
                "image/jpeg",
                largeContent
        );
        when(amazonS3Client.doesBucketExistV2(anyString())).thenReturn(true);

        // When
        s3Service.uploadImage(file, "large");

        // Then
        verify(amazonS3Client).putObject(argThat(request ->
                request.getMetadata().getContentLength() == largeContent.length
        ));
    }
}
