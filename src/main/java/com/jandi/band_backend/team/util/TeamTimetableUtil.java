package com.jandi.band_backend.team.util;

import tools.jackson.databind.JsonNode;
import com.jandi.band_backend.global.util.TimetableValidationUtil;
import com.jandi.band_backend.team.dto.TimetableUpdateReqDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeamTimetableUtil {
    private final TimetableValidationUtil timetableValidationUtil;

    // String 형식의 시간표 데이터를 JSON으로 변환
    public JsonNode stringToJson(String stringData) {
        return timetableValidationUtil.stringToJson(stringData);
    }

    // TimetableUpdateReqDTO 검사
    public void validateTimetableRequest(TimetableUpdateReqDTO requestDTO) {
        // DTO 검증
        JsonNode timetableData = requestDTO.getTimetableData();

        // 시간표 형식 검증 (공통 유틸리티 사용)
        timetableValidationUtil.validateTimetableData(timetableData);
    }
} 