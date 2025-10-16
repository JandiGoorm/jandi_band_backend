package com.jandi.band_backend.poll.service;

import com.jandi.band_backend.global.exception.BadRequestException;
import com.jandi.band_backend.global.util.EntityValidationUtil;
import com.jandi.band_backend.poll.dto.PollSongResultRespDTO;
import com.jandi.band_backend.poll.entity.Poll;
import com.jandi.band_backend.poll.entity.PollSong;
import com.jandi.band_backend.poll.entity.Vote;
import com.jandi.band_backend.poll.entity.Vote.VotedMark;
import com.jandi.band_backend.poll.repository.PollRepository;
import com.jandi.band_backend.poll.repository.PollSongRepository;
import com.jandi.band_backend.poll.repository.VoteRepository;
import com.jandi.band_backend.user.entity.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PollService 정렬 및 계산 로직 테스트")
class PollSortingServiceTest {

    @InjectMocks
    private PollService pollService;

    @Mock
    private PollRepository pollRepository;

    @Mock
    private PollSongRepository pollSongRepository;

    @Mock
    private VoteRepository voteRepository;

    @Mock
    private EntityValidationUtil entityValidationUtil;

    private Poll testPoll;
    private Users testUser;
    private PollSong song1, song2, song3;

    @BeforeEach
    void setUp() {
        testPoll = new Poll();
        testPoll.setId(1);
        testPoll.setTitle("테스트 투표");

        testUser = new Users();
        testUser.setId(1);
        testUser.setNickname("테스트사용자");

        song1 = createPollSong(1, "Song A", "Artist A", createVotes(5, 1, 0, 2)); // 점수: (5+2) - (1+0) = 6
        song2 = createPollSong(2, "Song B", "Artist B", createVotes(3, 2, 1, 1)); // 점수: (3+1) - (2+1) = 1
        song3 = createPollSong(3, "Song C", "Artist C", createVotes(8, 0, 0, 0)); // 점수: (8+0) - (0+0) = 8
    }

    private PollSong createPollSong(Integer id, String songName, String artistName, List<Vote> votes) {
        PollSong pollSong = new PollSong();
        pollSong.setId(id);
        pollSong.setPoll(testPoll);
        pollSong.setSongName(songName);
        pollSong.setArtistName(artistName);
        pollSong.setSuggester(testUser);
        pollSong.setVotes(votes);
        pollSong.setCreatedAt(LocalDateTime.now());
        return pollSong;
    }

    private List<Vote> createVotes(int likeCount, int dislikeCount, int cantCount, int hajjCount) {
        List<Vote> votes = new java.util.ArrayList<>();

        for (int i = 0; i < likeCount; i++) {
            Vote vote = new Vote();
            vote.setVotedMark(VotedMark.LIKE);
            votes.add(vote);
        }

        for (int i = 0; i < dislikeCount; i++) {
            Vote vote = new Vote();
            vote.setVotedMark(VotedMark.DISLIKE);
            votes.add(vote);
        }

        for (int i = 0; i < cantCount; i++) {
            Vote vote = new Vote();
            vote.setVotedMark(VotedMark.CANT);
            votes.add(vote);
        }

        for (int i = 0; i < hajjCount; i++) {
            Vote vote = new Vote();
            vote.setVotedMark(VotedMark.HAJJ);
            votes.add(vote);
        }

        return votes;
    }

    @Test
    @DisplayName("59. 정상 케이스 - LIKE 수 기준 내림차순 정렬")
    void getPollSongs_SortByLike_Desc() {
        // Given
        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(pollSongRepository.findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(testPoll))
                .thenReturn(Arrays.asList(song1, song2, song3));

        // When
        List<PollSongResultRespDTO> result = pollService.getPollSongs(1, "LIKE", "desc", 1);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());

        // Song C (8) > Song A (5) > Song B (3) 순서
        assertEquals("Song C", result.get(0).getSongName());
        assertEquals(8, result.get(0).getLikeCount());

        assertEquals("Song A", result.get(1).getSongName());
        assertEquals(5, result.get(1).getLikeCount());

        assertEquals("Song B", result.get(2).getSongName());
        assertEquals(3, result.get(2).getLikeCount());

        verify(entityValidationUtil).validatePollExists(1);
        verify(pollSongRepository).findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(testPoll);
    }

    @Test
    @DisplayName("60. LIKE 수 기준 오름차순 정렬")
    void getPollSongs_SortByLike_Asc() {
        // Given
        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(pollSongRepository.findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(testPoll))
                .thenReturn(Arrays.asList(song1, song2, song3));

        // When
        List<PollSongResultRespDTO> result = pollService.getPollSongs(1, "LIKE", "asc", 1);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());

        // Song B (3) > Song A (5) > Song C (8) 순서
        assertEquals("Song B", result.get(0).getSongName());
        assertEquals(3, result.get(0).getLikeCount());

        assertEquals("Song A", result.get(1).getSongName());
        assertEquals(5, result.get(1).getLikeCount());

        assertEquals("Song C", result.get(2).getSongName());
        assertEquals(8, result.get(2).getLikeCount());
    }

    @Test
    @DisplayName("61. DISLIKE 수 기준 내림차순 정렬")
    void getPollSongs_SortByDislike_Desc() {
        // Given
        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(pollSongRepository.findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(testPoll))
                .thenReturn(Arrays.asList(song1, song2, song3));

        // When
        List<PollSongResultRespDTO> result = pollService.getPollSongs(1, "DISLIKE", "desc", 1);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());

        // Song B (2) > Song A (1) > Song C (0) 순서
        assertEquals("Song B", result.get(0).getSongName());
        assertEquals(2, result.get(0).getDislikeCount());

        assertEquals("Song A", result.get(1).getSongName());
        assertEquals(1, result.get(1).getDislikeCount());

        assertEquals("Song C", result.get(2).getSongName());
        assertEquals(0, result.get(2).getDislikeCount());
    }

    @Test
    @DisplayName("62. SCORE 기준 내림차순 정렬")
    void getPollSongs_SortByScore_Desc() {
        // Given
        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(pollSongRepository.findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(testPoll))
                .thenReturn(Arrays.asList(song1, song2, song3));

        // When
        List<PollSongResultRespDTO> result = pollService.getPollSongs(1, "SCORE", "desc", 1);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());

        // Song C (8) > Song A (6) > Song B (1) 순서
        assertEquals("Song C", result.get(0).getSongName());
        assertEquals(8, result.get(0).getLikeCount() + result.get(0).getHajjCount()
                - result.get(0).getDislikeCount() - result.get(0).getCantCount());

        assertEquals("Song A", result.get(1).getSongName());
        assertEquals(6, result.get(1).getLikeCount() + result.get(1).getHajjCount()
                - result.get(1).getDislikeCount() - result.get(1).getCantCount());

        assertEquals("Song B", result.get(2).getSongName());
        assertEquals(1, result.get(2).getLikeCount() + result.get(2).getHajjCount()
                - result.get(2).getDislikeCount() - result.get(2).getCantCount());
    }

    @Test
    @DisplayName("63. SCORE 기준 오름차순 정렬")
    void getPollSongs_SortByScore_Asc() {
        // Given
        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(pollSongRepository.findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(testPoll))
                .thenReturn(Arrays.asList(song1, song2, song3));

        // When
        List<PollSongResultRespDTO> result = pollService.getPollSongs(1, "SCORE", "asc", 1);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());

        // Song B (1) > Song A (6) > Song C (8) 순서
        assertEquals("Song B", result.get(0).getSongName());
        assertEquals("Song A", result.get(1).getSongName());
        assertEquals("Song C", result.get(2).getSongName());
    }

    @Test
    @DisplayName("64. 유효하지 않은 정렬 기준")
    void getPollSongs_ThrowsException_InvalidSortBy() {
        // Given
        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(pollSongRepository.findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(testPoll))
                .thenReturn(Arrays.asList(song1, song2, song3));

        // When & Then
        assertThrows(BadRequestException.class,
                () -> pollService.getPollSongs(1, "INVALID", "desc", 1));

        verify(entityValidationUtil).validatePollExists(1);
        verify(pollSongRepository).findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(testPoll);
    }

    @Test
    @DisplayName("65. null 정렬 기준")
    void getPollSongs_ThrowsException_NullSortBy() {
        // Given
        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(pollSongRepository.findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(testPoll))
                .thenReturn(Arrays.asList(song1, song2, song3));

        // When & Then
        assertThrows(BadRequestException.class,
                () -> pollService.getPollSongs(1, null, "desc", 1));

        verify(entityValidationUtil).validatePollExists(1);
        verify(pollSongRepository).findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(testPoll);
    }

    @Test
    @DisplayName("66. 빈 곡 목록에 대한 정렬 적용")
    void getPollSongs_WithEmptyList() {
        // Given
        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(pollSongRepository.findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(testPoll))
                .thenReturn(Collections.emptyList());

        // When
        List<PollSongResultRespDTO> result = pollService.getPollSongs(1, "LIKE", "desc", 1);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(entityValidationUtil).validatePollExists(1);
        verify(pollSongRepository).findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(testPoll);
    }

    @Test
    @DisplayName("67. 모든 투표 수가 0인 곡들의 정렬")
    void getPollSongs_WithZeroVotes() {
        // Given
        PollSong zeroVoteSong1 = createPollSong(4, "Zero Song 1", "Artist D", Collections.emptyList());
        PollSong zeroVoteSong2 = createPollSong(5, "Zero Song 2", "Artist E", Collections.emptyList());

        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(pollSongRepository.findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(testPoll))
                .thenReturn(Arrays.asList(zeroVoteSong1, zeroVoteSong2));

        // When
        List<PollSongResultRespDTO> result = pollService.getPollSongs(1, "LIKE", "desc", 1);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        // 모두 0이므로 원래 순서 유지
        assertEquals("Zero Song 1", result.get(0).getSongName());
        assertEquals(0, result.get(0).getLikeCount());

        assertEquals("Zero Song 2", result.get(1).getSongName());
        assertEquals(0, result.get(1).getLikeCount());
    }

    @Test
    @DisplayName("68. 동점 곡들의 정렬 순서")
    void getPollSongs_WithTiedScores() {
        // Given
        PollSong tiedSong1 = createPollSong(6, "Tied Song 1", "Artist F", createVotes(3, 0, 0, 0)); // 점수: 3
        PollSong tiedSong2 = createPollSong(7, "Tied Song 2", "Artist G", createVotes(3, 0, 0, 0)); // 점수: 3

        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(pollSongRepository.findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(testPoll))
                .thenReturn(Arrays.asList(tiedSong1, tiedSong2));

        // When
        List<PollSongResultRespDTO> result = pollService.getPollSongs(1, "LIKE", "desc", 1);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        // 동점이므로 원래 순서 유지
        assertEquals("Tied Song 1", result.get(0).getSongName());
        assertEquals(3, result.get(0).getLikeCount());

        assertEquals("Tied Song 2", result.get(1).getSongName());
        assertEquals(3, result.get(1).getLikeCount());
    }

    @Test
    @DisplayName("69. 음수 점수를 가진 곡들의 정렬")
    void getPollSongs_WithNegativeScores() {
        // Given
        PollSong negativeSong1 = createPollSong(8, "Negative Song 1", "Artist H", createVotes(1, 5, 2, 0)); // 점수: (1+0) - (5+2) = -6
        PollSong negativeSong2 = createPollSong(9, "Negative Song 2", "Artist I", createVotes(0, 3, 1, 0)); // 점수: (0+0) - (3+1) = -4

        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(pollSongRepository.findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(testPoll))
                .thenReturn(Arrays.asList(negativeSong1, negativeSong2));

        // When
        List<PollSongResultRespDTO> result = pollService.getPollSongs(1, "SCORE", "desc", 1);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());

        // Negative Song 2 (-4) > Negative Song 1 (-6) 순서
        assertEquals("Negative Song 2", result.get(0).getSongName());
        assertEquals("Negative Song 1", result.get(1).getSongName());
    }

    @Test
    @DisplayName("70. 대소문자 구분 없는 정렬 기준 처리")
    void getPollSongs_WithCaseInsensitiveSortBy() {
        // Given
        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(pollSongRepository.findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(testPoll))
                .thenReturn(Arrays.asList(song1, song2, song3));

        // When - 소문자로 정렬 기준 전송
        List<PollSongResultRespDTO> result = pollService.getPollSongs(1, "like", "desc", 1);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());

        // LIKE 수 기준 내림차순 정렬 확인
        assertEquals("Song C", result.get(0).getSongName());
        assertEquals(8, result.get(0).getLikeCount());
    }

    @Test
    @DisplayName("71. 대소문자 구분 없는 정렬 순서 처리")
    void getPollSongs_WithCaseInsensitiveOrder() {
        // Given
        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(pollSongRepository.findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(testPoll))
                .thenReturn(Arrays.asList(song1, song2, song3));

        // When - 대문자로 정렬 순서 전송
        List<PollSongResultRespDTO> result = pollService.getPollSongs(1, "LIKE", "ASC", 1);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());

        // LIKE 수 기준 오름차순 정렬 확인
        assertEquals("Song B", result.get(0).getSongName());
        assertEquals(3, result.get(0).getLikeCount());
    }

    @Test
    @DisplayName("72. 기본 정렬 순서 (desc) 확인")
    void getPollSongs_WithDefaultOrder() {
        // Given
        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(pollSongRepository.findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(testPoll))
                .thenReturn(Arrays.asList(song1, song2, song3));

        // When - 정렬 순서를 지정하지 않거나 잘못된 값 전송 (기본값은 desc)
        List<PollSongResultRespDTO> result = pollService.getPollSongs(1, "LIKE", "invalid_order", 1);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());

        // 기본값인 내림차순 정렬 확인
        assertEquals("Song C", result.get(0).getSongName());
        assertEquals(8, result.get(0).getLikeCount());
    }
}
