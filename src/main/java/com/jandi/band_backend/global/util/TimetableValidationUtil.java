package com.jandi.band_backend.global.util;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Component
public class TimetableValidationUtil {
    private static final Set<String> WEEKDAYS = Set.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");

    // String 형식의 시간표 데이터를 JSON으로 변환
    public JsonNode stringToJson(String stringData) {
        ObjectMapper objectMapper = tools.jackson.databind.json.JsonMapper.builder().build();
        try {
            return objectMapper.readTree(stringData);
        } catch (Exception e) {
            throw new RuntimeException("JSON 변환에 실패했습니다");
        }
    }

    // JsonNode 시간표 데이터 검증
    public void validateTimetableData(JsonNode timetableData) {
        if (timetableData == null || timetableData.isEmpty()) {
            throw new IllegalArgumentException("시간표 데이터는 공란이 될 수 없습니다.");
        }

        // 시간표 형식 검증
        checkWeekFormIsValid(timetableData);
        checkTimeListFormIsValid(timetableData);
    }

    /// 내부 메서드
    // JSON에 MON~SUN만 있는지 검사
    private void checkWeekFormIsValid(JsonNode jsonNode) {
        // 모든 요일이 있는지 확인
        for (String curWeek : WEEKDAYS) {
            if (!jsonNode.has(curWeek)) {
                throw new IllegalArgumentException("시간표는 모든 요일을 포함해야 합니다: " + curWeek + " 누락되었습니다.");
            }
        }

        // 불필요한 요일이 있는지 확인
        Iterator<String> fieldNames = jsonNode.propertyNames().iterator();
        while (fieldNames.hasNext()) {
            String field = fieldNames.next();
            if (!WEEKDAYS.contains(field)) {
                throw new IllegalArgumentException("시간표에 잘못된 요일 키가 존재합니다: " + field);
            }
        }
    }

    // 각 요일마다 중복 없이 시간 리스트가 들어가있는지 검사
    private void checkTimeListFormIsValid(JsonNode jsonNode) {
        for (String curWeek : WEEKDAYS) {
            // 특정 요일의 시간 데이터가 배열 형식인지 확인
            JsonNode timeListNode = jsonNode.get(curWeek);
            if (!timeListNode.isArray()) {
                throw new IllegalArgumentException(curWeek + "의 시간 정보가 배열이 아닙니다.");
            }

            // 특정 요일에 중복된 시간 데이터가 있는지 확인
            Set<String> seenTimes = new HashSet<>();
            for (JsonNode timeNode : timeListNode) {
                String timeStr = checkTimeFormIsValid(timeNode);// 시간 형식이 올바른지 검사
                if (!seenTimes.add(timeStr)) {
                    throw new IllegalArgumentException(curWeek + "에 중복된 시간 " + timeNode.asText() + "이 존재합니다.");
                }
            }
        }
    }

    // 시간 형식이 HH:MM(MM==00 or 30)을 지켰는지 검사
    private String checkTimeFormIsValid(JsonNode timeNode) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        // 시간 형식이 HH:MM인지 확인
        LocalTime convertedTime;
        try {
            convertedTime = LocalTime.parse(timeNode.asText(), timeFormatter);
        } catch (Exception e) {
            throw new IllegalArgumentException(timeNode.asText() + "은 HH:mm (HH: 00~24, mm: 00, 30)형식의 문자열이어야 합니다.");
        }

        // MM이 30분 단위(00 or 30)인지 확인
        int minute = convertedTime.getMinute();
        if (minute != 0 && minute != 30) {
            throw new IllegalArgumentException(timeNode.asText() + "은 30분 단위여야 합니다.");
        }
        return timeNode.asText();
    }
}
