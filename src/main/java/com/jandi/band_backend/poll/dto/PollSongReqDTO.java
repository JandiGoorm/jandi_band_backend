package com.jandi.band_backend.poll.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PollSongReqDTO {
    @NotBlank(message = "곡 제목은 필수입니다.")
    private String songName;
    @NotBlank(message = "가수(밴드명)은 필수입니다.")
    private String artistName;
    @NotBlank(message = "유튜브 링크는 필수입니다.")
    private String youtubeUrl;
    private String description;
}
