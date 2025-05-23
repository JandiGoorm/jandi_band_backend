package com.jandi.band_backend.global.exception;

public class VoteAlreadyExistsException extends RuntimeException {
    public VoteAlreadyExistsException() {
        super("이미 해당 노래에 대한 같은 투표가 존재합니다.");
    }

    public VoteAlreadyExistsException(String message) {
        super(message);
    }
}
