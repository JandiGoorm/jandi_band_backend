package com.jandi.band_backend.auth.dto;

import lombok.Getter;

@Getter
public class LoginRespDTO extends TokenRespDTO{
    private final Boolean isRegistered;

    public LoginRespDTO(String accessToken, String refreshToken, Boolean isRegistered) {
        super(accessToken, refreshToken);
        this.isRegistered = isRegistered;
    }
}
