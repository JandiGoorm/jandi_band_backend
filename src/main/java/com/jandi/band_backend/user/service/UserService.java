package com.jandi.band_backend.user.service;

import com.jandi.band_backend.global.exception.UserNotFoundException;
import com.jandi.band_backend.univ.entity.University;
import com.jandi.band_backend.univ.repository.UniversityRepository;
import com.jandi.band_backend.user.dto.UpdateUserInfoReqDTO;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;

    /// 사용자 조회 (카카오 ID 기반) - 인증/로그인 시 사용자 찾기 전용
    @Transactional(readOnly = true)
    @Cacheable(value = "userInfo", key = "'kakao_' + #kakaoOauthId")
    public Users getMyInfoByKakaoId(String kakaoOauthId) {
        return userRepository.findByKakaoOauthId(kakaoOauthId)
                .orElseThrow(UserNotFoundException::new);
    }

    /// 사용자 조회 (userId 기반) - 내부 비즈니스 로직에서 사용
    @Transactional(readOnly = true)
    @Cacheable(value = "userInfo", key = "'user_' + #userId")
    public Users getMyInfo(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
    }

    /// 내 기본 정보 수정 (userId 기반)
    @Transactional
    @CacheEvict(value = {"userInfo", "myPages"}, key = "'user_' + #userId")
    public Integer updateMyInfo(Integer userId, UpdateUserInfoReqDTO updateDTO) {
        Users user = getMyInfo(userId);
        return updateUser(user, updateDTO);
    }

    /// 내부 메서드
    // 유저 정보 수정
    private Integer updateUser(Users user, UpdateUserInfoReqDTO updateDTO) {
        String newNickName = updateDTO.getNickname();
        University newUniversity = universityRepository.findByName(updateDTO.getUniversity());
        Users.Position newPosition = Users.Position.from(updateDTO.getPosition());

        Integer mask = 0;
        // 있는 정보만 수정, 없다면 오류는 내지 않되 반영하지 않음
        if (newNickName != null && !newNickName.isEmpty()) {
            user.setNickname(newNickName);
            mask += 1;
        }
        if (newUniversity != null) {
            user.setUniversity(newUniversity);
            mask += 10;
        }
        if (newPosition != null) {
            user.setPosition(newPosition);
            mask += 100;
        }
        userRepository.save(user);
        return mask;
    }
}
