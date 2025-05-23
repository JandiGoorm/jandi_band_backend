package com.jandi.band_backend.club.dto;

import com.jandi.band_backend.univ.dto.UniversityRespDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubDetailRespDTO {
    private Integer id;
    private String name;
    private UniversityRespDTO university;
    private Boolean isUnionClub;
    private String chatroomUrl;
    private String description;
    private String instagramId;
    private String photoUrl;
    private Integer memberCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
