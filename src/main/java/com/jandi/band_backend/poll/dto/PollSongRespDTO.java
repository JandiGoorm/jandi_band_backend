package com.jandi.band_backend.poll.dto;

import com.jandi.band_backend.global.util.TimeUtil;
import com.jandi.band_backend.poll.entity.PollSong;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

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
    private ZonedDateTime createdAt;
    private Integer likeCount;
    private Integer dislikeCount;
    private Integer cantCount;
    private Integer hajjCount;

    public static PollSongRespDTO fromEntity(PollSong pollSong) {
        return PollSongRespDTO.builder()
                .id(pollSong.getId())
                .pollId(pollSong.getPoll() != null ? pollSong.getPoll().getId() : null)
                .songName(pollSong.getSongName())
                .artistName(pollSong.getArtistName())
                .youtubeUrl(pollSong.getYoutubeUrl())
                .description(pollSong.getDescription())
                .suggesterId(pollSong.getSuggester() != null ? pollSong.getSuggester().getId() : null)
                .suggesterName(pollSong.getSuggester() != null ? pollSong.getSuggester().getNickname() : null)
                .createdAt(TimeUtil.toKST(pollSong.getCreatedAt()))
                .likeCount(calculateVoteCount(pollSong, "LIKE"))
                .dislikeCount(calculateVoteCount(pollSong, "DISLIKE"))
                .cantCount(calculateVoteCount(pollSong, "CANT"))
                .hajjCount(calculateVoteCount(pollSong, "HAJJ"))
                .build();
    }

    private static int calculateVoteCount(PollSong pollSong, String voteMark) {
        return (int) pollSong.getVotes().stream()
                .filter(vote -> vote.getVotedMark().name().equals(voteMark))
                .count();
    }
}
