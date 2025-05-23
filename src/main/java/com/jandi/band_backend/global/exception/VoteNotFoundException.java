package com.jandi.band_backend.global.exception;

public class VoteNotFoundException extends RuntimeException {
    public VoteNotFoundException() {
        super("해당 투표를 찾을 수 없습니다.");
    }

    public VoteNotFoundException(String message) {
        super(message);
    }
}
