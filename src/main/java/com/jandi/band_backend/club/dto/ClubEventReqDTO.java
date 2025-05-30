package com.jandi.band_backend.club.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubEventReqDTO {
    private String name;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
}
