package com.jandi.band_backend.promo.controller;

import com.jandi.band_backend.config.IntegrationTest;
import com.jandi.band_backend.image.S3Service;
import com.jandi.band_backend.promo.entity.Promo;
import com.jandi.band_backend.promo.entity.PromoLike;
import com.jandi.band_backend.promo.repository.PromoLikeRepository;
import com.jandi.band_backend.promo.repository.PromoRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PromoController 통합 테스트
 *
 * <p>테스트 전략:
 * <ul>
 *   <li>MockMvc를 사용한 전체 Spring Boot 통합 테스트</li>
 *   <li>실제 JWT 인증 플로우 포함</li>
 *   <li>S3Service는 Mock으로 외부 의존성 격리</li>
 *   <li>공연 홍보글의 CRUD, 좋아요, 검색 기능 검증</li>
 * </ul>
 *
 * <p>커버리지:
 * <ul>
 *   <li>홍보글 CRUD 기본 동작</li>
 *   <li>이미지 업로드/수정/삭제 처리</li>
 *   <li>좋아요 기능 (추가/조회)</li>
 *   <li>권한 검증 (작성자만 수정/삭제 가능)</li>
 * </ul>
 */
@IntegrationTest
@DisplayName("Promo API 통합 테스트")
public class PromoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private S3Service s3Service;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PromoRepository promoRepository;

    @Autowired
    private PromoLikeRepository promoLikeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private UniversityRepository universityRepository;

    private Users testUser;
    private Users otherUser;
    private String testUserToken;
    private String otherUserToken;
    private University university;

    @BeforeEach
    void setUp() throws Exception {
        // 지역 및 대학 설정
        Region region = TestDataFactory.createTestRegion("SEOUL", "서울");
        region = regionRepository.save(region);

        university = TestDataFactory.createTestUniversity("테스트대학교", region);
        university = universityRepository.save(university);

        // 테스트 사용자 생성
        testUser = TestDataFactory.createTestUser("1", "테스트유저", university);
        testUser = userRepository.save(testUser);

        otherUser = TestDataFactory.createTestUser("2", "다른유저", university);
        otherUser = userRepository.save(otherUser);

        // JWT 토큰 생성
        testUserToken = jwtTokenProvider.generateAccessToken(testUser.getKakaoOauthId());
        otherUserToken = jwtTokenProvider.generateAccessToken(otherUser.getKakaoOauthId());

        // S3 Mock 설정
        when(s3Service.uploadImage(any(), anyString())).thenReturn("https://s3.example.com/promo/test-image.jpg");
    }

    // === 홍보글 생성 테스트 ===

    @Test
    @DisplayName("홍보글 생성 - 성공 (이미지 없이)")
    void createPromo_Success_WithoutImage() throws Exception {
        // Given & When & Then
        mockMvc.perform(multipart("/api/promos")
                        .param("teamName", "테스트 밴드")
                        .param("title", "정기 공연 안내")
                        .param("admissionFee", "10000")
                        .param("eventDatetime", "2025-12-31T19:00:00")
                        .param("location", "홍대 클럽")
                        .param("address", "서울시 마포구 홍익로 123")
                        .param("latitude", "37.556514")
                        .param("longitude", "126.922591")
                        .param("description", "연말 정기 공연입니다")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("공연 홍보 생성 성공!"));
    }

    @Test
    @DisplayName("홍보글 생성 - 성공 (이미지 포함)")
    void createPromo_Success_WithImage() throws Exception {
        // Given
        MockMultipartFile imageFile = new MockMultipartFile(
                "image",
                "poster.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/promos")
                        .file(imageFile)
                        .param("teamName", "락밴드")
                        .param("title", "여름 페스티벌")
                        .param("admissionFee", "15000")
                        .param("eventDatetime", "2025-08-15T18:00:00")
                        .param("location", "올림픽공원")
                        .param("description", "야외 공연입니다")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("공연 홍보 생성 성공!"));
    }

    @Test
    @DisplayName("홍보글 생성 - 미인증 사용자")
    void createPromo_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(multipart("/api/promos")
                        .param("teamName", "테스트 밴드")
                        .param("title", "정기 공연 안내")
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("홍보글 생성 - 필수 필드 누락 (팀명)")
    void createPromo_MissingRequiredField() throws Exception {
        // When & Then
        mockMvc.perform(multipart("/api/promos")
                        .param("title", "정기 공연 안내")
                        .param("eventDatetime", "2025-12-31T19:00:00")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isBadRequest());
    }

    // === 홍보글 목록 조회 테스트 ===

    @Test
    @DisplayName("홍보글 목록 조회 - 성공 (페이징)")
    void getPromoList_Success() throws Exception {
        // Given
        Promo promo1 = createTestPromo("밴드1", "공연1", testUser);
        Promo promo2 = createTestPromo("밴드2", "공연2", testUser);
        promoRepository.save(promo1);
        promoRepository.save(promo2);

        // When & Then
        mockMvc.perform(get("/api/promos")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("공연 홍보 목록 조회 성공"))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    // === 홍보글 상세 조회 테스트 ===

    @Test
    @DisplayName("홍보글 상세 조회 - 성공")
    void getPromoDetail_Success() throws Exception {
        // Given
        Promo promo = createTestPromo("테스트 밴드", "정기 공연", testUser);
        promo = promoRepository.save(promo);

        // When & Then
        mockMvc.perform(get("/api/promos/{promoId}", promo.getId())
                        .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("공연 홍보 상세 조회 성공"))
                .andExpect(jsonPath("$.data.title").value("정기 공연"))
                .andExpect(jsonPath("$.data.teamName").value("테스트 밴드"))
                .andExpect(jsonPath("$.data.creatorId").value(testUser.getId()));
    }

    @Test
    @DisplayName("홍보글 상세 조회 - 존재하지 않는 홍보글")
    void getPromoDetail_NotFound() throws Exception {
        // Given
        Integer nonExistentPromoId = 99999;

        // When & Then
        mockMvc.perform(get("/api/promos/{promoId}", nonExistentPromoId)
                        .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    // === 홍보글 수정 테스트 ===

    @Test
    @DisplayName("홍보글 수정 - 성공 (작성자)")
    void updatePromo_Success_AsCreator() throws Exception {
        // Given
        Promo promo = createTestPromo("원래 밴드", "원래 제목", testUser);
        promo = promoRepository.save(promo);

        // When & Then
        mockMvc.perform(multipart("/api/promos/{promoId}", promo.getId())
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .param("teamName", "수정된 밴드")
                        .param("title", "수정된 제목")
                        .param("admissionFee", "20000")
                        .param("eventDatetime", "2025-12-25T20:00:00")
                        .param("description", "수정된 설명")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("공연 홍보 수정 성공!"));
    }

    @Test
    @DisplayName("홍보글 수정 - 권한 없음 (다른 사용자)")
    void updatePromo_Forbidden_NotCreator() throws Exception {
        // Given
        Promo promo = createTestPromo("테스트 밴드", "원래 제목", testUser);
        promo = promoRepository.save(promo);

        // When & Then
        mockMvc.perform(multipart("/api/promos/{promoId}", promo.getId())
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .param("teamName", "수정된 밴드")
                        .param("title", "수정된 제목")
                        .header("Authorization", "Bearer " + otherUserToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isBadRequest()); // 실제로는 400 반환
    }

    // === 홍보글 삭제 테스트 ===

    @Test
    @DisplayName("홍보글 삭제 - 성공 (작성자)")
    void deletePromo_Success_AsCreator() throws Exception {
        // Given
        Promo promo = createTestPromo("테스트 밴드", "삭제할 공연", testUser);
        promo = promoRepository.save(promo);

        // When & Then
        mockMvc.perform(delete("/api/promos/{promoId}", promo.getId())
                        .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("공연 홍보 삭제 성공!"));
    }

    @Test
    @DisplayName("홍보글 삭제 - 권한 없음 (다른 사용자)")
    void deletePromo_Forbidden_NotCreator() throws Exception {
        // Given
        Promo promo = createTestPromo("테스트 밴드", "삭제할 공연", testUser);
        promo = promoRepository.save(promo);

        // When & Then
        mockMvc.perform(delete("/api/promos/{promoId}", promo.getId())
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isBadRequest()); // 실제로는 400 반환
    }

    // === 좋아요 기능 테스트 ===

    @Test
    @DisplayName("좋아요 추가 - 성공")
    void likePromo_Success() throws Exception {
        // Given
        Promo promo = createTestPromo("테스트 밴드", "좋아요 테스트", testUser);
        promo = promoRepository.save(promo);

        // When & Then
        mockMvc.perform(post("/api/promos/{promoId}/like", promo.getId())
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("공연 홍보 좋아요가 추가되었습니다."));
    }

    @Test
    @DisplayName("좋아요 취소 - 성공 (이미 좋아요 누른 경우)")
    void unlikePromo_Success() throws Exception {
        // Given
        Promo promo = createTestPromo("테스트 밴드", "좋아요 테스트", testUser);
        promo = promoRepository.save(promo);

        // 먼저 좋아요 추가
        PromoLike like = new PromoLike();
        like.setPromo(promo);
        like.setUser(otherUser);
        promoLikeRepository.save(like);

        // When & Then - 다시 좋아요 요청하면 취소됨 (토글 방식)
        mockMvc.perform(post("/api/promos/{promoId}/like", promo.getId())
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("좋아요 상태 조회 - 성공")
    void getLikeStatus_Success() throws Exception {
        // Given
        Promo promo = createTestPromo("테스트 밴드", "좋아요 테스트", testUser);
        promo = promoRepository.save(promo);

        // When & Then
        mockMvc.perform(get("/api/promos/{promoId}/like/status", promo.getId())
                        .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("공연 홍보 좋아요 상태 조회 성공"))
                .andExpect(jsonPath("$.data").isBoolean());
    }

    @Test
    @DisplayName("좋아요 개수 조회 - 성공")
    void getLikeCount_Success() throws Exception {
        // Given
        Promo promo = createTestPromo("테스트 밴드", "좋아요 테스트", testUser);
        promo = promoRepository.save(promo);

        // 좋아요 추가
        PromoLike like1 = new PromoLike();
        like1.setPromo(promo);
        like1.setUser(testUser);
        promoLikeRepository.save(like1);

        PromoLike like2 = new PromoLike();
        like2.setPromo(promo);
        like2.setUser(otherUser);
        promoLikeRepository.save(like2);

        // When & Then - 인증 필요 없는 엔드포인트로 수정
        mockMvc.perform(get("/api/promos/{promoId}/like/count", promo.getId())
                        .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("공연 홍보 좋아요 수 조회 성공"));
    }

    // === 검색 기능 테스트 ===

    @Test
    @DisplayName("홍보글 검색 - 성공 (제목 검색)")
    void searchPromos_Success() throws Exception {
        // Given
        Promo promo1 = createTestPromo("락밴드", "락 페스티벌", testUser);
        Promo promo2 = createTestPromo("재즈밴드", "재즈 나이트", testUser);
        promoRepository.save(promo1);
        promoRepository.save(promo2);

        // When & Then
        mockMvc.perform(get("/api/promos/search")
                        .param("keyword", "락")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("공연 홍보 검색 성공"));
    }

    // === Helper Methods ===

    private Promo createTestPromo(String teamName, String title, Users creator) {
        Promo promo = new Promo();
        promo.setTeamName(teamName);
        promo.setTitle(title);
        promo.setAdmissionFee(new BigDecimal("10000"));
        promo.setEventDatetime(LocalDateTime.of(2025, 12, 31, 19, 0));
        promo.setLocation("테스트 공연장");
        promo.setAddress("서울시 강남구");
        promo.setLatitude(new BigDecimal("37.5"));
        promo.setLongitude(new BigDecimal("127.0"));
        promo.setDescription("테스트 공연입니다");
        promo.setCreator(creator);
        promo.setViewCount(0);
        promo.setCommentCount(0);
        promo.setLikeCount(0);
        return promo;
    }
}
