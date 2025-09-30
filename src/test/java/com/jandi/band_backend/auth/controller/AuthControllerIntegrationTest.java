package com.jandi.band_backend.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jandi.band_backend.auth.dto.RefreshReqDTO;
import com.jandi.band_backend.auth.dto.SignUpReqDTO;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import com.jandi.band_backend.security.jwt.JwtTokenProvider;
import com.jandi.band_backend.testutil.TestDataFactory;
import com.jandi.band_backend.univ.entity.Region;
import com.jandi.band_backend.univ.entity.University;
import com.jandi.band_backend.univ.repository.RegionRepository;
import org.springframework.http.MediaType;
import com.jandi.band_backend.univ.repository.UniversityRepository;
import com.jandi.band_backend.security.CustomUserDetails;
import com.jandi.band_backend.security.CustomUserDetailsService;
import com.jandi.band_backend.user.dto.UserInfoDTO;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController 통합 테스트")
class AuthControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UniversityRepository universityRepository;

    @Autowired
    private RegionRepository regionRepository;

    private String validAccessToken;
    private String validRefreshToken;
    private Users testUser;
    private University testUniversity;
    private CustomUserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        // 테스트 데이터 생성 - TestDataFactory 사용
        Region region = TestDataFactory.createTestRegion("SEOUL", "서울");
        region = regionRepository.save(region);

        testUniversity = TestDataFactory.createTestUniversity("테스트대학교", region);
        testUniversity = universityRepository.save(testUniversity);

        testUser = TestDataFactory.createTestUser("test123456", "테스트사용자", testUniversity);
        testUser = userRepository.save(testUser);

        // CustomUserDetails 생성
        testUserDetails = (CustomUserDetails) customUserDetailsService.loadUserByUsername(testUser.getKakaoOauthId());

        // JWT 토큰 생성
        validAccessToken = jwtTokenProvider.generateAccessToken(testUser.getKakaoOauthId());
        validRefreshToken = jwtTokenProvider.generateRefreshToken(testUser.getKakaoOauthId());

        // SecurityContext 설정
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            testUserDetails, null, testUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("카카오 로그인 - 인증 코드 누락 시 400 오류")
    void kakaoLogin_MissingAuthCode_BadRequest() throws Exception {
        mockMvc.perform(get("/api/auth/login"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("토큰 갱신 - 유효한 리프레시 토큰")
    void refreshToken_ValidToken() throws Exception {
        // SecurityContext 설정
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            testUserDetails, null, testUserDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        RefreshReqDTO refreshReqDTO = new RefreshReqDTO(validRefreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReqDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("토큰 갱신 - 잘못된 리프레시 토큰")
    void refreshToken_InvalidToken() throws Exception {
        RefreshReqDTO refreshReqDTO = new RefreshReqDTO("invalid_token");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReqDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("회원가입 - 유효한 데이터")
    void signUp_ValidData() throws Exception {
        SignUpReqDTO signUpReqDTO = new SignUpReqDTO("GUITAR", "서울대학교");

        mockMvc.perform(post("/api/auth/signup")
                        .header("Authorization", "Bearer " + validAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpReqDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("회원가입 - 인증 없이 접근")
    void signUp_Unauthorized() throws Exception {
        SignUpReqDTO signUpReqDTO = new SignUpReqDTO("GUITAR", "서울대학교");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signUpReqDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("로그아웃 - 유효한 토큰")
    void logout_ValidToken() throws Exception {
        RefreshReqDTO refreshReqDTO = new RefreshReqDTO(validRefreshToken);

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + validAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReqDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("로그아웃 - 인증 없이 접근")
    void logout_Unauthorized() throws Exception {
        RefreshReqDTO refreshReqDTO = new RefreshReqDTO(validRefreshToken);

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReqDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("회원탈퇴 - 유효한 토큰")
    void cancel_ValidToken() throws Exception {
        mockMvc.perform(post("/api/auth/cancel")
                        .header("Authorization", "Bearer " + validAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("회원탈퇴 - 인증 없이 접근")
    void cancel_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/cancel"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("잘못된 JSON 형식")
    void invalidJsonFormat() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Content-Type 누락")
    void missingContentType() throws Exception {
        RefreshReqDTO refreshReqDTO = new RefreshReqDTO(validRefreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .content(objectMapper.writeValueAsString(refreshReqDTO)))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("CORS 헤더 검증")
    void corsHeaders() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("보안 헤더 검증")
    void securityHeaders() throws Exception {
        mockMvc.perform(get("/api/auth/login")
                        .param("code", "test_code"))
                .andExpect(header().exists("X-Content-Type-Options"))
                .andExpect(header().exists("X-Frame-Options"));
    }
}