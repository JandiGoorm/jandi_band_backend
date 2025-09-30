package com.jandi.band_backend.poll.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jandi.band_backend.poll.dto.*;
import com.jandi.band_backend.poll.service.PollService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PollController 단위 테스트
 * Spring Context 없이 MockitoExtension을 사용하여 순수 단위 테스트 수행
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PollController 단위 테스트")
class PollControllerUnitTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private PollService pollService;

    @InjectMocks
    private PollController pollController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(pollController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    }

    @Test
    @DisplayName("투표 생성 - 정상 처리")
    void createPoll_Success() throws Exception {
        // Given
        PollReqDTO pollReqDTO = PollReqDTO.builder()
                .title("5월 정기공연 곡 투표")
                .clubId(1)
                .endDatetime(LocalDateTime.now().plusDays(7))
                .build();

        PollRespDTO pollRespDTO = PollRespDTO.builder()
                .id(1)
                .title("5월 정기공연 곡 투표")
                .clubId(1)
                .clubName("록밴드 동아리")
                .creatorId(1)
                .creatorName("테스트사용자")
                .startDatetime(LocalDateTime.now())
                .endDatetime(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .build();

        when(pollService.createPoll(any(PollReqDTO.class), eq(1)))
                .thenReturn(pollRespDTO);

        // When & Then
        mockMvc.perform(post("/api/polls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pollReqDTO))
                        .requestAttr("currentUserId", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("5월 정기공연 곡 투표"))
                .andExpect(jsonPath("$.data.clubId").value(1));

        verify(pollService).createPoll(any(PollReqDTO.class), eq(1));
    }

    @Test
    @DisplayName("투표 상세 조회 - 정상 처리")
    void getPollDetail_Success() throws Exception {
        // Given
        PollDetailRespDTO pollDetailRespDTO = PollDetailRespDTO.builder()
                .id(1)
                .title("5월 정기공연 곡 투표")
                .clubId(1)
                .clubName("록밴드 동아리")
                .creatorId(1)
                .creatorName("테스트사용자")
                .startDatetime(LocalDateTime.now().minusDays(1))
                .endDatetime(LocalDateTime.now().plusDays(6))
                .songs(Arrays.asList())
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        when(pollService.getPollDetail(eq(1), eq(1)))
                .thenReturn(pollDetailRespDTO);

        // When & Then
        mockMvc.perform(get("/api/polls/1")
                        .requestAttr("currentUserId", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("5월 정기공연 곡 투표"))
                .andExpect(jsonPath("$.data.clubId").value(1));

        verify(pollService).getPollDetail(eq(1), eq(1));
    }

    @Test
    @DisplayName("투표에 곡 추가 - 정상 처리")
    void addSongToPoll_Success() throws Exception {
        // Given
        PollSongReqDTO pollSongReqDTO = PollSongReqDTO.builder()
                .songName("Bohemian Rhapsody")
                .artistName("Queen")
                .youtubeUrl("https://www.youtube.com/watch?v=fJ9rUzIMcZQ")
                .description("대표적인 록 명곡")
                .build();

        PollSongRespDTO pollSongRespDTO = PollSongRespDTO.builder()
                .id(1)
                .pollId(1)
                .songName("Bohemian Rhapsody")
                .artistName("Queen")
                .youtubeUrl("https://www.youtube.com/watch?v=fJ9rUzIMcZQ")
                .description("대표적인 록 명곡")
                .suggesterId(1)
                .suggesterName("테스트사용자")
                .createdAt(LocalDateTime.now())
                .likeCount(0)
                .dislikeCount(0)
                .cantCount(0)
                .hajjCount(0)
                .build();

        when(pollService.addSongToPoll(eq(1), any(PollSongReqDTO.class), eq(1)))
                .thenReturn(pollSongRespDTO);

        // When & Then
        mockMvc.perform(post("/api/polls/1/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pollSongReqDTO))
                        .requestAttr("currentUserId", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.songName").value("Bohemian Rhapsody"))
                .andExpect(jsonPath("$.data.artistName").value("Queen"));

        verify(pollService).addSongToPoll(eq(1), any(PollSongReqDTO.class), eq(1));
    }

    @Test
    @DisplayName("투표 곡 목록 조회 - 정상 처리")
    void getPollSongs_Success() throws Exception {
        // Given
        List<PollSongResultRespDTO> songs = Arrays.asList(
                PollSongResultRespDTO.builder()
                        .id(1)
                        .pollId(1)
                        .songName("Bohemian Rhapsody")
                        .artistName("Queen")
                        .createdAt(LocalDateTime.now())
                        .likeCount(10)
                        .dislikeCount(2)
                        .cantCount(1)
                        .hajjCount(3)
                        .build(),
                PollSongResultRespDTO.builder()
                        .id(2)
                        .pollId(1)
                        .songName("Hotel California")
                        .artistName("Eagles")
                        .createdAt(LocalDateTime.now())
                        .likeCount(8)
                        .dislikeCount(1)
                        .cantCount(2)
                        .hajjCount(1)
                        .build()
        );

        when(pollService.getPollSongs(eq(1), eq("LIKE"), eq("desc")))
                .thenReturn(songs);

        // When & Then
        mockMvc.perform(get("/api/polls/1/songs")
                        .param("sort", "all")
                        .param("search", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].songName").value("Bohemian Rhapsody"))
                .andExpect(jsonPath("$.data[1].songName").value("Hotel California"));

        verify(pollService).getPollSongs(eq(1), eq("all"), eq(""));
    }

    @Test
    @DisplayName("곡에 투표하기 - 정상 처리")
    void setVoteForSong_Success() throws Exception {
        // Given
        PollSongRespDTO pollSongRespDTO = PollSongRespDTO.builder()
                .id(1)
                .pollId(1)
                .songName("Bohemian Rhapsody")
                .artistName("Queen")
                .youtubeUrl("https://www.youtube.com/watch?v=fJ9rUzIMcZQ")
                .suggesterId(1)
                .suggesterName("사용자1")
                .createdAt(LocalDateTime.now())
                .likeCount(11) // 투표 후 증가
                .dislikeCount(2)
                .cantCount(1)
                .hajjCount(3)
                .userVoteType("LIKE") // 사용자 투표 정보
                .build();

        when(pollService.setVoteForSong(eq(1), eq(1), eq("LIKE"), eq(1)))
                .thenReturn(pollSongRespDTO);

        // When & Then
        mockMvc.perform(put("/api/polls/1/songs/1/votes/LIKE")
                        .requestAttr("currentUserId", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(11))
                .andExpect(jsonPath("$.data.userVoteType").value("LIKE"));

        verify(pollService).setVoteForSong(eq(1), eq(1), eq("LIKE"), eq(1));
    }

    @Test
    @DisplayName("곡 투표 취소 - 정상 처리")
    void removeVoteFromSong_Success() throws Exception {
        // Given
        PollSongRespDTO pollSongRespDTO = PollSongRespDTO.builder()
                .id(1)
                .pollId(1)
                .songName("Bohemian Rhapsody")
                .artistName("Queen")
                .youtubeUrl("https://www.youtube.com/watch?v=fJ9rUzIMcZQ")
                .suggesterId(1)
                .suggesterName("사용자1")
                .createdAt(LocalDateTime.now())
                .likeCount(9) // 투표 취소 후 감소
                .dislikeCount(2)
                .cantCount(1)
                .hajjCount(3)
                .userVoteType(null) // 투표 취소됨
                .build();

        when(pollService.removeVoteFromSong(eq(1), eq(1), eq("LIKE"), eq(1)))
                .thenReturn(pollSongRespDTO);

        // When & Then
        mockMvc.perform(delete("/api/polls/1/songs/1/votes/LIKE")
                        .requestAttr("currentUserId", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(9))
                .andExpect(jsonPath("$.data.userVoteType").doesNotExist());

        verify(pollService).removeVoteFromSong(eq(1), eq(1), eq("LIKE"), eq(1));
    }

    @Test
    @DisplayName("투표 생성 - 잘못된 요청 데이터")
    void createPoll_InvalidRequest() throws Exception {
        // Given - title이 없는 잘못된 요청
        PollReqDTO invalidPollReqDTO = PollReqDTO.builder()
                .clubId(1)
                .endDatetime(LocalDateTime.now().plusDays(7))
                .build();

        // When & Then
        mockMvc.perform(post("/api/polls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidPollReqDTO))
                        .requestAttr("currentUserId", 1))
                .andExpect(status().isBadRequest());

        verify(pollService, never()).createPoll(any(), any());
    }

    @Test
    @DisplayName("곡 추가 - 잘못된 곡 정보")
    void addSongToPoll_InvalidSongData() throws Exception {
        // Given - 곡명이 없는 잘못된 요청
        PollSongReqDTO invalidSongReqDTO = PollSongReqDTO.builder()
                .artistName("Queen")
                .youtubeUrl("https://www.youtube.com/watch?v=fJ9rUzIMcZQ")
                .build();

        // When & Then
        mockMvc.perform(post("/api/polls/1/songs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidSongReqDTO))
                        .requestAttr("currentUserId", 1))
                .andExpect(status().isBadRequest());

        verify(pollService, never()).addSongToPoll(any(), any(), any());
    }

    @Test
    @DisplayName("존재하지 않는 투표 조회")
    void getPollDetail_NotFound() throws Exception {
        // Given
        when(pollService.getPollDetail(eq(999), eq(1)))
                .thenThrow(new RuntimeException("투표를 찾을 수 없습니다"));

        // When & Then
        mockMvc.perform(get("/api/polls/999")
                        .requestAttr("currentUserId", 1))
                .andExpect(status().isInternalServerError());

        verify(pollService).getPollDetail(eq(999), eq(1));
    }
}