package com.jandi.band_backend.global.exception;

// 잘못된 접근, 권한없는 접근 등
public class InvalidAccessException extends RuntimeException {
    public InvalidAccessException(String message) {
        super(message);
    }
}
