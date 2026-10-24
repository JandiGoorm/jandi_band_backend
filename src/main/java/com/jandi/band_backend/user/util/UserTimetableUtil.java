package com.jandi.band_backend.user.util;

import tools.jackson.databind.JsonNode;
import com.jandi.band_backend.global.util.TimetableValidationUtil;
import com.jandi.band_backend.user.dto.UserTimetableReqDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserTimetableUtil {
    private final TimetableValidationUtil timetableValidationUtil;

    // String 형식의 시간표 데이터를 JSON으로 변환
    public JsonNode stringToJson(String stringData) {
        return timetableValidationUtil.stringToJson(stringData);
    }

    // UserTimetableReqDTO 검사
    public void validateTimetableRequest(UserTimetableReqDTO requestDTO) {
        // DTO 검증
        String name = requestDTO.getName();
        JsonNode timetableData = requestDTO.getTimetableData();

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("제목은 공란이 될 수 없습니다.");
        }

        // 시간표 형식 검증 (공통 유틸리티 사용)
        timetableValidationUtil.validateTimetableData(timetableData);
    }
} 