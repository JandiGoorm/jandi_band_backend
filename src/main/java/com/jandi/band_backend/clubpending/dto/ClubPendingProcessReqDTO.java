package com.jandi.band_backend.clubpending.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClubPendingProcessReqDTO {

    @NotNull(message = "승인 여부는 필수입니다.")
    private Boolean approve;  // true: 승인, false: 거부
}
