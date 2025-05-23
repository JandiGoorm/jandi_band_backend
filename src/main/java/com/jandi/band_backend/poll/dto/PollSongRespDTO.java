package com.jandi.band_backend.poll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollSongRespDTO {
    private Integer id;
    private Integer pollId;
    private String songName;
    private String artistName;
    private String youtubeUrl;
    private String description;
    private Integer suggesterId;
    private String suggesterName;
    private LocalDateTime createdAt;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer cantCount;
    private Integer hajjCount;
    private String userVoteType;
}
