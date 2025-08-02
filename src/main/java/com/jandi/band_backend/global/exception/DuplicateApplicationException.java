package com.jandi.band_backend.global.exception;

public class DuplicateApplicationException extends RuntimeException {
    public DuplicateApplicationException() {
        super("이미 신청한 동아리입니다.");
    }
    
    public DuplicateApplicationException(String message) {
        super(message);
    }
}