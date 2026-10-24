package com.jandi.band_backend.team.dto;

import tools.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TimetableRespDTO {

    private Integer userId;
    private Integer teamId;
    private JsonNode timetableData;
    private LocalDateTime updatedTimetableAt;
}
