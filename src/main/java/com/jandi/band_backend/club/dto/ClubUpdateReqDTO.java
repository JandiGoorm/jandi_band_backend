package com.jandi.band_backend.club.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClubUpdateReqDTO {
    @Size(max = 100, message = "동아리 이름은 100자 이내여야 합니다")
    private String name;
    // universityId가 null이면 연합동아리, 아니면 특정 대학 소속 동아리
    private Integer universityId;
    @Size(max = 255, message = "카카오톡 채팅방 링크는 255자 이내여야 합니다")
    private String chatroomUrl;
    private String description;
    @Size(max = 50, message = "인스타그램 아이디는 50자 이내여야 합니다")
    private String instagramId;
    private String photoUrl;
}
