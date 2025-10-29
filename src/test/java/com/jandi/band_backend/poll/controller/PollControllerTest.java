package com.jandi.band_backend.poll.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jandi.band_backend.config.TestConfig;
import com.jandi.band_backend.poll.dto.*;
import com.jandi.band_backend.poll.service.PollService;
import com.jandi.band_backend.invite.service.JoinService;
import com.jandi.band_backend.invite.service.InviteUtilService;
import com.jandi.band_backend.invite.redis.InviteCodeService;
import com.jandi.band_backend.security.CustomUserDetails;
import com.jandi.band_backend.security.jwt.JwtTokenProvider;
import com.jandi.band_backend.user.entity.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("PollController API 테스트")
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class PollControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PollService pollService;

    @MockBean
    private JoinService joinService;

    @MockBean
    private InviteUtilService inviteUtilService;

    @MockBean
    private InviteCodeService inviteCodeService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private PollReqDTO validPollReqDTO;
    private PollRespDTO pollRespDTO;
    private PollDetailRespDTO pollDetailRespDTO;
    private CustomUserDetails authenticatedUser;

    @BeforeEach
    void setUp() {
        // 투표 생성 요청 DTO
        validPollReqDTO = PollReqDTO.builder()
                .title("5월 정기공연 곡 선정")
                .clubId(1)
                .endDatetime(LocalDateTime.now().plusDays(30))
                .build();

        // 투표 응답 DTO
        pollRespDTO = PollRespDTO.builder()
                .id(1)
                .title("5월 정기공연 곡 선정")
                .clubId(1)
                .clubName("테스트 동아리")
                .creatorId(1)
                .creatorName("테스트사용자")
                .startDatetime(LocalDateTime.now().minusDays(1))
                .endDatetime(LocalDateTime.now().plusDays(30))
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        // 투표 상세 응답 DTO
        pollDetailRespDTO = PollDetailRespDTO.builder()
                .id(1)
                .title("5월 정기공연 곡 선정")
                .clubId(1)
                .clubName("테스트 동아리")
                .creatorId(1)
                .startDatetime(LocalDateTime.now().minusDays(1))
                .endDatetime(LocalDateTime.now().plusDays(30))
                .songs(Arrays.asList())
                .build();

        Users userEntity = new Users();
        userEntity.setId(1);
        userEntity.setKakaoOauthId("mock-kakao-id");
        userEntity.setNickname("테스트사용자");
        userEntity.setAdminRole(Users.AdminRole.USER);
        userEntity.setIsRegistered(true);

        authenticatedUser = new CustomUserDetails(userEntity);

        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.isAccessToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getAuthentication(anyString())).thenAnswer(invocation ->
                new UsernamePasswordAuthenticationToken(
                        authenticatedUser,
                        "",
                        authenticatedUser.getAuthorities()
                ));
    }

    @Test
    @DisplayName("투표 생성 - 정상 케이스")
    void createPoll_Success() throws Exception {
        // Given - 서비스 mocking
        when(pollService.createPoll(any(PollReqDTO.class), eq(1))).thenReturn(pollRespDTO);

        // When & Then
        mockMvc.perform(post("/api/polls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPollReqDTO))
                        .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("투표 생성 - 인증되지 않은 사용자")
    void createPoll_Unauthorized() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/polls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPollReqDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("클럽별 투표 목록 조회 - 정상 케이스")
    void getPollList_Success() throws Exception {
        // Given
        List<PollRespDTO> pollList = Arrays.asList(pollRespDTO);
        Page<PollRespDTO> pollPage = new PageImpl<>(pollList, PageRequest.of(0, 5), 1);
        when(pollService.getPollsByClub(eq(1), any())).thenReturn(pollPage);

        // When & Then
        mockMvc.perform(get("/api/polls/clubs/1")
                        .param("page", "0")
                        .param("size", "5")
                        .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }

    @Test
    @DisplayName("투표 상세 조회 - 정상 케이스")
    void getPollDetail_Success() throws Exception {
        // Given
        when(pollService.getPollDetail(eq(1), any())).thenReturn(pollDetailRespDTO);

        // When & Then
        mockMvc.perform(get("/api/polls/1")
                        .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("투표 곡 삭제 - 정상 케이스")
    void deleteSongFromPoll_Success() throws Exception {
        // Given
        doNothing().when(pollService).deletePollSong(1, 2, 1);

        // When & Then
        mockMvc.perform(delete("/api/polls/1/songs/2")
                        .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("투표 곡이 성공적으로 삭제되었습니다."));

        verify(pollService).deletePollSong(1, 2, 1);
    }

    @Test
    @DisplayName("존재하지 않는 투표 조회")
    void getPollDetail_NotFound() throws Exception {
        // Given
        when(pollService.getPollDetail(eq(999), any())).thenThrow(new RuntimeException("투표를 찾을 수 없습니다"));

        // When & Then
        mockMvc.perform(get("/api/polls/999")
                        .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isBadRequest());
    }
}
