package com.jandi.band_backend.team.dto;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TimetableUpdateReqDTO {

    @NotNull(message = "시간표 데이터는 필수입니다.")
    private JsonNode timetableData;
}
