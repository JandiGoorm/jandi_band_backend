package com.jandi.band_backend.global.exception;

public class AlreadyProcessedException extends RuntimeException {
    public AlreadyProcessedException() {
        super("이미 처리된 신청입니다.");
    }
    
    public AlreadyProcessedException(String message) {
        super(message);
    }
}