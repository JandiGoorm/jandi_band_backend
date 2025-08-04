package com.jandi.band_backend.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "공지사항 생성/수정 요청 DTO")
public class NoticeReqDTO {

    @Schema(description = "공지사항 제목", example = "사이트 점검 안내", required = true, maxLength = 255)
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 255, message = "제목은 255자를 초과할 수 없습니다")
    private String title;

    @Schema(description = "공지사항 내용", example = "오늘 밤 12시부터 새벽 2시까지 사이트 점검이 있습니다.", required = false)
    private String content;

    @Schema(description = "팝업 노출 시작 시각", example = "2024-12-10T00:00:00", required = true)
    @NotNull(message = "노출 시작 시각은 필수입니다")
    private LocalDateTime startDatetime;

    @Schema(description = "팝업 노출 종료 시각", example = "2024-12-10T23:59:59", required = true)
    @NotNull(message = "노출 종료 시각은 필수입니다")
    private LocalDateTime endDatetime;

    @Schema(description = "일시정지 여부 (생성 시 생략하면 자동으로 false 설정)", example = "false", required = false)
    private Boolean isPaused = false;

    @Schema(description = "첨부 이미지 파일", required = false)
    private MultipartFile image;
}
