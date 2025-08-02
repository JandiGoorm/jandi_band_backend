package com.jandi.band_backend.global.exception;

public class InvalidPendingStatusException extends RuntimeException {
    public InvalidPendingStatusException() {
        super("잘못된 상태 변환입니다.");
    }
    
    public InvalidPendingStatusException(String message) {
        super(message);
    }
}