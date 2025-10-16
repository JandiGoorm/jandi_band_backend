package com.jandi.band_backend.auth.service;

import com.jandi.band_backend.auth.dto.LoginRespDTO;
import com.jandi.band_backend.auth.dto.SignUpReqDTO;
import com.jandi.band_backend.auth.dto.kakao.KakaoUserInfoDTO;
import com.jandi.band_backend.auth.redis.TokenBlacklistService;
import com.jandi.band_backend.auth.service.kakao.KakaoUserService;
import com.jandi.band_backend.club.entity.ClubMember;
import com.jandi.band_backend.club.repository.ClubMemberRepository;
import com.jandi.band_backend.global.exception.InvalidAccessException;
import com.jandi.band_backend.global.exception.UniversityNotFoundException;
import com.jandi.band_backend.global.exception.UserNotFoundException;
import com.jandi.band_backend.security.jwt.JwtTokenProvider;
import com.jandi.band_backend.univ.entity.University;
import com.jandi.band_backend.univ.repository.UniversityRepository;
import com.jandi.band_backend.user.dto.UserInfoDTO;
import com.jandi.band_backend.user.entity.UserPhoto;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserPhotoRepository;
import com.jandi.band_backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPhotoRepository userPhotoRepository;

    @Mock
    private UniversityRepository universityRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private KakaoUserService kakaoUserService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("기존 사용자 로그인 성공")
    void login_ExistingUser_Success() {
        // Given
        KakaoUserInfoDTO kakaoUserInfo = new KakaoUserInfoDTO(
                "12345",
                "테스트사용자",
                "profile.jpg"
        );

        Users existingUser = new Users();
        existingUser.setId(1);
        existingUser.setKakaoOauthId("12345");
        existingUser.setNickname("테스트사용자");
        existingUser.setIsRegistered(true);
        existingUser.setDeletedAt(null);

        String accessToken = "access-token";
        String refreshToken = "refresh-token";

        when(userRepository.findByKakaoOauthId("12345")).thenReturn(Optional.of(existingUser));
        when(jwtTokenProvider.generateAccessToken("12345")).thenReturn(accessToken);
        when(jwtTokenProvider.generateRefreshToken("12345")).thenReturn(refreshToken);

        // When
        LoginRespDTO result = authService.login(kakaoUserInfo);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo(accessToken);
        assertThat(result.getRefreshToken()).isEqualTo(refreshToken);
        assertThat(result.getIsRegistered()).isTrue();

        verify(userRepository).findByKakaoOauthId("12345");
        verify(jwtTokenProvider).generateAccessToken("12345");
        verify(jwtTokenProvider).generateRefreshToken("12345");
    }

    @Test
    @DisplayName("신규 사용자 로그인 - 임시 회원 생성")
    void login_NewUser_CreateTemporaryUser() {
        // Given
        KakaoUserInfoDTO kakaoUserInfo = new KakaoUserInfoDTO(
                "67890",
                "신규사용자",
                "new_profile.jpg"
        );

        Users newUser = new Users();
        newUser.setId(2);
        newUser.setKakaoOauthId("67890");
        newUser.setNickname("신규사용자");
        newUser.setIsRegistered(false);
        newUser.setDeletedAt(null);

        String accessToken = "new-access-token";
        String refreshToken = "new-refresh-token";

        when(userRepository.findByKakaoOauthId("67890")).thenReturn(Optional.empty());
        when(userRepository.save(any(Users.class))).thenReturn(newUser);
        when(jwtTokenProvider.generateAccessToken("67890")).thenReturn(accessToken);
        when(jwtTokenProvider.generateRefreshToken("67890")).thenReturn(refreshToken);

        // When
        LoginRespDTO result = authService.login(kakaoUserInfo);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo(accessToken);
        assertThat(result.getRefreshToken()).isEqualTo(refreshToken);
        assertThat(result.getIsRegistered()).isFalse();

        verify(userRepository).findByKakaoOauthId("67890");
        verify(userRepository).save(any(Users.class));
        verify(jwtTokenProvider).generateAccessToken("67890");
        verify(jwtTokenProvider).generateRefreshToken("67890");
    }

    @Test
    @DisplayName("탈퇴한 사용자 로그인 실패")
    void login_WithdrawnUser_ThrowsException() {
        // Given
        KakaoUserInfoDTO kakaoUserInfo = new KakaoUserInfoDTO(
                "11111",
                "탈퇴사용자",
                "withdrawn.jpg"
        );

        Users withdrawnUser = new Users();
        withdrawnUser.setId(3);
        withdrawnUser.setKakaoOauthId("11111");
        withdrawnUser.setNickname("탈퇴사용자");
        withdrawnUser.setIsRegistered(true);
        withdrawnUser.setDeletedAt(LocalDateTime.now().minusDays(1));

        ReflectionTestUtils.setField(authService, "userWithdrawDays", 30);

        when(userRepository.findByKakaoOauthId("11111")).thenReturn(Optional.of(withdrawnUser));

        // When & Then
        assertThatThrownBy(() -> authService.login(kakaoUserInfo))
                .isInstanceOf(InvalidAccessException.class)
                .hasMessageContaining("탈퇴 후");

        verify(userRepository).findByKakaoOauthId("11111");
        verify(jwtTokenProvider, never()).generateAccessToken(anyString());
        verify(jwtTokenProvider, never()).generateRefreshToken(anyString());
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logout_Success() {
        // Given
        Integer userId = 1;
        String refreshToken = "valid-refresh-token";

        Users user = new Users();
        user.setId(userId);
        user.setKakaoOauthId("12345");
        user.setNickname("테스트사용자");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.isAccessToken(refreshToken)).thenReturn(false);

        // When
        authService.logout(userId, refreshToken);

        // Then
        verify(userRepository).findById(userId);
        verify(jwtTokenProvider).validateToken(refreshToken);
        verify(jwtTokenProvider).isAccessToken(refreshToken);
        verify(tokenBlacklistService).saveToken(refreshToken);
    }

    @Test
    @DisplayName("로그아웃 실패 - 사용자 없음")
    void logout_UserNotFound() {
        // Given
        Integer userId = 999;
        String refreshToken = "valid-refresh-token";

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.logout(userId, refreshToken))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository).findById(userId);
        verify(tokenBlacklistService, never()).saveToken(anyString());
    }

    @Test
    @DisplayName("정식 회원가입 성공")
    void signup_Success() {
        // Given
        Integer userId = 1;
        SignUpReqDTO signUpReq = new SignUpReqDTO("GUITAR", "테스트대학교");

        Users user = new Users();
        user.setId(userId);
        user.setKakaoOauthId("12345");
        user.setNickname("테스트사용자");
        user.setIsRegistered(false);

        University university = new University();
        university.setId(1);
        university.setName("테스트대학교");

        UserPhoto userPhoto = new UserPhoto();
        userPhoto.setId(1);
        userPhoto.setUser(user);
        userPhoto.setImageUrl("profile.jpg");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(universityRepository.findByName("테스트대학교")).thenReturn(university);
        when(userRepository.save(user)).thenReturn(user);
        when(userPhotoRepository.findByUser(user)).thenReturn(userPhoto);

        // When
        UserInfoDTO result = authService.signup(userId, signUpReq);

        // Then
        assertThat(result).isNotNull();
        assertThat(user.getIsRegistered()).isTrue();
        assertThat(user.getPosition()).isEqualTo(Users.Position.GUITAR);
        assertThat(user.getUniversity()).isEqualTo(university);

        verify(userRepository).findById(userId);
        verify(universityRepository).findByName("테스트대학교");
        verify(userRepository).save(user);
        verify(userPhotoRepository).findByUser(user);
    }

    @Test
    @DisplayName("정식 회원가입 실패 - 이미 가입 완료")
    void signup_AlreadyRegistered() {
        // Given
        Integer userId = 1;
        SignUpReqDTO signUpReq = new SignUpReqDTO("GUITAR", "테스트대학교");

        Users user = new Users();
        user.setId(userId);
        user.setKakaoOauthId("12345");
        user.setNickname("테스트사용자");
        user.setIsRegistered(true); // 이미 가입 완료

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> authService.signup(userId, signUpReq))
                .isInstanceOf(InvalidAccessException.class)
                .hasMessageContaining("이미 회원 가입이 완료된 계정입니다");

        verify(userRepository).findById(userId);
        verify(universityRepository, never()).findByName(anyString());
        verify(userRepository, never()).save(any(Users.class));
    }

    @Test
    @DisplayName("정식 회원가입 실패 - 존재하지 않는 대학")
    void signup_UniversityNotFound() {
        // Given
        Integer userId = 1;
        SignUpReqDTO signUpReq = new SignUpReqDTO("GUITAR", "존재하지않는대학교");

        Users user = new Users();
        user.setId(userId);
        user.setKakaoOauthId("12345");
        user.setNickname("테스트사용자");
        user.setIsRegistered(false);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(universityRepository.findByName("존재하지않는대학교")).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> authService.signup(userId, signUpReq))
                .isInstanceOf(UniversityNotFoundException.class)
                .hasMessageContaining("존재하지 않는 대학입니다");

        verify(userRepository).findById(userId);
        verify(universityRepository).findByName("존재하지않는대학교");
        verify(userRepository, never()).save(any(Users.class));
    }

    @Test
    @DisplayName("회원탈퇴 실패 - 동아리 대표로 활동 중")
    void cancel_AsClubRepresentative_ThrowsException() {
        // Given
        Integer userId = 1;

        Users user = new Users();
        user.setId(userId);
        user.setKakaoOauthId("12345");
        user.setNickname("대표사용자");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(clubMemberRepository.findClubNamesByUserRole(userId, ClubMember.MemberRole.REPRESENTATIVE))
                .thenReturn(List.of("밴드동아리", "음악동아리"));

        // When & Then
        assertThatThrownBy(() -> authService.cancel(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("탈퇴할 수 없습니다")
                .hasMessageContaining("밴드동아리, 음악동아리");

        verify(userRepository).findById(userId);
        verify(clubMemberRepository).findClubNamesByUserRole(userId, ClubMember.MemberRole.REPRESENTATIVE);
        verify(userRepository, never()).save(any(Users.class));
    }
}