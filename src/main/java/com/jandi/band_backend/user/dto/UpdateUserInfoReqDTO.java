package com.jandi.band_backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserInfoReqDTO {
    private String nickname;
    private String position;
    private String university;
}
