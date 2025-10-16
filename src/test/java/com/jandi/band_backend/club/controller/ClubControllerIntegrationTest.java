package com.jandi.band_backend.club.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jandi.band_backend.club.dto.ClubReqDTO;
import com.jandi.band_backend.club.dto.ClubUpdateReqDTO;
import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.entity.ClubMember;
import com.jandi.band_backend.club.entity.ClubPhoto;
import com.jandi.band_backend.club.repository.ClubMemberRepository;
import com.jandi.band_backend.club.repository.ClubPhotoRepository;
import com.jandi.band_backend.club.repository.ClubRepository;
import com.jandi.band_backend.config.IntegrationTest;
import com.jandi.band_backend.image.S3Service;
import com.jandi.band_backend.security.jwt.JwtTokenProvider;
import com.jandi.band_backend.univ.entity.Region;
import com.jandi.band_backend.univ.entity.University;
import com.jandi.band_backend.univ.repository.RegionRepository;
import com.jandi.band_backend.univ.repository.UniversityRepository;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserRepository;
import com.jandi.band_backend.testutil.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ClubController 통합 테스트
 *
 * <p>테스트 전략:
 * <ul>
 *   <li>MockMvc를 사용한 전체 Spring Boot 통합 테스트</li>
 *   <li>실제 JWT 인증 플로우 포함 (JwtTokenProvider 사용)</li>
 *   <li>S3Service는 @MockBean으로 격리 (외부 의존성)</li>
 *   <li>ClubService는 실제 빈 사용 (통합 테스트 목적)</li>
 *   <li>권한 검증(대표자/일반 회원) 포함</li>
 * </ul>
 *
 * <p>커버리지:
 * <ul>
 *   <li>동아리 CRUD 기본 동작</li>
 *   <li>인증/권한 실패 케이스</li>
 *   <li>대표 사진 업로드 (S3 mock 사용)</li>
 *   <li>대표자 위임, 부원 명단 조회, 동아리 탈퇴</li>
 * </ul>
 */
@IntegrationTest
@DisplayName("Club API 통합 테스트")
public class ClubControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private S3Service s3Service;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private ClubPhotoRepository clubPhotoRepository;

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
    void setUp() {
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

        // JWT 토큰 생성 (kakaoOauthId 기반)
        testUserToken = jwtTokenProvider.generateAccessToken(testUser.getKakaoOauthId());
        otherUserToken = jwtTokenProvider.generateAccessToken(otherUser.getKakaoOauthId());
    }

    // === 동아리 생성 테스트 ===

    @Test
    @DisplayName("동아리 생성 - 성공")
    void createClub_Success() throws Exception {
        // Given
        ClubReqDTO request = new ClubReqDTO();
        request.setName("테스트 밴드");
        request.setDescription("열정적인 밴드 동아리입니다");
        request.setUniversityId(university.getId());

        // When & Then
        mockMvc.perform(post("/api/clubs")
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("동아리가 성공적으로 생성되었습니다"))
                .andExpect(jsonPath("$.data.name").value("테스트 밴드"))
                .andExpect(jsonPath("$.data.description").value("열정적인 밴드 동아리입니다"))
                .andExpect(jsonPath("$.data.representativeId").value(testUser.getId()));
    }

    @Test
    @DisplayName("동아리 생성 - 미인증 사용자")
    void createClub_Unauthorized() throws Exception {
        // Given
        ClubReqDTO request = new ClubReqDTO();
        request.setName("테스트 밴드");
        request.setDescription("설명");
        request.setUniversityId(university.getId());

        // When & Then
        // Spring Security는 JWT 없으면 400 반환
        mockMvc.perform(post("/api/clubs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // === 동아리 목록 조회 테스트 ===

    @Test
    @DisplayName("동아리 목록 조회 - 성공")
    void getClubList_Success() throws Exception {
        // Given
        Club club1 = TestDataFactory.createTestClub("밴드1", university, testUser);
        Club club2 = TestDataFactory.createTestClub("밴드2", university, otherUser);
        clubRepository.save(club1);
        clubRepository.save(club2);

        // When & Then
        mockMvc.perform(get("/api/clubs")
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    // === 동아리 상세 조회 테스트 ===

    @Test
    @DisplayName("동아리 상세 조회 - 성공")
    void getClubDetail_Success() throws Exception {
        // Given
        Club club = TestDataFactory.createTestClub("테스트 밴드", university, testUser);
        club = clubRepository.save(club);

        // ClubMember 추가 (대표자)
        ClubMember member = new ClubMember();
        member.setClub(club);
        member.setUser(testUser);
        member.setRole(ClubMember.MemberRole.REPRESENTATIVE);
        clubMemberRepository.save(member);

        // When & Then
        mockMvc.perform(get("/api/clubs/{clubId}", club.getId())
                        .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("테스트 밴드"))
                .andExpect(jsonPath("$.data.representativeId").value(testUser.getId()));
    }

    @Test
    @DisplayName("동아리 상세 조회 - 존재하지 않는 동아리")
    void getClubDetail_NotFound() throws Exception {
        // Given
        Integer nonExistentClubId = 99999;

        // When & Then
        mockMvc.perform(get("/api/clubs/{clubId}", nonExistentClubId)
                        .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("CLUB_NOT_FOUND"));
    }

    // === 동아리 수정 테스트 ===

    @Test
    @DisplayName("동아리 정보 수정 - 성공 (대표자)")
    void updateClub_Success_AsRepresentative() throws Exception {
        // Given
        Club club = TestDataFactory.createTestClub("원래 이름", university, testUser);
        club = clubRepository.save(club);

        // ClubMember를 REPRESENTATIVE로 추가
        ClubMember member = new ClubMember();
        member.setClub(club);
        member.setUser(testUser);
        member.setRole(ClubMember.MemberRole.REPRESENTATIVE);
        clubMemberRepository.save(member);

        ClubUpdateReqDTO request = new ClubUpdateReqDTO();
        request.setName("수정된 이름");
        request.setDescription("수정된 설명");

        // When & Then
        mockMvc.perform(patch("/api/clubs/{clubId}", club.getId())
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("동아리 정보가 성공적으로 수정되었습니다"));
    }

    @Test
    @DisplayName("동아리 정보 수정 - 권한 없음 (일반 회원)")
    void updateClub_Forbidden_NotRepresentative() throws Exception {
        // Given
        Club club = TestDataFactory.createTestClub("테스트 밴드", university, testUser);
        club = clubRepository.save(club);

        // testUser를 REPRESENTATIVE로 추가
        ClubMember representative = new ClubMember();
        representative.setClub(club);
        representative.setUser(testUser);
        representative.setRole(ClubMember.MemberRole.REPRESENTATIVE);
        clubMemberRepository.save(representative);

        // otherUser를 MEMBER로 추가
        ClubMember member = new ClubMember();
        member.setClub(club);
        member.setUser(otherUser);
        member.setRole(ClubMember.MemberRole.MEMBER);
        clubMemberRepository.save(member);

        ClubUpdateReqDTO request = new ClubUpdateReqDTO();
        request.setName("수정된 이름");

        // When & Then
        mockMvc.perform(patch("/api/clubs/{clubId}", club.getId())
                        .header("Authorization", "Bearer " + otherUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // === 대표자 위임 테스트 ===

    @Test
    @DisplayName("동아리 대표자 위임 - 성공")
    void transferRepresentative_Success() throws Exception {
        // Given
        Club club = TestDataFactory.createTestClub("테스트 밴드", university, testUser);
        club = clubRepository.save(club);

        // testUser를 REPRESENTATIVE로 추가
        ClubMember representative = new ClubMember();
        representative.setClub(club);
        representative.setUser(testUser);
        representative.setRole(ClubMember.MemberRole.REPRESENTATIVE);
        clubMemberRepository.save(representative);

        // otherUser를 MEMBER로 추가
        ClubMember member = new ClubMember();
        member.setClub(club);
        member.setUser(otherUser);
        member.setRole(ClubMember.MemberRole.MEMBER);
        clubMemberRepository.save(member);

        // When & Then
        mockMvc.perform(patch("/api/clubs/{clubId}/representative", club.getId())
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newRepresentativeUserId\":" + otherUser.getId() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("동아리 대표자 권한이 성공적으로 위임되었습니다"));
    }

    // === 동아리 삭제 테스트 ===

    @Test
    @DisplayName("동아리 삭제 - 성공 (대표자)")
    void deleteClub_Success_AsRepresentative() throws Exception {
        // Given
        Club club = TestDataFactory.createTestClub("테스트 밴드", university, testUser);
        club = clubRepository.save(club);

        // ClubMember를 REPRESENTATIVE로 추가
        ClubMember representative = new ClubMember();
        representative.setClub(club);
        representative.setUser(testUser);
        representative.setRole(ClubMember.MemberRole.REPRESENTATIVE);
        clubMemberRepository.save(representative);

        // ClubPhoto를 추가 (deleteClub에서 필요)
        ClubPhoto clubPhoto = new ClubPhoto();
        clubPhoto.setClub(club);
        clubPhoto.setImageUrl("https://s3.example.com/default.jpg");
        clubPhoto.setIsCurrent(true);
        clubPhotoRepository.save(clubPhoto);

        // When & Then
        mockMvc.perform(delete("/api/clubs/{clubId}", club.getId())
                        .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("동아리가 성공적으로 삭제되었습니다"));
    }

    @Test
    @DisplayName("동아리 삭제 - 권한 없음 (일반 회원)")
    void deleteClub_Forbidden_NotRepresentative() throws Exception {
        // Given
        Club club = TestDataFactory.createTestClub("테스트 밴드", university, testUser);
        club = clubRepository.save(club);

        // testUser를 REPRESENTATIVE로 추가
        ClubMember representative = new ClubMember();
        representative.setClub(club);
        representative.setUser(testUser);
        representative.setRole(ClubMember.MemberRole.REPRESENTATIVE);
        clubMemberRepository.save(representative);

        // otherUser를 MEMBER로 추가
        ClubMember member = new ClubMember();
        member.setClub(club);
        member.setUser(otherUser);
        member.setRole(ClubMember.MemberRole.MEMBER);
        clubMemberRepository.save(member);

        // When & Then
        mockMvc.perform(delete("/api/clubs/{clubId}", club.getId())
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isForbidden());
    }

    // === 대표 사진 업로드 테스트 ===

    @Test
    @DisplayName("동아리 대표 사진 업로드 - 성공")
    void uploadClubPhoto_Success() throws Exception {
        // Given
        Club club = TestDataFactory.createTestClub("테스트 밴드", university, testUser);
        club = clubRepository.save(club);

        // ClubMember를 REPRESENTATIVE로 추가
        ClubMember representative = new ClubMember();
        representative.setClub(club);
        representative.setUser(testUser);
        representative.setRole(ClubMember.MemberRole.REPRESENTATIVE);
        clubMemberRepository.save(representative);

        // ClubPhoto를 추가 (uploadClubPhoto에서 필요)
        ClubPhoto clubPhoto = new ClubPhoto();
        clubPhoto.setClub(club);
        clubPhoto.setImageUrl("https://s3.example.com/default.jpg");
        clubPhoto.setIsCurrent(true);
        clubPhotoRepository.save(clubPhoto);

        MockMultipartFile file = new MockMultipartFile(
                "image",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes()
        );

        given(s3Service.uploadImage(any(), anyString())).willReturn("https://s3.example.com/test.jpg");

        // When & Then
        mockMvc.perform(multipart("/api/clubs/{clubId}/main-image", club.getId())
                        .file(file)
                        .header("Authorization", "Bearer " + testUserToken)
                        .with(request -> {
                            request.setMethod("POST");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("동아리 대표 사진이 성공적으로 업로드되었습니다"))
                .andExpect(jsonPath("$.data").value("https://s3.example.com/test.jpg"));
    }

    // === 부원 명단 조회 테스트 ===

    @Test
    @DisplayName("동아리 부원 명단 조회 - 성공")
    void getClubMembers_Success() throws Exception {
        // Given
        Club club = TestDataFactory.createTestClub("테스트 밴드", university, testUser);
        club = clubRepository.save(club);

        // ClubMember 추가
        ClubMember member = new ClubMember();
        member.setClub(club);
        member.setUser(testUser);
        member.setRole(ClubMember.MemberRole.REPRESENTATIVE);
        clubMemberRepository.save(member);

        // When & Then
        mockMvc.perform(get("/api/clubs/{clubId}/members", club.getId())
                        .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(club.getId()))
                .andExpect(jsonPath("$.data.members").isArray())
                .andExpect(jsonPath("$.data.totalMemberCount").isNumber());
    }

    // === 동아리 탈퇴 테스트 ===

    @Test
    @DisplayName("동아리 탈퇴 - 성공")
    void leaveClub_Success() throws Exception {
        // Given
        Club club = TestDataFactory.createTestClub("테스트 밴드", university, testUser);
        club = clubRepository.save(club);

        // testUser를 REPRESENTATIVE로 추가
        ClubMember representative = new ClubMember();
        representative.setClub(club);
        representative.setUser(testUser);
        representative.setRole(ClubMember.MemberRole.REPRESENTATIVE);
        clubMemberRepository.save(representative);

        // otherUser를 MEMBER로 추가
        ClubMember member = new ClubMember();
        member.setClub(club);
        member.setUser(otherUser);
        member.setRole(ClubMember.MemberRole.MEMBER);
        clubMemberRepository.save(member);

        // When & Then
        mockMvc.perform(delete("/api/clubs/{clubId}/members/me", club.getId())
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("동아리에서 성공적으로 탈퇴했습니다"));
    }
}
