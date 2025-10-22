package com.jandi.band_backend.poll.service;

import com.jandi.band_backend.global.exception.BadRequestException;
import com.jandi.band_backend.global.exception.VoteNotFoundException;
import com.jandi.band_backend.global.util.EntityValidationUtil;
import com.jandi.band_backend.poll.dto.PollSongRespDTO;
import com.jandi.band_backend.poll.entity.Poll;
import com.jandi.band_backend.poll.entity.PollSong;
import com.jandi.band_backend.poll.entity.Vote;
import com.jandi.band_backend.poll.entity.Vote.VotedMark;
import com.jandi.band_backend.poll.repository.PollRepository;
import com.jandi.band_backend.poll.repository.PollSongRepository;
import com.jandi.band_backend.poll.repository.VoteRepository;
import com.jandi.band_backend.user.entity.UserPhoto;
import com.jandi.band_backend.user.entity.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PollService 투표 취소 테스트")
class PollVoteCancelServiceTest {

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
    private PollSong testPollSong;
    private Vote testVote;

    @BeforeEach
    void setUp() {
        testPoll = new Poll();
        testPoll.setId(1);
        testPoll.setTitle("테스트 투표");
        testPoll.setEndDatetime(LocalDateTime.now().plusDays(30));

        testUser = new Users();
        testUser.setId(1);
        testUser.setNickname("테스트사용자");

        UserPhoto userPhoto = new UserPhoto();
        userPhoto.setImageUrl("https://example.com/profile.jpg");
        userPhoto.setIsCurrent(true);
        userPhoto.setDeletedAt(null);
        testUser.setPhotos(List.of(userPhoto));

        testPollSong = new PollSong();
        testPollSong.setId(1);
        testPollSong.setPoll(testPoll);
        testPollSong.setSongName("Bohemian Rhapsody");
        testPollSong.setArtistName("Queen");
        testPollSong.setSuggester(testUser);
        testPollSong.setVotes(Collections.emptyList());

        testVote = new Vote();
        testVote.setId(1);
        testVote.setPollSong(testPollSong);
        testVote.setUser(testUser);
        testVote.setVotedMark(VotedMark.LIKE);
    }

    @Test
    @DisplayName("35. 정상 케이스 - 투표 취소 성공")
    void removeVoteFromSong_Success() {
        // Given
        when(entityValidationUtil.validatePollSongBelongsToPoll(1, 1)).thenReturn(testPollSong);
        when(voteRepository.findByPollSongIdAndUserIdAndVotedMarkAndDeletedAtIsNull(1, 1, VotedMark.LIKE))
                .thenReturn(Optional.of(testVote));

        // When
        PollSongRespDTO result = pollService.removeVoteFromSong(1, 1, "LIKE", 1);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Bohemian Rhapsody", result.getSongName());
        assertEquals("Queen", result.getArtistName());
        assertNull(result.getUserVoteType()); // 투표 취소 후 null
        assertEquals(0, result.getLikeCount());
        assertEquals(0, result.getDislikeCount());
        assertEquals(0, result.getCantCount());
        assertEquals(0, result.getHajjCount());

        verify(entityValidationUtil).validatePollSongBelongsToPoll(1, 1);
        verify(voteRepository).findByPollSongIdAndUserIdAndVotedMarkAndDeletedAtIsNull(1, 1, VotedMark.LIKE);
        verify(voteRepository).delete(testVote);
    }

    @Test
    @DisplayName("36. 투표하지 않은 항목의 투표 취소 시도")
    void removeVoteFromSong_ThrowsException_VoteNotFound() {
        // Given
        when(entityValidationUtil.validatePollSongBelongsToPoll(1, 1)).thenReturn(testPollSong);
        when(voteRepository.findByPollSongIdAndUserIdAndVotedMarkAndDeletedAtIsNull(1, 1, VotedMark.LIKE))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(VoteNotFoundException.class,
                () -> pollService.removeVoteFromSong(1, 1, "LIKE", 1));

        verify(entityValidationUtil).validatePollSongBelongsToPoll(1, 1);
        verify(voteRepository).findByPollSongIdAndUserIdAndVotedMarkAndDeletedAtIsNull(1, 1, VotedMark.LIKE);
        verify(voteRepository, never()).delete(any());
    }

    @Test
    @DisplayName("37. 존재하지 않는 투표 곡의 투표 취소")
    void removeVoteFromSong_ThrowsException_PollSongNotFound() {
        // Given
        when(entityValidationUtil.validatePollSongBelongsToPoll(1, 999))
                .thenThrow(new RuntimeException("투표 곡을 찾을 수 없습니다."));

        // When & Then
        assertThrows(RuntimeException.class,
                () -> pollService.removeVoteFromSong(1, 999, "LIKE", 1));

        verify(entityValidationUtil).validatePollSongBelongsToPoll(1, 999);
        verify(voteRepository, never()).findByPollSongIdAndUserIdAndVotedMarkAndDeletedAtIsNull(any(), any(), any());
        verify(voteRepository, never()).delete(any());
    }

    @Test
    @DisplayName("38. 다른 타입의 투표를 취소 시도")
    void removeVoteFromSong_ThrowsException_DifferentVoteType() {
        // Given - LIKE로 투표했지만 DISLIKE 취소 시도
        when(entityValidationUtil.validatePollSongBelongsToPoll(1, 1)).thenReturn(testPollSong);
        when(voteRepository.findByPollSongIdAndUserIdAndVotedMarkAndDeletedAtIsNull(1, 1, VotedMark.DISLIKE))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(VoteNotFoundException.class,
                () -> pollService.removeVoteFromSong(1, 1, "DISLIKE", 1));

        verify(entityValidationUtil).validatePollSongBelongsToPoll(1, 1);
        verify(voteRepository).findByPollSongIdAndUserIdAndVotedMarkAndDeletedAtIsNull(1, 1, VotedMark.DISLIKE);
        verify(voteRepository, never()).delete(any());
    }

    @Test
    @DisplayName("39. 존재하지 않는 사용자의 투표 취소")
    void removeVoteFromSong_ThrowsException_UserNotFound() {
        // Given
        when(entityValidationUtil.validatePollSongBelongsToPoll(1, 1)).thenReturn(testPollSong);
        when(voteRepository.findByPollSongIdAndUserIdAndVotedMarkAndDeletedAtIsNull(1, 999, VotedMark.LIKE))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(VoteNotFoundException.class,
                () -> pollService.removeVoteFromSong(1, 1, "LIKE", 999));

        verify(entityValidationUtil).validatePollSongBelongsToPoll(1, 1);
        verify(voteRepository).findByPollSongIdAndUserIdAndVotedMarkAndDeletedAtIsNull(1, 999, VotedMark.LIKE);
        verify(voteRepository, never()).delete(any());
    }

    @Test
    @DisplayName("40. 유효하지 않은 투표 타입으로 취소")
    void removeVoteFromSong_ThrowsException_InvalidVoteType() {
        // Given
        when(entityValidationUtil.validatePollSongBelongsToPoll(1, 1)).thenReturn(testPollSong);

        // When & Then
        assertThrows(BadRequestException.class,
                () -> pollService.removeVoteFromSong(1, 1, "INVALID_TYPE", 1));

        verify(entityValidationUtil).validatePollSongBelongsToPoll(1, 1);
        verify(voteRepository, never()).findByPollSongIdAndUserIdAndVotedMarkAndDeletedAtIsNull(any(), any(), any());
        verify(voteRepository, never()).delete(any());
    }

    @Test
    @DisplayName("41. null 투표 타입으로 취소")
    void removeVoteFromSong_ThrowsException_NullVoteType() {
        // Given
        when(entityValidationUtil.validatePollSongBelongsToPoll(1, 1)).thenReturn(testPollSong);

        // When & Then
        assertThrows(BadRequestException.class,
                () -> pollService.removeVoteFromSong(1, 1, null, 1));

        verify(entityValidationUtil).validatePollSongBelongsToPoll(1, 1);
        verify(voteRepository, never()).findByPollSongIdAndUserIdAndVotedMarkAndDeletedAtIsNull(any(), any(), any());
        verify(voteRepository, never()).delete(any());
    }

    @Test
    @DisplayName("42. 한국어 투표 타입으로 취소")
    void removeVoteFromSong_WithKoreanVoteType() {
        // Given
        when(entityValidationUtil.validatePollSongBelongsToPoll(1, 1)).thenReturn(testPollSong);
        when(voteRepository.findByPollSongIdAndUserIdAndVotedMarkAndDeletedAtIsNull(1, 1, VotedMark.LIKE))
                .thenReturn(Optional.of(testVote));

        // When - 한국어 투표 타입으로 취소
        PollSongRespDTO result = pollService.removeVoteFromSong(1, 1, "좋아요", 1);

        // Then
        assertNotNull(result);
        assertEquals("Bohemian Rhapsody", result.getSongName());
        assertNull(result.getUserVoteType());

        verify(entityValidationUtil).validatePollSongBelongsToPoll(1, 1);
        verify(voteRepository).findByPollSongIdAndUserIdAndVotedMarkAndDeletedAtIsNull(1, 1, VotedMark.LIKE);
        verify(voteRepository).delete(testVote);
    }

    @Test
    @DisplayName("43. 대소문자 구분 없는 투표 타입으로 취소")
    void removeVoteFromSong_WithCaseInsensitiveVoteType() {
        // Given
        when(entityValidationUtil.validatePollSongBelongsToPoll(1, 1)).thenReturn(testPollSong);
        when(voteRepository.findByPollSongIdAndUserIdAndVotedMarkAndDeletedAtIsNull(1, 1, VotedMark.LIKE))
                .thenReturn(Optional.of(testVote));

        // When - 소문자로 투표 타입 전송
        PollSongRespDTO result = pollService.removeVoteFromSong(1, 1, "like", 1);

        // Then
        assertNotNull(result);
        assertEquals("Bohemian Rhapsody", result.getSongName());
        assertNull(result.getUserVoteType());

        verify(entityValidationUtil).validatePollSongBelongsToPoll(1, 1);
        verify(voteRepository).findByPollSongIdAndUserIdAndVotedMarkAndDeletedAtIsNull(1, 1, VotedMark.LIKE);
        verify(voteRepository).delete(testVote);
    }

    @Test
    @DisplayName("44. Repository 삭제 중 예외 발생")
    void removeVoteFromSong_ThrowsException_RepositoryDeleteFailure() {
        // Given
        when(entityValidationUtil.validatePollSongBelongsToPoll(1, 1)).thenReturn(testPollSong);
        when(voteRepository.findByPollSongIdAndUserIdAndVotedMarkAndDeletedAtIsNull(1, 1, VotedMark.LIKE))
                .thenReturn(Optional.of(testVote));
        doThrow(new RuntimeException("데이터베이스 삭제 오류")).when(voteRepository).delete(testVote);

        // When & Then
        assertThrows(RuntimeException.class,
                () -> pollService.removeVoteFromSong(1, 1, "LIKE", 1));

        verify(entityValidationUtil).validatePollSongBelongsToPoll(1, 1);
        verify(voteRepository).findByPollSongIdAndUserIdAndVotedMarkAndDeletedAtIsNull(1, 1, VotedMark.LIKE);
        verify(voteRepository).delete(testVote);
    }

    @Test
    @DisplayName("45. 프로필 사진이 없는 사용자의 투표 취소")
    void removeVoteFromSong_WithUserNoProfilePhoto() {
        // Given
        Users userWithoutPhoto = new Users();
        userWithoutPhoto.setId(1);
        userWithoutPhoto.setNickname("사진없는사용자");
        userWithoutPhoto.setPhotos(Collections.emptyList());

        PollSong pollSongWithoutPhoto = new PollSong();
        pollSongWithoutPhoto.setId(1);
        pollSongWithoutPhoto.setPoll(testPoll);
        pollSongWithoutPhoto.setSongName("Bohemian Rhapsody");
        pollSongWithoutPhoto.setArtistName("Queen");
        pollSongWithoutPhoto.setSuggester(userWithoutPhoto);
        pollSongWithoutPhoto.setVotes(Collections.emptyList());

        when(entityValidationUtil.validatePollSongBelongsToPoll(1, 1)).thenReturn(pollSongWithoutPhoto);
        when(voteRepository.findByPollSongIdAndUserIdAndVotedMarkAndDeletedAtIsNull(1, 1, VotedMark.LIKE))
                .thenReturn(Optional.of(testVote));

        // When
        PollSongRespDTO result = pollService.removeVoteFromSong(1, 1, "LIKE", 1);

        // Then
        assertNotNull(result);
        assertEquals("사진없는사용자", result.getSuggesterName());
        assertNull(result.getSuggesterProfilePhoto()); // 프로필 사진이 null이어야 함
        assertNull(result.getUserVoteType());

        verify(entityValidationUtil).validatePollSongBelongsToPoll(1, 1);
        verify(voteRepository).findByPollSongIdAndUserIdAndVotedMarkAndDeletedAtIsNull(1, 1, VotedMark.LIKE);
        verify(voteRepository).delete(testVote);
    }

    @Test
    @DisplayName("46. 마감된 투표의 투표 취소")
    void removeVoteFromSong_WithExpiredPoll() {
        // Given
        Poll expiredPoll = new Poll();
        expiredPoll.setId(1);
        expiredPoll.setTitle("마감된 투표");
        expiredPoll.setEndDatetime(LocalDateTime.now().minusDays(1)); // 이미 마감

        PollSong expiredPollSong = new PollSong();
        expiredPollSong.setId(1);
        expiredPollSong.setPoll(expiredPoll);
        expiredPollSong.setSongName("Bohemian Rhapsody");
        expiredPollSong.setArtistName("Queen");
        expiredPollSong.setSuggester(testUser);
        expiredPollSong.setVotes(Collections.emptyList());

        when(entityValidationUtil.validatePollSongBelongsToPoll(1, 1)).thenReturn(expiredPollSong);
        when(voteRepository.findByPollSongIdAndUserIdAndVotedMarkAndDeletedAtIsNull(1, 1, VotedMark.LIKE))
                .thenReturn(Optional.of(testVote));

        // When - 마감된 투표에도 투표 취소는 가능 (비즈니스 로직에 따라)
        PollSongRespDTO result = pollService.removeVoteFromSong(1, 1, "LIKE", 1);

        // Then
        assertNotNull(result);
        assertEquals("Bohemian Rhapsody", result.getSongName());
        assertNull(result.getUserVoteType());

        verify(entityValidationUtil).validatePollSongBelongsToPoll(1, 1);
        verify(voteRepository).findByPollSongIdAndUserIdAndVotedMarkAndDeletedAtIsNull(1, 1, VotedMark.LIKE);
        verify(voteRepository).delete(testVote);
    }
}
