package com.jandi.band_backend.global.exception;

public class PendingNotFoundException extends RuntimeException {
    public PendingNotFoundException() {
        super("신청을 찾을 수 없습니다.");
    }
    
    public PendingNotFoundException(String message) {
        super(message);
    }
}