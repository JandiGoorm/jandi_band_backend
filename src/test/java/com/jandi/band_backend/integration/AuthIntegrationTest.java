package com.jandi.band_backend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jandi.band_backend.auth.dto.RefreshReqDTO;
import com.jandi.band_backend.auth.dto.SignUpReqDTO;
import com.jandi.band_backend.auth.service.kakao.KakaoUserService;
import com.jandi.band_backend.config.IntegrationTest;
import com.jandi.band_backend.security.jwt.JwtTokenProvider;
import com.jandi.band_backend.testutil.TestDataFactory;
import com.jandi.band_backend.univ.entity.Region;
import com.jandi.band_backend.univ.entity.University;
import com.jandi.band_backend.univ.repository.RegionRepository;
import com.jandi.band_backend.univ.repository.UniversityRepository;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.ArgumentMatchers.anyString;

@IntegrationTest
@DisplayName("Authentication API 통합 테스트")
class AuthIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UniversityRepository universityRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KakaoUserService kakaoUserService;
    private University university;
    private Users testUser;
    private String accessToken;

    @BeforeEach
    void setUp() {
        // 테스트 데이터 준비 - TestDataFactory 사용
        Region region = TestDataFactory.createTestRegion("SEOUL", "서울");
        region = regionRepository.save(region);

        university = TestDataFactory.createTestUniversity("서울대학교", region);
        university.setUniversityCode("SNU0001");
        university = universityRepository.save(university);

        testUser = TestDataFactory.createTestUser("test_kakao_id", "테스트유저", university);
        testUser = userRepository.save(testUser);

        // JWT 토큰 생성
        accessToken = jwtTokenProvider.generateAccessToken(testUser.getKakaoOauthId());

        doNothing().when(kakaoUserService).unlink(anyString());
    }

    @Test
    @DisplayName("회원가입 API - 성공 케이스")
    void signup_Success() throws Exception {
        // Given
        Users unregisteredUser = TestDataFactory.createTestUser("unregistered_kakao_id", "미등록유저", university);
        unregisteredUser.setIsRegistered(false);
        unregisteredUser = userRepository.save(unregisteredUser);

        String unregisteredAccessToken = jwtTokenProvider.generateAccessToken(unregisteredUser.getKakaoOauthId());

        SignUpReqDTO signUpRequest = new SignUpReqDTO();
        signUpRequest.setPosition("DRUM");
        signUpRequest.setUniversity(university.getName());

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                .header("Authorization", "Bearer " + unregisteredAccessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.nickname").value("미등록유저"));
    }

    @Test
    @DisplayName("회원가입 API - 인증 토큰 없음")
    void signup_NoAuth() throws Exception {
        // Given
        SignUpReqDTO signUpRequest = new SignUpReqDTO();
        signUpRequest.setPosition("드러머");
        signUpRequest.setUniversity(university.getName());

        // When & Then
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그아웃 API - 성공 케이스")
    void logout_Success() throws Exception {
        // Given
        RefreshReqDTO refreshRequest = new RefreshReqDTO("test_refresh_token");

        // When & Then
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("로그아웃 API - 인증 토큰 없음")
    void logout_NoAuth() throws Exception {
        // Given
        RefreshReqDTO refreshRequest = new RefreshReqDTO("test_refresh_token");

        // When & Then
        mockMvc.perform(post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그아웃 API - 잘못된 토큰")
    void logout_InvalidToken() throws Exception {
        // Given
        RefreshReqDTO refreshRequest = new RefreshReqDTO("test_refresh_token");

        // When & Then
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer invalid_token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("회원탈퇴 API - 성공 케이스")
    void cancel_Success() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/cancel")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("회원탈퇴 API - 인증 토큰 없음")
    void cancel_NoAuth() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/cancel")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("토큰 재발급 API - 성공 케이스")
    void refresh_Success() throws Exception {
        // Given
        String refreshToken = jwtTokenProvider.generateRefreshToken(testUser.getKakaoOauthId());
        RefreshReqDTO refreshRequest = new RefreshReqDTO(refreshToken);

        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("토큰 재발급 API - 잘못된 리프레시 토큰")
    void refresh_InvalidRefreshToken() throws Exception {
        // Given
        RefreshReqDTO refreshRequest = new RefreshReqDTO("invalid_refresh_token");

        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized());
    }
}
