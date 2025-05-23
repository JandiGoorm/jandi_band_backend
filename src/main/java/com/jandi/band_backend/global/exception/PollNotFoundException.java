package com.jandi.band_backend.global.exception;

public class PollNotFoundException extends RuntimeException {
    public PollNotFoundException() {
        super("해당 투표를 찾을 수 없습니다.");
    }

    public PollNotFoundException(String message) {
        super(message);
    }
}
