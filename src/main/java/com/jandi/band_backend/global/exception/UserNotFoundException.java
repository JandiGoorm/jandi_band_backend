package com.jandi.band_backend.global.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException() {
        super("존재하지 않는 사용자입니다");
    }

    public UserNotFoundException(String message) {
        super(message);
    }
}
