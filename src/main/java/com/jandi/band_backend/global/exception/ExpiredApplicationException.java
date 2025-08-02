package com.jandi.band_backend.global.exception;

public class ExpiredApplicationException extends RuntimeException {
    public ExpiredApplicationException() {
        super("만료된 신청입니다.");
    }
    
    public ExpiredApplicationException(String message) {
        super(message);
    }
}