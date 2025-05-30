package com.jandi.band_backend.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PracticeScheduleReqDTO {
    @NotNull(message = "팀 ID는 필수입니다")
    private Integer teamId;

    @NotBlank(message = "연습 일정명은 필수입니다")
    @Size(max = 255, message = "연습 일정명은 255자를 초과할 수 없습니다")
    private String name;

    @NotNull(message = "연습 시작 일시는 필수입니다")
    private LocalDateTime startDatetime;

    @NotNull(message = "연습 종료 일시는 필수입니다")
    private LocalDateTime endDatetime;

    private String noPosition; // 연습에서 제외되는 포지션
}
