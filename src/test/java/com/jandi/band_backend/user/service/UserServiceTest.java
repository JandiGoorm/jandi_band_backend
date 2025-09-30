package com.jandi.band_backend.user.service;

import com.jandi.band_backend.global.exception.UserNotFoundException;
import com.jandi.band_backend.univ.entity.University;
import com.jandi.band_backend.univ.repository.UniversityRepository;
import com.jandi.band_backend.user.dto.UpdateUserInfoReqDTO;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 사용자 관리 테스트")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UniversityRepository universityRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("카카오 ID로 사용자 조회 성공")
    void getMyInfoByKakaoId_Success() {
        // Given
        String kakaoOauthId = "kakao123";
        Users mockUser = createMockUser();

        when(userRepository.findByKakaoOauthId(kakaoOauthId)).thenReturn(Optional.of(mockUser));

        // When
        Users result = userService.getMyInfoByKakaoId(kakaoOauthId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getKakaoOauthId()).isEqualTo(kakaoOauthId);
        assertThat(result.getNickname()).isEqualTo("테스트 사용자");

        verify(userRepository).findByKakaoOauthId(kakaoOauthId);
    }

    @Test
    @DisplayName("카카오 ID로 사용자 조회 실패 - 존재하지 않는 사용자")
    void getMyInfoByKakaoId_UserNotFound() {
        // Given
        String kakaoOauthId = "nonexistent";

        when(userRepository.findByKakaoOauthId(kakaoOauthId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getMyInfoByKakaoId(kakaoOauthId))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findByKakaoOauthId(kakaoOauthId);
    }

    @Test
    @DisplayName("사용자 ID로 사용자 조회 성공")
    void getMyInfo_Success() {
        // Given
        Integer userId = 1;
        Users mockUser = createMockUser();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        // When
        Users result = userService.getMyInfo(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(userId);
        assertThat(result.getNickname()).isEqualTo("테스트 사용자");

        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("사용자 ID로 사용자 조회 실패 - 존재하지 않는 사용자")
    void getMyInfo_UserNotFound() {
        // Given
        Integer userId = 999;

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getMyInfo(userId))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(userId);
    }

    @Test
    @DisplayName("사용자 정보 수정 성공 - 모든 필드 수정")
    void updateMyInfo_Success_AllFields() {
        // Given
        Integer userId = 1;
        UpdateUserInfoReqDTO updateDTO = new UpdateUserInfoReqDTO();
        updateDTO.setNickname("수정된 닉네임");
        updateDTO.setUniversity("수정된 대학교");
        updateDTO.setPosition("GUITAR");

        Users mockUser = createMockUser();
        University mockUniversity = createMockUniversity();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(universityRepository.findByName("수정된 대학교")).thenReturn(mockUniversity);
        when(userRepository.save(any(Users.class))).thenReturn(mockUser);

        // When
        userService.updateMyInfo(userId, updateDTO);

        // Then
        verify(userRepository).findById(userId);
        verify(universityRepository).findByName("수정된 대학교");
        verify(userRepository).save(mockUser);

        // 실제 업데이트가 되었는지 확인 (mockUser의 상태가 변경되었는지)
        assertThat(mockUser.getNickname()).isEqualTo("수정된 닉네임");
        assertThat(mockUser.getUniversity()).isEqualTo(mockUniversity);
        assertThat(mockUser.getPosition()).isEqualTo(Users.Position.GUITAR);
    }

    @Test
    @DisplayName("사용자 정보 수정 성공 - 닉네임만 수정")
    void updateMyInfo_Success_OnlyNickname() {
        // Given
        Integer userId = 1;
        UpdateUserInfoReqDTO updateDTO = new UpdateUserInfoReqDTO();
        updateDTO.setNickname("수정된 닉네임");
        // university와 position은 null

        Users mockUser = createMockUser();
        University originalUniversity = mockUser.getUniversity();
        Users.Position originalPosition = mockUser.getPosition();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(universityRepository.findByName(null)).thenReturn(null);
        when(userRepository.save(any(Users.class))).thenReturn(mockUser);

        // When
        userService.updateMyInfo(userId, updateDTO);

        // Then
        verify(userRepository).findById(userId);
        verify(userRepository).save(mockUser);

        // 닉네임만 변경되고 나머지는 그대로인지 확인
        assertThat(mockUser.getNickname()).isEqualTo("수정된 닉네임");
        assertThat(mockUser.getUniversity()).isEqualTo(originalUniversity);
        assertThat(mockUser.getPosition()).isEqualTo(originalPosition);
    }

    @Test
    @DisplayName("사용자 정보 수정 성공 - 빈 문자열 필드 무시")
    void updateMyInfo_Success_IgnoreEmptyFields() {
        // Given
        Integer userId = 1;
        UpdateUserInfoReqDTO updateDTO = new UpdateUserInfoReqDTO();
        updateDTO.setNickname(""); // 빈 문자열
        updateDTO.setUniversity("null"); // "null" 문자열
        updateDTO.setPosition("INVALID"); // 잘못된 포지션

        Users mockUser = createMockUser();
        String originalNickname = mockUser.getNickname();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(universityRepository.findByName("null")).thenReturn(null);
        when(userRepository.save(any(Users.class))).thenReturn(mockUser);

        // When
        userService.updateMyInfo(userId, updateDTO);

        // Then
        verify(userRepository).findById(userId);
        verify(userRepository).save(mockUser);

        // 빈 문자열과 잘못된 값들은 무시되어야 함
        assertThat(mockUser.getNickname()).isEqualTo(originalNickname); // 변경되지 않음
    }

    @Test
    @DisplayName("사용자 정보 수정 실패 - 존재하지 않는 사용자")
    void updateMyInfo_UserNotFound() {
        // Given
        Integer userId = 999;
        UpdateUserInfoReqDTO updateDTO = new UpdateUserInfoReqDTO();
        updateDTO.setNickname("수정된 닉네임");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateMyInfo(userId, updateDTO))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(userId);
        verify(userRepository, never()).save(any());
    }

    // Helper methods for creating mock objects
    private Users createMockUser() {
        Users user = new Users();
        user.setId(1);
        user.setKakaoOauthId("kakao123");
        user.setNickname("테스트 사용자");
        user.setUniversity(createMockUniversity());
        user.setPosition(Users.Position.VOCAL);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        return user;
    }

    private University createMockUniversity() {
        University university = new University();
        university.setId(1);
        university.setName("테스트 대학교");
        return university;
    }
}