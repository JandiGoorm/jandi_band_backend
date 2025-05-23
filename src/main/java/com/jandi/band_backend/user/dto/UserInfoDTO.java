package com.jandi.band_backend.user.dto;

import com.jandi.band_backend.user.entity.UserPhoto;
import com.jandi.band_backend.user.entity.Users;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserInfoDTO {
    private Integer id; // 자체 고유 아이디 (카카오 회원번호 아님)
    private String nickname;
    private String profilePhoto;
    private String position;
    private String university;

    public UserInfoDTO(Users user, UserPhoto userPhoto) {
        id = user.getId();
        nickname = user.getNickname();
        profilePhoto = userPhoto == null ? "" : userPhoto.getImageUrl();
        position = user.getPosition().name();
        university = user.getUniversity().getName();
    }
}
