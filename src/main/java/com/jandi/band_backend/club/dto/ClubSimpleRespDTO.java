package com.jandi.band_backend.club.dto;

import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.entity.ClubPhoto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubSimpleRespDTO {
    private Integer id;
    private String name;
    private String universityName;
    private Boolean isUnionClub;
    private String photoUrl;
    private Integer memberCount;

    public static ClubSimpleRespDTO fromEntity(Club club, String photoUrl, int memberCount) {
        // 대학 정보와 연합 동아리 여부 설정
        String universityName = null;
        boolean isUnionClub = (club.getUniversity() == null);

        if (!isUnionClub) {
            universityName = club.getUniversity().getName();
        }

        return ClubSimpleRespDTO.builder()
                .id(club.getId())
                .name(club.getName())
                .universityName(universityName)
                .isUnionClub(isUnionClub)
                .photoUrl(photoUrl)
                .memberCount(memberCount)
                .build();
    }
}
