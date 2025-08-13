package com.jandi.band_backend.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "공지사항 부분 수정 요청 DTO - 모든 필드가 선택적")
public class NoticeUpdateReqDTO {

    @Schema(description = "공지사항 제목 (선택적)", example = "사이트 점검 안내", maxLength = 255)
    @Size(max = 255, message = "제목은 255자를 초과할 수 없습니다")
    private String title;

    @Schema(description = "공지사항 내용 (선택적)", example = "오늘 밤 12시부터 새벽 2시까지 사이트 점검이 있습니다.")
    private String content;

    @Schema(description = "팝업 노출 시작 시각 (선택적)", example = "2024-12-10T00:00:00")
    private LocalDateTime startDatetime;

    @Schema(description = "팝업 노출 종료 시각 (선택적)", example = "2024-12-10T23:59:59")
    private LocalDateTime endDatetime;

    @Schema(description = "첨부 이미지 파일 (선택적)", required = false)
    private MultipartFile image;

    @Schema(description = "이미지 삭제 여부 (true로 설정 시 기존 이미지 삭제)", example = "false", defaultValue = "null")
    private Boolean deleteImage;
}
