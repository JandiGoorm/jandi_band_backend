package com.jandi.band_backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRespDTO {
    private String accessToken;
    private String refreshToken;
}
