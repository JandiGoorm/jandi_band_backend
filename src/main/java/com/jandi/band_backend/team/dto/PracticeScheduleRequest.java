package com.jandi.band_backend.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PracticeScheduleRequest {
    @NotNull(message = "팀 ID는 필수입니다")
    private Integer teamId;

    @NotBlank(message = "곡 제목은 필수입니다")
    @Size(max = 100, message = "곡 제목은 100자를 초과할 수 없습니다")
    private String songName;

    @Size(max = 100, message = "아티스트명은 100자를 초과할 수 없습니다")
    private String artistName;

    @Size(max = 500, message = "YouTube URL은 500자를 초과할 수 없습니다")
    private String youtubeUrl;

    @NotNull(message = "연습 시작 일시는 필수입니다")
    private LocalDateTime startDatetime;

    @NotNull(message = "연습 종료 일시는 필수입니다")
    private LocalDateTime endDatetime;

    @Size(max = 255, message = "장소는 255자를 초과할 수 없습니다")
    private String location;

    @Size(max = 255, message = "주소는 255자를 초과할 수 없습니다")
    private String address;

    private String additionalDescription; // 추가 설명
} 