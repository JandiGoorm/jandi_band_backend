package com.jandi.band_backend.global.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CommonRespDTO 테스트")
class CommonRespDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("success() 메서드는 success=true와 데이터를 반환한다")
    void success_WithData_ReturnsSuccessTrue() {
        // Given
        String testData = "테스트 데이터";

        // When
        CommonRespDTO<String> response = CommonRespDTO.success("성공", testData);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("성공");
        assertThat(response.getData()).isEqualTo(testData);
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    @DisplayName("success() 메서드는 데이터 없이도 생성 가능하다")
    void success_WithoutData_ReturnsSuccessTrue() {
        // When
        CommonRespDTO<Void> response = CommonRespDTO.success("성공");

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isEqualTo("성공");
        assertThat(response.getData()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    @Test
    @DisplayName("error() 메서드는 success=false와 errorCode를 반환한다")
    void error_ReturnsSuccessFalseWithErrorCode() {
        // When
        CommonRespDTO<?> response = CommonRespDTO.error("에러 발생", "USER_NOT_FOUND");

        // Then
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getMessage()).isEqualTo("에러 발생");
        assertThat(response.getErrorCode()).isEqualTo("USER_NOT_FOUND");
        assertThat(response.getData()).isNull();
    }

    @Test
    @DisplayName("JSON 직렬화 시 필드명이 정확히 매핑된다")
    void jsonSerialization_FieldNamesCorrect() throws Exception {
        // Given
        CommonRespDTO<Integer> response = CommonRespDTO.success("성공", 123);

        // When
        String json = objectMapper.writeValueAsString(response);

        // Then
        assertThat(json).contains("\"success\":true");
        assertThat(json).contains("\"message\":\"성공\"");
        assertThat(json).contains("\"data\":123");
        assertThat(json).doesNotContain("\"errorCode\""); // null이므로 포함되지 않음 (@JsonInclude)
    }

    @Test
    @DisplayName("에러 응답 JSON 직렬화 시 errorCode가 포함된다")
    void errorJsonSerialization_IncludesErrorCode() throws Exception {
        // Given
        CommonRespDTO<?> response = CommonRespDTO.error("에러", "INVALID_REQUEST");

        // When
        String json = objectMapper.writeValueAsString(response);

        // Then
        assertThat(json).contains("\"success\":false");
        assertThat(json).contains("\"message\":\"에러\"");
        assertThat(json).contains("\"errorCode\":\"INVALID_REQUEST\"");
        assertThat(json).doesNotContain("\"data\""); // null이므로 포함되지 않음
    }

    @Test
    @DisplayName("복잡한 객체도 data로 담을 수 있다")
    void success_WithComplexObject_Works() {
        // Given
        TestDto testDto = new TestDto("테스트", 100);

        // When
        CommonRespDTO<TestDto> response = CommonRespDTO.success("성공", testDto);

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().getName()).isEqualTo("테스트");
        assertThat(response.getData().getValue()).isEqualTo(100);
    }

    @Test
    @DisplayName("null 메시지로도 생성 가능하다")
    void success_WithNullMessage_Works() {
        // When
        CommonRespDTO<String> response = CommonRespDTO.success(null, "데이터");

        // Then
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getData()).isEqualTo("데이터");
    }

    // 테스트용 DTO
    private static class TestDto {
        private final String name;
        private final int value;

        public TestDto(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }
}
