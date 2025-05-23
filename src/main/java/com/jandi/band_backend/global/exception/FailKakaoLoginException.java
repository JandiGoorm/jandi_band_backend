package com.jandi.band_backend.global.exception;

import java.util.Map;

public class FailKakaoLoginException extends RuntimeException {
    public FailKakaoLoginException(String message) {
        super(message);
    }

    public FailKakaoLoginException(Map errorBody) {
        String kakaoErrorCode = (String) errorBody.get("error_code");
        String kakaoErrorDesc = (String) errorBody.get("error_description");

        String message = kakaoErrorCode + ": " + kakaoErrorDesc;
        throw new FailKakaoLoginException(message);
    }
}
