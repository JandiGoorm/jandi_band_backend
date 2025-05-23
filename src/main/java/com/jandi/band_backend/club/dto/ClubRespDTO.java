package com.jandi.band_backend.club.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubRespDTO {
    private Integer id;
    private String name;
    private String universityName;
    private Boolean isUnionClub;
    private String photoUrl;
    private Integer memberCount;
}
