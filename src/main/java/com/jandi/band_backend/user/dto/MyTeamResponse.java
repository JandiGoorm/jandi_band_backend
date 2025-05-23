package com.jandi.band_backend.user.dto;

import com.jandi.band_backend.team.entity.TeamMember;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MyTeamResponse {
    private Integer id;
    private String name;
    private String description;
    private Integer clubId;
    private String clubName;
    private Integer creatorId;
    private String creatorName;
    private LocalDateTime joinedAt;
    private LocalDateTime createdAt;
    private Integer memberCount;

    public static MyTeamResponse from(TeamMember teamMember, Integer memberCount) {
        MyTeamResponse response = new MyTeamResponse();
        response.setId(teamMember.getTeam().getId());
        response.setName(teamMember.getTeam().getName());
        response.setDescription(teamMember.getTeam().getDescription());
        response.setClubId(teamMember.getTeam().getClub().getId());
        response.setClubName(teamMember.getTeam().getClub().getName());
        response.setCreatorId(teamMember.getTeam().getCreator().getId());
        response.setCreatorName(teamMember.getTeam().getCreator().getNickname());
        response.setJoinedAt(teamMember.getJoinedAt());
        response.setCreatedAt(teamMember.getTeam().getCreatedAt());
        response.setMemberCount(memberCount);
        return response;
    }
} 