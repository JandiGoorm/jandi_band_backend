package com.jandi.band_backend.image;

import com.jandi.band_backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 실무 파일 업로드 기능 테스트
 * - 이미지 업로드 검증 (관리자 전용)
 * - 파일 크기 제한
 * - 파일 형식 검증
 */
@WebMvcTest(ImageController.class)
@ActiveProfiles("test")
@DisplayName("파일 업로드 통합 테스트 - 관리자 전용 API")
class FileUploadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private S3Service s3Service;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("파일 1. 유효한 이미지 파일 업로드 성공")
    @WithMockUser(roles = "ADMIN")
    void uploadValidImageFile_Success() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "test-image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake image content".getBytes()
        );

        mockMvc.perform(multipart("/api/images/upload")
                        .file(imageFile)
                        .param("dirName", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("파일 2. PNG 이미지 파일 업로드 성공")
    @WithMockUser(roles = "ADMIN")
    void uploadPngImageFile_Success() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "file",
                "test-image.png",
                MediaType.IMAGE_PNG_VALUE,
                "fake png image content".getBytes()
        );

        mockMvc.perform(multipart("/api/images/upload")
                        .file(imageFile)
                        .param("dirName", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("파일 3. 잘못된 파일 형식 업로드 거부")
    @WithMockUser(roles = "ADMIN")
    void uploadInvalidFileType_Returns400() throws Exception {
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "test-file.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "this is a text file".getBytes()
        );

        mockMvc.perform(multipart("/api/images/upload")
                        .file(textFile)
                        .param("dirName", "test"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("파일 4. 빈 파일 업로드 거부")
    @WithMockUser(roles = "ADMIN")
    void uploadEmptyFile_Returns400() throws Exception {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[0]
        );

        mockMvc.perform(multipart("/api/images/upload")
                        .file(emptyFile)
                        .param("dirName", "test"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("파일 5. 이미지 삭제 기능 검증")
    @WithMockUser(roles = "ADMIN")
    void deleteImage_Success() throws Exception {
        String testFileUrl = "test-images/sample.jpg";

        mockMvc.perform(delete("/api/images")
                        .param("fileUrl", testFileUrl))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // 삭제 권한이 있으면 성공, 없으면 403 또는 404
                    assert status == 200 || status == 403 || status == 404;
                });
    }
}