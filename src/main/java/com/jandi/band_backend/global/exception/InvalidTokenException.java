package com.jandi.band_backend.global.exception;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException() {
        super("유효하지 않은 토큰입니다.");
    }
}
