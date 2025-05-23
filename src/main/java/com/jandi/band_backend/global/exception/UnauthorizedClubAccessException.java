package com.jandi.band_backend.global.exception;

public class UnauthorizedClubAccessException extends RuntimeException {
    public UnauthorizedClubAccessException(String message) {
        super(message);
    }
}
