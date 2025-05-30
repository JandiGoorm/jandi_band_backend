package com.jandi.band_backend.club.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubEventDetailRespDTO {
    private Long id;
    private String name;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private List<ParticipantRespDTO> participants;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantRespDTO {
        private Integer userId;
        private String userName;
    }
}
