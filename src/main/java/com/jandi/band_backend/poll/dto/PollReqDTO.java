package com.jandi.band_backend.poll.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PollReqDTO {
    @NotBlank(message = "투표 제목은 필수입니다.")
    private String title;
    @NotNull(message = "동아리 ID는 필수입니다.")
    private Integer clubId;
    @NotNull(message = "투표 마감 시간은 필수입니다.")
    @Future(message = "투표 마감 시간은 현재 시간 이후로 설정해야 합니다.")
    private LocalDateTime endDatetime;
}
