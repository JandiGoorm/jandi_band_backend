package com.jandi.band_backend.team.dto;

import com.jandi.band_backend.team.entity.Team;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class ClubTeamResponse {
    private Integer id;
    private String name;
    private String description;
    private Integer creatorId;
    private String creatorName;
    private LocalDateTime createdAt;
    private Integer memberCount;
    private String currentPracticeSong; // 현재 연습 중인 곡 (가장 최근 연습 일정 기준)
    
    public static ClubTeamResponse from(Team team, Integer memberCount, String currentPracticeSong) {
        ClubTeamResponse response = new ClubTeamResponse();
        response.setId(team.getId());
        response.setName(team.getName());
        response.setDescription(team.getDescription());
        response.setCreatorId(team.getCreator().getId());
        response.setCreatorName(team.getCreator().getNickname());
        response.setCreatedAt(team.getCreatedAt());
        response.setMemberCount(memberCount);
        response.setCurrentPracticeSong(currentPracticeSong);
        return response;
    }
} 