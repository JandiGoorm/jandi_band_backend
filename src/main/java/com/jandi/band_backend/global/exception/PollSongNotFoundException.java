package com.jandi.band_backend.global.exception;

public class PollSongNotFoundException extends RuntimeException {
    public PollSongNotFoundException() {
        super("해당 투표 노래를 찾을 수 없습니다.");
    }

    public PollSongNotFoundException(String message) {
        super(message);
    }
}
