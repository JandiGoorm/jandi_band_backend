package com.jandi.band_backend.poll.service;

import com.jandi.band_backend.global.exception.BadRequestException;
import com.jandi.band_backend.global.exception.VoteAlreadyExistsException;
import com.jandi.band_backend.global.util.EntityValidationUtil;
import com.jandi.band_backend.global.util.UserValidationUtil;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PollService 투표 참여 테스트")
class PollVotingServiceTest {

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

    @Mock
    private UserValidationUtil userValidationUtil;

    private Poll testPoll;
    private Users testUser;
    private PollSong testPollSong;
    private Vote testVote;

    @BeforeEach
    void setUp() {
        testPoll = new Poll();
        testPoll.setId(1);
        testPoll.setTitle("테스트 투표");
        testPoll.setEndDatetime(LocalDateTime.now().plusDays(30)); // 아직 마감되지 않음

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
        testPollSong.setVotes(new ArrayList<>());

        testVote = new Vote();
        testVote.setId(1);
        testVote.setPollSong(testPollSong);
        testVote.setUser(testUser);
        testVote.setVotedMark(VotedMark.LIKE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"LIKE", "like", "좋아요", "DISLIKE", "별로에요", "CANT", "실력부족", "HAJJ", "하고싶지_않은데_존중해요"})
    @DisplayName("22. 정상 케이스 - 다양한 투표 타입으로 곡에 투표 성공")
    void setVoteForSong_WithVariousVoteTypes_Success(String voteType) {
        // Given
        when(userValidationUtil.getUserById(1)).thenReturn(testUser);
        when(entityValidationUtil.validatePollSongBelongsToPoll(1, 1)).thenReturn(testPollSong);
        when(voteRepository.findByPollSongIdAndUserId(1, 1)).thenReturn(Collections.emptyList());

        // voteRepository.save()가 호출될 때 pollSong의 votes 리스트에도 추가되도록 Mock 설정
        when(voteRepository.save(any(Vote.class))).thenAnswer(invocation -> {
            Vote savedVote = invocation.getArgument(0);
            testPollSong.getVotes().add(savedVote);
            return savedVote;
        });

        // When
        PollSongRespDTO result = pollService.setVoteForSong(1, 1, voteType, 1);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Bohemian Rhapsody", result.getSongName());
        assertEquals("Queen", result.getArtistName());
        assertNotNull(result.getUserVoteType()); // 투표 타입이 설정되었는지 확인

        verify(userValidationUtil).getUserById(1);
        verify(entityValidationUtil).validatePollSongBelongsToPoll(1, 1);
        verify(voteRepository).findByPollSongIdAndUserId(1, 1);
        verify(voteRepository).save(any(Vote.class));

        // setUp에서 votes 리스트 초기화
        testPollSong.getVotes().clear();
    }

    @Test
    @DisplayName("23. 동일한 타입으로 재투표 시도")
    void setVoteForSong_ThrowsException_VoteAlreadyExists() {
        // Given
        when(userValidationUtil.getUserById(1)).thenReturn(testUser);
        when(entityValidationUtil.validatePollSongBelongsToPoll(1, 1)).thenReturn(testPollSong);
        when(voteRepository.findByPollSongIdAndUserId(1, 1)).thenReturn(List.of(testVote));

        // When & Then
        assertThrows(VoteAlreadyExistsException.class,
                () -> pollService.setVoteForSong(1, 1, "LIKE", 1));

        verify(userValidationUtil).getUserById(1);
        verify(entityValidationUtil).validatePollSongBelongsToPoll(1, 1);
        verify(voteRepository).findByPollSongIdAndUserId(1, 1);
        verify(voteRepository, never()).save(any());
    }

    @Test
    @DisplayName("24. 다른 타입으로 투표 변경 성공")
    void setVoteForSong_ChangeVoteType_Success() {
        // Given
        when(userValidationUtil.getUserById(1)).thenReturn(testUser);
        when(entityValidationUtil.validatePollSongBelongsToPoll(1, 1)).thenReturn(testPollSong);
        when(voteRepository.findByPollSongIdAndUserId(1, 1)).thenReturn(List.of(testVote));

        // When - LIKE에서 DISLIKE로 변경
        PollSongRespDTO result = pollService.setVoteForSong(1, 1, "DISLIKE", 1);

        // Then
        assertNotNull(result);
        assertEquals(VotedMark.DISLIKE, testVote.getVotedMark()); // 투표가 변경되었는지 확인

        verify(userValidationUtil).getUserById(1);
        verify(entityValidationUtil).validatePollSongBelongsToPoll(1, 1);
        verify(voteRepository).findByPollSongIdAndUserId(1, 1);
        verify(voteRepository, never()).save(any()); // Managed Entity이므로 save 호출되지 않음
    }

    @Test
    @DisplayName("25. 마감된 투표에 투표 시도")
    void setVoteForSong_WithExpiredPoll() {
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
        expiredPollSong.setVotes(new ArrayList<>()); // 변경 가능한 리스트로 설정

        when(userValidationUtil.getUserById(1)).thenReturn(testUser);
        when(entityValidationUtil.validatePollSongBelongsToPoll(1, 1)).thenReturn(expiredPollSong);
        when(voteRepository.findByPollSongIdAndUserId(1, 1)).thenReturn(Collections.emptyList());

        // voteRepository.save()가 호출될 때 pollSong의 votes 리스트에도 추가되도록 Mock 설정
        when(voteRepository.save(any(Vote.class))).thenAnswer(invocation -> {
            Vote savedVote = invocation.getArgument(0);
            expiredPollSong.getVotes().add(savedVote);
            return savedVote;
        });

        // When - 마감된 투표에도 투표는 가능 (비즈니스 로직에 따라)
        PollSongRespDTO result = pollService.setVoteForSong(1, 1, "LIKE", 1);

        // Then
        assertNotNull(result);
        assertEquals("Bohemian Rhapsody", result.getSongName());

        verify(userValidationUtil).getUserById(1);
        verify(entityValidationUtil).validatePollSongBelongsToPoll(1, 1);
        verify(voteRepository).save(any(Vote.class));
    }

    @Test
    @DisplayName("26. 존재하지 않는 투표 곡에 투표")
    void setVoteForSong_ThrowsException_PollSongNotFound() {
        // Given
        when(userValidationUtil.getUserById(1)).thenReturn(testUser);
        when(entityValidationUtil.validatePollSongBelongsToPoll(1, 999))
                .thenThrow(new RuntimeException("투표 곡을 찾을 수 없습니다."));

        // When & Then
        assertThrows(RuntimeException.class,
                () -> pollService.setVoteForSong(1, 999, "LIKE", 1));

        verify(userValidationUtil).getUserById(1);
        verify(entityValidationUtil).validatePollSongBelongsToPoll(1, 999);
        verify(voteRepository, never()).findByPollSongIdAndUserId(any(), any());
        verify(voteRepository, never()).save(any());
    }

    @Test
    @DisplayName("27. 존재하지 않는 투표에 투표 시도")
    void setVoteForSong_ThrowsException_PollNotFound() {
        // Given
        when(userValidationUtil.getUserById(1)).thenReturn(testUser);
        when(entityValidationUtil.validatePollSongBelongsToPoll(999, 1))
                .thenThrow(new RuntimeException("투표를 찾을 수 없습니다."));

        // When & Then
        assertThrows(RuntimeException.class,
                () -> pollService.setVoteForSong(999, 1, "LIKE", 1));

        verify(userValidationUtil).getUserById(1);
        verify(entityValidationUtil).validatePollSongBelongsToPoll(999, 1);
        verify(voteRepository, never()).findByPollSongIdAndUserId(any(), any());
        verify(voteRepository, never()).save(any());
    }

    @Test
    @DisplayName("28. 유효하지 않은 투표 타입으로 투표")
    void setVoteForSong_ThrowsException_InvalidVoteType() {
        // Given
        when(userValidationUtil.getUserById(1)).thenReturn(testUser);
        when(entityValidationUtil.validatePollSongBelongsToPoll(1, 1)).thenReturn(testPollSong);

        // When & Then
        assertThrows(BadRequestException.class,
                () -> pollService.setVoteForSong(1, 1, "INVALID_TYPE", 1));

        verify(userValidationUtil).getUserById(1);
        verify(entityValidationUtil).validatePollSongBelongsToPoll(1, 1);
        verify(voteRepository, never()).findByPollSongIdAndUserId(any(), any());
        verify(voteRepository, never()).save(any());
    }

    @Test
    @DisplayName("29. null 또는 빈 투표 타입으로 투표")
    void setVoteForSong_ThrowsException_NullVoteType() {
        // Given
        when(userValidationUtil.getUserById(1)).thenReturn(testUser);
        when(entityValidationUtil.validatePollSongBelongsToPoll(1, 1)).thenReturn(testPollSong);

        // When & Then
        assertThrows(BadRequestException.class,
                () -> pollService.setVoteForSong(1, 1, null, 1));

        verify(userValidationUtil).getUserById(1);
        verify(entityValidationUtil).validatePollSongBelongsToPoll(1, 1);
        verify(voteRepository, never()).findByPollSongIdAndUserId(any(), any());
        verify(voteRepository, never()).save(any());
    }

    @Test
    @DisplayName("30. 존재하지 않는 사용자로 투표 시도")
    void setVoteForSong_ThrowsException_UserNotFound() {
        // Given
        when(userValidationUtil.getUserById(999))
                .thenThrow(new RuntimeException("사용자를 찾을 수 없습니다."));

        // When & Then
        assertThrows(RuntimeException.class,
                () -> pollService.setVoteForSong(1, 1, "LIKE", 999));

        verify(userValidationUtil).getUserById(999);
        verify(entityValidationUtil, never()).validatePollSongBelongsToPoll(any(), any());
        verify(voteRepository, never()).findByPollSongIdAndUserId(any(), any());
        verify(voteRepository, never()).save(any());
    }

    @Test
    @DisplayName("31. 투표와 곡이 매칭되지 않을 때 투표 시도")
    void setVoteForSong_ThrowsException_PollSongNotBelongToPoll() {
        // Given
        when(userValidationUtil.getUserById(1)).thenReturn(testUser);
        when(entityValidationUtil.validatePollSongBelongsToPoll(1, 2))
                .thenThrow(new RuntimeException("해당 곡은 이 투표에 속하지 않습니다."));

        // When & Then
        assertThrows(RuntimeException.class,
                () -> pollService.setVoteForSong(1, 2, "LIKE", 1));

        verify(userValidationUtil).getUserById(1);
        verify(entityValidationUtil).validatePollSongBelongsToPoll(1, 2);
        verify(voteRepository, never()).findByPollSongIdAndUserId(any(), any());
        verify(voteRepository, never()).save(any());
    }

    @Test
    @DisplayName("32. Repository 저장 중 예외 발생")
    void setVoteForSong_ThrowsException_RepositorySaveFailure() {
        // Given
        when(userValidationUtil.getUserById(1)).thenReturn(testUser);
        when(entityValidationUtil.validatePollSongBelongsToPoll(1, 1)).thenReturn(testPollSong);
        when(voteRepository.findByPollSongIdAndUserId(1, 1)).thenReturn(Collections.emptyList());
        when(voteRepository.save(any(Vote.class)))
                .thenThrow(new RuntimeException("데이터베이스 저장 오류"));

        // When & Then
        assertThrows(RuntimeException.class,
                () -> pollService.setVoteForSong(1, 1, "LIKE", 1));

        verify(userValidationUtil).getUserById(1);
        verify(entityValidationUtil).validatePollSongBelongsToPoll(1, 1);
        verify(voteRepository).findByPollSongIdAndUserId(1, 1);
        verify(voteRepository).save(any(Vote.class));
    }

}
