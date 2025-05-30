package com.jandi.band_backend.team.dto;

import com.jandi.band_backend.team.entity.TeamEvent;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PracticeScheduleRespDTO {
    private Integer id;
    private Integer teamId;
    private String teamName;
    private String name;
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
    private String noPosition;
    private Integer creatorId;
    private String creatorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PracticeScheduleRespDTO from(TeamEvent teamEvent) {
        PracticeScheduleRespDTO response = new PracticeScheduleRespDTO();
        response.setId(teamEvent.getId());
        response.setTeamId(teamEvent.getTeam().getId());
        response.setTeamName(teamEvent.getTeam().getName());
        response.setName(teamEvent.getName());
        response.setStartDatetime(teamEvent.getStartDatetime());
        response.setEndDatetime(teamEvent.getEndDatetime());
        response.setNoPosition(teamEvent.getNoPosition() != null ? teamEvent.getNoPosition().name() : null);
        response.setCreatorId(teamEvent.getCreator().getId());
        response.setCreatorName(teamEvent.getCreator().getNickname());
        response.setCreatedAt(teamEvent.getCreatedAt());
        response.setUpdatedAt(teamEvent.getUpdatedAt());
        return response;
    }
}
