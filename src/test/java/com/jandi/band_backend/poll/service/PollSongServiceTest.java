package com.jandi.band_backend.poll.service;

import com.jandi.band_backend.global.exception.PollNotFoundException;
import com.jandi.band_backend.global.util.EntityValidationUtil;
import com.jandi.band_backend.global.util.UserValidationUtil;
import com.jandi.band_backend.poll.dto.PollSongReqDTO;
import com.jandi.band_backend.poll.dto.PollSongRespDTO;
import com.jandi.band_backend.poll.entity.Poll;
import com.jandi.band_backend.poll.entity.PollSong;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PollService 곡 추가 관리 테스트")
class PollSongServiceTest {

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
    private PollSongReqDTO pollSongReqDTO;
    private PollSong testPollSong;

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

        pollSongReqDTO = PollSongReqDTO.builder()
                .songName("Bohemian Rhapsody")
                .artistName("Queen")
                .youtubeUrl("https://www.youtube.com/watch?v=fJ9rUzIMcZQ")
                .description("클래식한 록 명곡입니다")
                .build();

        testPollSong = new PollSong();
        testPollSong.setId(1);
        testPollSong.setPoll(testPoll);
        testPollSong.setSongName(pollSongReqDTO.getSongName());
        testPollSong.setArtistName(pollSongReqDTO.getArtistName());
        testPollSong.setYoutubeUrl(pollSongReqDTO.getYoutubeUrl());
        testPollSong.setDescription(pollSongReqDTO.getDescription());
        testPollSong.setSuggester(testUser);
        testPollSong.setVotes(Collections.emptyList());
        testPollSong.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("11. 정상 케이스 - 투표에 곡 추가 성공 (기본 정보)")
    void addSongToPoll_Success_BasicInfo() {
        // Given
        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(userValidationUtil.getUserById(1)).thenReturn(testUser);
        when(pollSongRepository.save(any(PollSong.class))).thenReturn(testPollSong);

        // When
        PollSongRespDTO result = pollService.addSongToPoll(1, pollSongReqDTO, 1);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("Bohemian Rhapsody", result.getSongName());
        assertEquals("Queen", result.getArtistName());
        assertEquals("https://www.youtube.com/watch?v=fJ9rUzIMcZQ", result.getYoutubeUrl());
        assertEquals("클래식한 록 명곡입니다", result.getDescription());

        verify(entityValidationUtil).validatePollExists(1);
        verify(userValidationUtil).getUserById(1);
        verify(pollSongRepository).save(any(PollSong.class));
    }

    @Test
    @DisplayName("11-2. 정상 케이스 - 곡 추가 시 제안자 정보 연결")
    void addSongToPoll_Success_SuggesterInfo() {
        // Given
        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(userValidationUtil.getUserById(1)).thenReturn(testUser);
        when(pollSongRepository.save(any(PollSong.class))).thenReturn(testPollSong);

        // When
        PollSongRespDTO result = pollService.addSongToPoll(1, pollSongReqDTO, 1);

        // Then
        assertEquals(1, result.getSuggesterId());
        assertEquals("테스트사용자", result.getSuggesterName());
        assertEquals("https://example.com/profile.jpg", result.getSuggesterProfilePhoto());
    }

    @Test
    @DisplayName("11-3. 정상 케이스 - 곡 추가 시 투표 카운트 초기화")
    void addSongToPoll_Success_InitializeVoteCounts() {
        // Given
        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(userValidationUtil.getUserById(1)).thenReturn(testUser);
        when(pollSongRepository.save(any(PollSong.class))).thenReturn(testPollSong);

        // When
        PollSongRespDTO result = pollService.addSongToPoll(1, pollSongReqDTO, 1);

        // Then
        assertEquals(0, result.getLikeCount());
        assertEquals(0, result.getDislikeCount());
        assertEquals(0, result.getCantCount());
        assertEquals(0, result.getHajjCount());
    }

    @Test
    @DisplayName("12. 존재하지 않는 투표에 곡 추가 시도")
    void addSongToPoll_ThrowsException_PollNotFound() {
        // Given
        when(entityValidationUtil.validatePollExists(999))
                .thenThrow(new PollNotFoundException("투표를 찾을 수 없습니다."));

        // When & Then
        assertThrows(PollNotFoundException.class,
                () -> pollService.addSongToPoll(999, pollSongReqDTO, 1));

        verify(entityValidationUtil).validatePollExists(999);
        verify(userValidationUtil, never()).getUserById(any());
        verify(pollSongRepository, never()).save(any());
    }

    @Test
    @DisplayName("13. 이미 마감된 투표에 곡 추가 시도")
    void addSongToPoll_WithExpiredPoll() {
        // Given
        Poll expiredPoll = new Poll();
        expiredPoll.setId(1);
        expiredPoll.setTitle("마감된 투표");
        expiredPoll.setEndDatetime(LocalDateTime.now().minusDays(1)); // 이미 마감

        when(entityValidationUtil.validatePollExists(1)).thenReturn(expiredPoll);
        when(userValidationUtil.getUserById(1)).thenReturn(testUser);

        PollSong expiredPollSong = new PollSong();
        expiredPollSong.setId(1);
        expiredPollSong.setPoll(expiredPoll);
        expiredPollSong.setSongName(pollSongReqDTO.getSongName());
        expiredPollSong.setArtistName(pollSongReqDTO.getArtistName());
        expiredPollSong.setSuggester(testUser);
        expiredPollSong.setVotes(Collections.emptyList());

        when(pollSongRepository.save(any(PollSong.class))).thenReturn(expiredPollSong);

        // When - 마감된 투표에도 곡 추가는 가능 (비즈니스 로직에 따라)
        PollSongRespDTO result = pollService.addSongToPoll(1, pollSongReqDTO, 1);

        // Then
        assertNotNull(result);
        assertEquals("Bohemian Rhapsody", result.getSongName());

        verify(entityValidationUtil).validatePollExists(1);
        verify(userValidationUtil).getUserById(1);
        verify(pollSongRepository).save(any(PollSong.class));
    }

    @Test
    @DisplayName("14. null 또는 빈 곡명으로 곡 추가")
    void addSongToPoll_WithNullOrEmptySongName() {
        // Given
        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(userValidationUtil.getUserById(1)).thenReturn(testUser);

        PollSongReqDTO nullSongNameRequest = PollSongReqDTO.builder()
                .songName(null)
                .artistName("Queen")
                .youtubeUrl("https://www.youtube.com/watch?v=fJ9rUzIMcZQ")
                .description("곡명이 null인 경우")
                .build();

        PollSong nullSongNamePollSong = new PollSong();
        nullSongNamePollSong.setId(1);
        nullSongNamePollSong.setPoll(testPoll);
        nullSongNamePollSong.setSongName(null);
        nullSongNamePollSong.setArtistName("Queen");
        nullSongNamePollSong.setSuggester(testUser);
        nullSongNamePollSong.setVotes(Collections.emptyList());

        when(pollSongRepository.save(any(PollSong.class))).thenReturn(nullSongNamePollSong);

        // When
        PollSongRespDTO result = pollService.addSongToPoll(1, nullSongNameRequest, 1);

        // Then
        assertNotNull(result);
        assertNull(result.getSongName());
        assertEquals("Queen", result.getArtistName());

        verify(pollSongRepository).save(any(PollSong.class));
    }

    @Test
    @DisplayName("15. null 또는 빈 아티스트명으로 곡 추가")
    void addSongToPoll_WithNullOrEmptyArtistName() {
        // Given
        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(userValidationUtil.getUserById(1)).thenReturn(testUser);

        PollSongReqDTO nullArtistNameRequest = PollSongReqDTO.builder()
                .songName("Bohemian Rhapsody")
                .artistName(null)
                .youtubeUrl("https://www.youtube.com/watch?v=fJ9rUzIMcZQ")
                .description("아티스트명이 null인 경우")
                .build();

        PollSong nullArtistNamePollSong = new PollSong();
        nullArtistNamePollSong.setId(1);
        nullArtistNamePollSong.setPoll(testPoll);
        nullArtistNamePollSong.setSongName("Bohemian Rhapsody");
        nullArtistNamePollSong.setArtistName(null);
        nullArtistNamePollSong.setSuggester(testUser);
        nullArtistNamePollSong.setVotes(Collections.emptyList());

        when(pollSongRepository.save(any(PollSong.class))).thenReturn(nullArtistNamePollSong);

        // When
        PollSongRespDTO result = pollService.addSongToPoll(1, nullArtistNameRequest, 1);

        // Then
        assertNotNull(result);
        assertEquals("Bohemian Rhapsody", result.getSongName());
        assertNull(result.getArtistName());

        verify(pollSongRepository).save(any(PollSong.class));
    }

    @Test
    @DisplayName("16. 존재하지 않는 사용자로 곡 추가 시도")
    void addSongToPoll_ThrowsException_UserNotFound() {
        // Given
        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(userValidationUtil.getUserById(999))
                .thenThrow(new RuntimeException("사용자를 찾을 수 없습니다."));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> pollService.addSongToPoll(1, pollSongReqDTO, 999));

        assertEquals("사용자를 찾을 수 없습니다.", exception.getMessage());

        verify(entityValidationUtil).validatePollExists(1);
        verify(userValidationUtil).getUserById(999);
        verify(pollSongRepository, never()).save(any());
    }

    @Test
    @DisplayName("17. 삭제된 투표에 곡 추가 시도")
    void addSongToPoll_WithDeletedPoll() {
        // Given
        Poll deletedPoll = new Poll();
        deletedPoll.setId(1);
        deletedPoll.setTitle("삭제된 투표");
        deletedPoll.setDeletedAt(LocalDateTime.now().minusDays(1)); // 삭제된 투표

        when(entityValidationUtil.validatePollExists(1)).thenReturn(deletedPoll);
        when(userValidationUtil.getUserById(1)).thenReturn(testUser);

        PollSong deletedPollSong = new PollSong();
        deletedPollSong.setId(1);
        deletedPollSong.setPoll(deletedPoll);
        deletedPollSong.setSongName(pollSongReqDTO.getSongName());
        deletedPollSong.setArtistName(pollSongReqDTO.getArtistName());
        deletedPollSong.setSuggester(testUser);
        deletedPollSong.setVotes(Collections.emptyList());

        when(pollSongRepository.save(any(PollSong.class))).thenReturn(deletedPollSong);

        // When - 삭제된 투표에도 곡 추가는 가능 (비즈니스 로직에 따라)
        PollSongRespDTO result = pollService.addSongToPoll(1, pollSongReqDTO, 1);

        // Then
        assertNotNull(result);
        assertEquals("Bohemian Rhapsody", result.getSongName());

        verify(entityValidationUtil).validatePollExists(1);
        verify(userValidationUtil).getUserById(1);
        verify(pollSongRepository).save(any(PollSong.class));
    }

    @Test
    @DisplayName("18. 프로필 사진이 없는 사용자의 곡 추가")
    void addSongToPoll_WithUserNoProfilePhoto() {
        // Given
        Users userWithoutPhoto = new Users();
        userWithoutPhoto.setId(1);
        userWithoutPhoto.setNickname("사진없는사용자");
        userWithoutPhoto.setPhotos(Collections.emptyList()); // 프로필 사진 없음

        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(userValidationUtil.getUserById(1)).thenReturn(userWithoutPhoto);

        PollSong pollSongWithoutPhoto = new PollSong();
        pollSongWithoutPhoto.setId(1);
        pollSongWithoutPhoto.setPoll(testPoll);
        pollSongWithoutPhoto.setSongName(pollSongReqDTO.getSongName());
        pollSongWithoutPhoto.setArtistName(pollSongReqDTO.getArtistName());
        pollSongWithoutPhoto.setSuggester(userWithoutPhoto);
        pollSongWithoutPhoto.setVotes(Collections.emptyList());

        when(pollSongRepository.save(any(PollSong.class))).thenReturn(pollSongWithoutPhoto);

        // When
        PollSongRespDTO result = pollService.addSongToPoll(1, pollSongReqDTO, 1);

        // Then
        assertNotNull(result);
        assertEquals("사진없는사용자", result.getSuggesterName());
        assertNull(result.getSuggesterProfilePhoto()); // 프로필 사진이 null이어야 함

        verify(pollSongRepository).save(any(PollSong.class));
    }

    @Test
    @DisplayName("19. Repository 저장 중 예외 발생")
    void addSongToPoll_ThrowsException_RepositorySaveFailure() {
        // Given
        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(userValidationUtil.getUserById(1)).thenReturn(testUser);
        when(pollSongRepository.save(any(PollSong.class)))
                .thenThrow(new RuntimeException("데이터베이스 저장 오류"));

        // When & Then
        assertThrows(RuntimeException.class,
                () -> pollService.addSongToPoll(1, pollSongReqDTO, 1));

        verify(entityValidationUtil).validatePollExists(1);
        verify(userValidationUtil).getUserById(1);
        verify(pollSongRepository).save(any(PollSong.class));
    }

    @Test
    @DisplayName("20. 투표 정보가 null인 상태에서 DTO 변환")
    void addSongToPoll_WithNullPollInfo() {
        // Given
        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(userValidationUtil.getUserById(1)).thenReturn(testUser);

        PollSong pollSongWithNullPoll = new PollSong();
        pollSongWithNullPoll.setId(1);
        pollSongWithNullPoll.setPoll(null); // null 투표
        pollSongWithNullPoll.setSongName(pollSongReqDTO.getSongName());
        pollSongWithNullPoll.setArtistName(pollSongReqDTO.getArtistName());
        pollSongWithNullPoll.setSuggester(testUser);
        pollSongWithNullPoll.setVotes(Collections.emptyList());

        when(pollSongRepository.save(any(PollSong.class))).thenReturn(pollSongWithNullPoll);

        // When
        PollSongRespDTO result = pollService.addSongToPoll(1, pollSongReqDTO, 1);

        // Then
        assertNotNull(result);
        assertNull(result.getPollId()); // null이어야 함
        assertEquals("Bohemian Rhapsody", result.getSongName());
        assertEquals("Queen", result.getArtistName());

        verify(pollSongRepository).save(any(PollSong.class));
    }

    @Test
    @DisplayName("21. 유효하지 않은 YouTube URL로 곡 추가")
    void addSongToPoll_WithInvalidYouTubeUrl() {
        // Given
        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(userValidationUtil.getUserById(1)).thenReturn(testUser);

        PollSongReqDTO invalidUrlRequest = PollSongReqDTO.builder()
                .songName("Bohemian Rhapsody")
                .artistName("Queen")
                .youtubeUrl("invalid-url") // 유효하지 않은 URL
                .description("유효하지 않은 URL 테스트")
                .build();

        PollSong pollSongWithInvalidUrl = new PollSong();
        pollSongWithInvalidUrl.setId(1);
        pollSongWithInvalidUrl.setPoll(testPoll);
        pollSongWithInvalidUrl.setSongName("Bohemian Rhapsody");
        pollSongWithInvalidUrl.setArtistName("Queen");
        pollSongWithInvalidUrl.setYoutubeUrl("invalid-url");
        pollSongWithInvalidUrl.setSuggester(testUser);
        pollSongWithInvalidUrl.setVotes(Collections.emptyList());

        when(pollSongRepository.save(any(PollSong.class))).thenReturn(pollSongWithInvalidUrl);

        // When
        PollSongRespDTO result = pollService.addSongToPoll(1, invalidUrlRequest, 1);

        // Then
        assertNotNull(result);
        assertEquals("invalid-url", result.getYoutubeUrl());

        verify(pollSongRepository).save(any(PollSong.class));
    }
}
