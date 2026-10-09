package com.jandi.band_backend.global.util;

import com.jandi.band_backend.image.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3FileManagementUtil {
    private final S3Service s3Service;

    /**
     * 안전한 파일 업로드 (예외 처리 포함)
     */
    public String uploadFile(MultipartFile file, String directory, String errorMessage) {
        try {
            return s3Service.uploadImage(file, directory);
        } catch (Exception e) {
            log.error("S3 파일 업로드 실패: directory={}, error={}", directory, e.getMessage());
            throw new RuntimeException(errorMessage + ": " + e.getMessage());
        }
    }

    /**
     * 안전한 파일 삭제 (예외 처리 포함, 카카오 URL 제외)
     */
    public void deleteFileWithKakaoCheck(String fileUrl) {
        deleteFileSafely(fileUrl);
    }

    /**
     * 안전한 파일 삭제 (예외 처리 포함)
     */
    public void deleteFileSafely(String fileUrl) {
        try {
            s3Service.deleteImage(fileUrl);
            log.debug("이미지 정리 요청 처리: {}", fileUrl);
        } catch (Exception e) {
            log.error("S3 파일 삭제 실패: url={}, error={}", fileUrl, e.getMessage());
            // 파일 삭제 실패는 치명적이지 않으므로 예외를 던지지 않음
        }
    }

    /**
     * 조건부 파일 삭제 (기본 URL과 다른 경우에만 삭제)
     */
    public void deleteFileIfNotDefault(String fileUrl, String defaultUrl) {
        if (!fileUrl.equals(defaultUrl)) {
            deleteFileSafely(fileUrl);
        } else {
            log.debug("기본 이미지는 삭제하지 않습니다: {}", fileUrl);
        }
    }

}
