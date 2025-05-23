package com.jandi.band_backend.poll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollRespDTO {
    private Integer id;
    private String title;
    private Integer clubId;
    private String clubName;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private Integer creatorId;
    private String creatorName;
    private LocalDateTime createdAt;
}
