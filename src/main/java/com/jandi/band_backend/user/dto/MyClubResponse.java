package com.jandi.band_backend.user.dto;

import com.jandi.band_backend.club.entity.ClubMember;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MyClubResponse {
    private Integer id;
    private String name;
    private String description;
    private String photoUrl;
    private String universityName;
    private boolean isUnionClub;
    private ClubMember.MemberRole myRole;
    private LocalDateTime joinedAt;
    private Integer memberCount;

    public static MyClubResponse from(ClubMember clubMember, String photoUrl, Integer memberCount) {
        MyClubResponse response = new MyClubResponse();
        response.setId(clubMember.getClub().getId());
        response.setName(clubMember.getClub().getName());
        response.setDescription(clubMember.getClub().getDescription());
        response.setPhotoUrl(photoUrl);
        response.setUniversityName(
            clubMember.getClub().getUniversity() != null ? 
            clubMember.getClub().getUniversity().getName() : null
        );
        response.setUnionClub(clubMember.getClub().getUniversity() == null);
        response.setMyRole(clubMember.getRole());
        response.setJoinedAt(clubMember.getJoinedAt());
        response.setMemberCount(memberCount);
        return response;
    }
} 