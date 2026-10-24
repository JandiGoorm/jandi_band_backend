package com.jandi.band_backend.team.dto;

import tools.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamDetailRespDTO {

    private Integer id;
    private String name;
    private Integer clubId;
    private String clubName;
    private Integer creatorId;
    private String creatorName;
    private List<MemberInfoDTO> members;
    private LocalDateTime suggestedScheduleAt;
    private SubmissionProgressDTO submissionProgress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MemberInfoDTO {
        private Integer userId;
        private String name;
        private String position;
        private LocalDateTime timetableUpdatedAt;
        private Boolean isSubmitted;
        private JsonNode timetableData;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubmissionProgressDTO {
        private Integer submittedMember;
        private Integer totalMember;
    }
}
