package com.jandi.band_backend.team.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.entity.ClubMember;
import com.jandi.band_backend.club.repository.ClubMemberRepository;
import com.jandi.band_backend.club.repository.ClubRepository;
import com.jandi.band_backend.config.IntegrationTest;
import com.jandi.band_backend.security.jwt.JwtTokenProvider;
import com.jandi.band_backend.team.dto.TeamReqDTO;
import com.jandi.band_backend.team.entity.Team;
import com.jandi.band_backend.team.entity.TeamMember;
import com.jandi.band_backend.team.repository.TeamMemberRepository;
import com.jandi.band_backend.team.repository.TeamRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TeamController 통합 테스트
 *
 * <p>테스트 전략:
 * <ul>
 *   <li>MockMvc를 사용한 전체 Spring Boot 통합 테스트</li>
 *   <li>실제 JWT 인증 플로우 포함</li>
 *   <li>Team은 Club 내 종속 엔티티이므로 Club과 ClubMember 설정 필요</li>
 *   <li>팀장(TeamLeader)과 일반 팀원 권한 검증 포함</li>
 * </ul>
 *
 * <p>커버리지:
 * <ul>
 *   <li>팀 CRUD 기본 동작</li>
 *   <li>동아리 내 팀 목록 조회</li>
 *   <li>팀장 권한 검증 (수정/삭제)</li>
 *   <li>팀 탈퇴 기능</li>
 * </ul>
 */
@IntegrationTest
@DisplayName("Team API 통합 테스트")
public class TeamControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TeamMemberRepository teamMemberRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

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
    private Club testClub;

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

        // JWT 토큰 생성
        testUserToken = jwtTokenProvider.generateAccessToken(testUser.getKakaoOauthId());
        otherUserToken = jwtTokenProvider.generateAccessToken(otherUser.getKakaoOauthId());

        // 테스트 동아리 생성 (Team은 Club 내에 존재)
        testClub = TestDataFactory.createTestClub("테스트 밴드", university, testUser);
        testClub = clubRepository.save(testClub);

        // testUser를 동아리 대표로 추가
        ClubMember representative = new ClubMember();
        representative.setClub(testClub);
        representative.setUser(testUser);
        representative.setRole(ClubMember.MemberRole.REPRESENTATIVE);
        clubMemberRepository.save(representative);

        // otherUser를 동아리 일반 회원으로 추가
        ClubMember member = new ClubMember();
        member.setClub(testClub);
        member.setUser(otherUser);
        member.setRole(ClubMember.MemberRole.MEMBER);
        clubMemberRepository.save(member);
    }

    // === 팀 생성 테스트 ===

    @Test
    @DisplayName("팀 생성 - 성공 (동아리 회원)")
    void createTeam_Success() throws Exception {
        // Given
        TeamReqDTO request = new TeamReqDTO();
        request.setName("새로운 곡 팀");

        // When & Then
        mockMvc.perform(post("/api/clubs/{clubId}/teams", testClub.getId())
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("곡 팀이 성공적으로 생성되었습니다."))
                .andExpect(jsonPath("$.data.name").value("새로운 곡 팀"));
    }

    @Test
    @DisplayName("팀 생성 - 미인증 사용자")
    void createTeam_Unauthorized() throws Exception {
        // Given
        TeamReqDTO request = new TeamReqDTO();
        request.setName("새로운 곡 팀");

        // When & Then
        mockMvc.perform(post("/api/clubs/{clubId}/teams", testClub.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // === 동아리 팀 목록 조회 테스트 ===

    @Test
    @DisplayName("동아리 팀 목록 조회 - 성공")
    void getTeamsByClub_Success() throws Exception {
        // Given
        Team team1 = TestDataFactory.createTestTeam("팀1", testClub, testUser);
        Team team2 = TestDataFactory.createTestTeam("팀2", testClub, otherUser);
        teamRepository.save(team1);
        teamRepository.save(team2);

        // When & Then
        mockMvc.perform(get("/api/clubs/{clubId}/teams", testClub.getId())
                        .header("Authorization", "Bearer " + testUserToken)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("곡 팀 목록을 성공적으로 조회했습니다."))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    // === 팀 상세 정보 조회 테스트 ===

    @Test
    @DisplayName("팀 상세 정보 조회 - 성공")
    void getTeamDetail_Success() throws Exception {
        // Given
        Team team = TestDataFactory.createTestTeam("테스트 팀", testClub, testUser);
        team = teamRepository.save(team);

        // TeamMember 추가 (creator가 팀장 역할)
        TeamMember teamLeader = new TeamMember();
        teamLeader.setTeam(team);
        teamLeader.setUser(testUser);
        teamMemberRepository.save(teamLeader);

        // When & Then
        mockMvc.perform(get("/api/teams/{teamId}", team.getId())
                        .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("곡 팀 정보를 성공적으로 조회했습니다."))
                .andExpect(jsonPath("$.data.name").value("테스트 팀"));
    }

    @Test
    @DisplayName("팀 상세 정보 조회 - 존재하지 않는 팀")
    void getTeamDetail_NotFound() throws Exception {
        // Given
        Integer nonExistentTeamId = 99999;

        // When & Then
        mockMvc.perform(get("/api/teams/{teamId}", nonExistentTeamId)
                        .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("TEAM_NOT_FOUND"));
    }

    // === 팀 이름 수정 테스트 ===

    @Test
    @DisplayName("팀 이름 수정 - 성공 (팀장)")
    void updateTeam_Success_AsLeader() throws Exception {
        // Given
        Team team = TestDataFactory.createTestTeam("원래 팀 이름", testClub, testUser);
        team = teamRepository.save(team);

        // TeamMember를 LEADER로 추가
        TeamMember teamLeader = new TeamMember();
        teamLeader.setTeam(team);
        teamLeader.setUser(testUser);
        teamMemberRepository.save(teamLeader);

        TeamReqDTO request = new TeamReqDTO();
        request.setName("수정된 팀 이름");

        // When & Then
        mockMvc.perform(patch("/api/teams/{teamId}", team.getId())
                        .header("Authorization", "Bearer " + testUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("곡 팀 이름이 성공적으로 수정되었습니다."))
                .andExpect(jsonPath("$.data.name").value("수정된 팀 이름"));
    }

    @Test
    @DisplayName("팀 이름 수정 - 권한 없음 (일반 팀원)")
    void updateTeam_Forbidden_NotLeader() throws Exception {
        // Given
        Team team = TestDataFactory.createTestTeam("테스트 팀", testClub, testUser);
        team = teamRepository.save(team);

        // testUser를 LEADER로 추가
        TeamMember teamLeader = new TeamMember();
        teamLeader.setTeam(team);
        teamLeader.setUser(testUser);
        teamMemberRepository.save(teamLeader);

        // otherUser를 MEMBER로 추가
        TeamMember teamMember = new TeamMember();
        teamMember.setTeam(team);
        teamMember.setUser(otherUser);
        teamMemberRepository.save(teamMember);

        TeamReqDTO request = new TeamReqDTO();
        request.setName("수정된 팀 이름");

        // When & Then
        mockMvc.perform(patch("/api/teams/{teamId}", team.getId())
                        .header("Authorization", "Bearer " + otherUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // === 팀 삭제 테스트 ===

    @Test
    @DisplayName("팀 삭제 - 성공 (팀장)")
    void deleteTeam_Success_AsLeader() throws Exception {
        // Given
        Team team = TestDataFactory.createTestTeam("테스트 팀", testClub, testUser);
        team = teamRepository.save(team);

        // TeamMember를 LEADER로 추가
        TeamMember teamLeader = new TeamMember();
        teamLeader.setTeam(team);
        teamLeader.setUser(testUser);
        teamMemberRepository.save(teamLeader);

        // When & Then
        mockMvc.perform(delete("/api/teams/{teamId}", team.getId())
                        .header("Authorization", "Bearer " + testUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("곡 팀이 성공적으로 삭제되었습니다."));
    }

    @Test
    @DisplayName("팀 삭제 - 권한 없음 (일반 팀원)")
    void deleteTeam_Forbidden_NotLeader() throws Exception {
        // Given
        Team team = TestDataFactory.createTestTeam("테스트 팀", testClub, testUser);
        team = teamRepository.save(team);

        // testUser를 LEADER로 추가
        TeamMember teamLeader = new TeamMember();
        teamLeader.setTeam(team);
        teamLeader.setUser(testUser);
        teamMemberRepository.save(teamLeader);

        // otherUser를 MEMBER로 추가
        TeamMember teamMember = new TeamMember();
        teamMember.setTeam(team);
        teamMember.setUser(otherUser);
        teamMemberRepository.save(teamMember);

        // When & Then
        mockMvc.perform(delete("/api/teams/{teamId}", team.getId())
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isBadRequest());
    }

    // === 팀 탈퇴 테스트 ===

    @Test
    @DisplayName("팀 탈퇴 - 성공")
    void leaveTeam_Success() throws Exception {
        // Given
        Team team = TestDataFactory.createTestTeam("테스트 팀", testClub, testUser);
        team = teamRepository.save(team);

        // testUser를 LEADER로 추가
        TeamMember teamLeader = new TeamMember();
        teamLeader.setTeam(team);
        teamLeader.setUser(testUser);
        teamMemberRepository.save(teamLeader);

        // otherUser를 MEMBER로 추가
        TeamMember teamMember = new TeamMember();
        teamMember.setTeam(team);
        teamMember.setUser(otherUser);
        teamMemberRepository.save(teamMember);

        // When & Then
        mockMvc.perform(delete("/api/teams/{teamId}/members/me", team.getId())
                        .header("Authorization", "Bearer " + otherUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("팀에서 성공적으로 탈퇴했습니다."));
    }
}
