package com.jandi.band_backend.image;

public class ImageCleanupException extends RuntimeException {
    public ImageCleanupException() {
        super("DB 저장은 완료됐지만 이미지 파일 정리에 실패했습니다. 관리자 확인이 필요합니다.");
    }
}
