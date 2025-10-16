package com.jandi.band_backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRespDTO {
    @JsonIgnore
    private String accessToken;
    @JsonIgnore
    private String refreshToken;
}
