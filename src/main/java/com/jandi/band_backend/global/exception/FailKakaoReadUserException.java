package com.jandi.band_backend.global.exception;

import java.util.Map;

public class FailKakaoReadUserException extends RuntimeException {
    public FailKakaoReadUserException(String message) {
        super(message);
    }

    public FailKakaoReadUserException(Map errorBody) {
        String kakaoErrorCode = (String) errorBody.get("error_code");
        String kakaoErrorDesc = (String) errorBody.get("error_description");

        String message = kakaoErrorCode + ": " + kakaoErrorDesc;
        throw new FailKakaoReadUserException(message);
    }
}
