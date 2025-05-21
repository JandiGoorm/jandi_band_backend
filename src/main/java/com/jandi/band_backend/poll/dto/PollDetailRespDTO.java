package com.jandi.band_backend.poll.dto;

import com.jandi.band_backend.global.util.TimeUtil;
import com.jandi.band_backend.poll.entity.Poll;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PollDetailRespDTO {
    private Integer id;
    private String title;
    private Integer clubId;
    private String clubName;
    private ZonedDateTime startDatetime;
    private ZonedDateTime endDatetime;
    private Integer creatorId;
    private String creatorName;
    private ZonedDateTime createdAt;
    private List<PollSongRespDTO> songs;

    public static PollDetailRespDTO fromEntity(Poll poll, List<PollSongRespDTO> songs) {
        return PollDetailRespDTO.builder()
                .id(poll.getId())
                .title(poll.getTitle())
                .clubId(poll.getClub() != null ? poll.getClub().getId() : null)
                .clubName(poll.getClub() != null ? poll.getClub().getName() : null)
                .startDatetime(TimeUtil.toKST(poll.getStartDatetime()))
                .endDatetime(TimeUtil.toKST(poll.getEndDatetime()))
                .creatorId(poll.getCreator() != null ? poll.getCreator().getId() : null)
                .creatorName(poll.getCreator() != null ? poll.getCreator().getNickname() : null)
                .createdAt(TimeUtil.toKST(poll.getCreatedAt()))
                .songs(songs)
                .build();
    }
}
