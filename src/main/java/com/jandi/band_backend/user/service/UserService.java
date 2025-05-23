package com.jandi.band_backend.user.service;

import com.jandi.band_backend.global.exception.UserNotFoundException;
import com.jandi.band_backend.univ.entity.University;
import com.jandi.band_backend.univ.repository.UniversityRepository;
import com.jandi.band_backend.user.dto.UpdateUserInfoReqDTO;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;

    /// 내 기본 정보 조회
    public Users getMyInfo(String kakaoOauthId) {
        return userRepository.findByKakaoOauthId(kakaoOauthId)
                .orElseThrow(UserNotFoundException::new);
    }

    /// 내 기본 정보 수정
    public void updateMyInfo(String kakaoOauthId, UpdateUserInfoReqDTO updateDTO) {
        Users user = getMyInfo(kakaoOauthId);
        updateUser(user, updateDTO);
    }

    /// 내부 메서드
    // 유저 정보 수정
    private void updateUser(Users user, UpdateUserInfoReqDTO updateDTO) {
        String newNickName = updateDTO.getNickname();
        University newUniversity = universityRepository.findByName(updateDTO.getUniversity());
        Users.Position newPosition = Users.Position.from(updateDTO.getPosition());

        // 있는 정보만 수정, 없다면 오류는 내지 않되 반영하지 않음
        if (newNickName != null && !newNickName.isEmpty()) user.setNickname(newNickName);
        if (newUniversity != null) user.setUniversity(newUniversity);
        if (newPosition != null) user.setPosition(newPosition);
        userRepository.save(user);
    }
}
