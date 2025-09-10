package com.jandi.band_backend.poll.service;

import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.global.exception.ClubNotFoundException;
import com.jandi.band_backend.global.exception.PollNotFoundException;
import com.jandi.band_backend.global.util.EntityValidationUtil;
import com.jandi.band_backend.poll.dto.PollDetailRespDTO;
import com.jandi.band_backend.poll.dto.PollRespDTO;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PollService 조회 관련 테스트")
class PollQueryServiceTest {

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

    private Club testClub;
    private Users testUser;
    private Poll testPoll;
    private PollSong testPollSong;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        testClub = new Club();
        testClub.setId(1);
        testClub.setName("테스트 동아리");

        testUser = new Users();
        testUser.setId(1);
        testUser.setNickname("테스트사용자");

        UserPhoto userPhoto = new UserPhoto();
        userPhoto.setImageUrl("https://example.com/profile.jpg");
        userPhoto.setIsCurrent(true);
        userPhoto.setDeletedAt(null);
        testUser.setPhotos(List.of(userPhoto));

        testPoll = new Poll();
        testPoll.setId(1);
        testPoll.setTitle("테스트 투표");
        testPoll.setClub(testClub);
        testPoll.setCreator(testUser);
        testPoll.setStartDatetime(LocalDateTime.now().minusDays(1));
        testPoll.setEndDatetime(LocalDateTime.now().plusDays(30));
        testPoll.setCreatedAt(LocalDateTime.now());

        testPollSong = new PollSong();
        testPollSong.setId(1);
        testPollSong.setPoll(testPoll);
        testPollSong.setSongName("Bohemian Rhapsody");
        testPollSong.setArtistName("Queen");
        testPollSong.setSuggester(testUser);
        testPollSong.setVotes(Collections.emptyList());
        testPollSong.setCreatedAt(LocalDateTime.now());

        pageable = PageRequest.of(0, 5);
    }

    @Test
    @DisplayName("47. 정상 케이스 - 클럽별 투표 목록 조회 성공")
    void getPollsByClub_Success() {
        // Given
        when(entityValidationUtil.validateClubExists(1)).thenReturn(testClub);

        Page<Poll> pollPage = new PageImpl<>(List.of(testPoll), pageable, 1);
        when(pollRepository.findAllByClubAndDeletedAtIsNullOrderByCreatedAtDesc(testClub, pageable))
                .thenReturn(pollPage);

        // When
        Page<PollRespDTO> result = pollService.getPollsByClub(1, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());

        PollRespDTO pollDto = result.getContent().get(0);
        assertEquals(1, pollDto.getId());
        assertEquals("테스트 투표", pollDto.getTitle());
        assertEquals(1, pollDto.getClubId());
        assertEquals("테스트 동아리", pollDto.getClubName());
        assertEquals(1, pollDto.getCreatorId());
        assertEquals("테스트사용자", pollDto.getCreatorName());

        verify(entityValidationUtil).validateClubExists(1);
        verify(pollRepository).findAllByClubAndDeletedAtIsNullOrderByCreatedAtDesc(testClub, pageable);
    }

    @Test
    @DisplayName("48. 존재하지 않는 클럽의 투표 목록 조회")
    void getPollsByClub_ThrowsException_ClubNotFound() {
        // Given
        when(entityValidationUtil.validateClubExists(999))
                .thenThrow(new ClubNotFoundException("클럽을 찾을 수 없습니다."));

        // When & Then
        assertThrows(ClubNotFoundException.class,
                () -> pollService.getPollsByClub(999, pageable));

        verify(entityValidationUtil).validateClubExists(999);
        verify(pollRepository, never()).findAllByClubAndDeletedAtIsNullOrderByCreatedAtDesc(any(), any());
    }

    @Test
    @DisplayName("49. 빈 투표 목록 조회")
    void getPollsByClub_WithEmptyResults() {
        // Given
        when(entityValidationUtil.validateClubExists(1)).thenReturn(testClub);

        Page<Poll> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(pollRepository.findAllByClubAndDeletedAtIsNullOrderByCreatedAtDesc(testClub, pageable))
                .thenReturn(emptyPage);

        // When
        Page<PollRespDTO> result = pollService.getPollsByClub(1, pageable);

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getContent().size());
        assertTrue(result.getContent().isEmpty());

        verify(entityValidationUtil).validateClubExists(1);
        verify(pollRepository).findAllByClubAndDeletedAtIsNullOrderByCreatedAtDesc(testClub, pageable);
    }

    @Test
    @DisplayName("50. 정상 케이스 - 투표 상세 조회 성공")
    void getPollDetail_Success() {
        // Given
        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(pollSongRepository.findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(testPoll))
                .thenReturn(List.of(testPollSong));

        // When
        PollDetailRespDTO result = pollService.getPollDetail(1, 1);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("테스트 투표", result.getTitle());
        assertEquals(1, result.getClubId());
        assertEquals("테스트 동아리", result.getClubName());
        assertEquals(1, result.getCreatorId());
        assertEquals("테스트사용자", result.getCreatorName());

        assertNotNull(result.getSongs());
        assertEquals(1, result.getSongs().size());
        assertEquals("Bohemian Rhapsody", result.getSongs().get(0).getSongName());
        assertEquals("Queen", result.getSongs().get(0).getArtistName());

        verify(entityValidationUtil).validatePollExists(1);
        verify(pollSongRepository).findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(testPoll);
    }

    @Test
    @DisplayName("51. 존재하지 않는 투표 상세 조회")
    void getPollDetail_ThrowsException_PollNotFound() {
        // Given
        when(entityValidationUtil.validatePollExists(999))
                .thenThrow(new PollNotFoundException("투표를 찾을 수 없습니다."));

        // When & Then
        assertThrows(PollNotFoundException.class,
                () -> pollService.getPollDetail(999, 1));

        verify(entityValidationUtil).validatePollExists(999);
        verify(pollSongRepository, never()).findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("52. 삭제된 투표 상세 조회")
    void getPollDetail_WithDeletedPoll() {
        // Given
        Poll deletedPoll = new Poll();
        deletedPoll.setId(1);
        deletedPoll.setTitle("삭제된 투표");
        deletedPoll.setClub(testClub);
        deletedPoll.setCreator(testUser);
        deletedPoll.setDeletedAt(LocalDateTime.now().minusDays(1)); // 삭제된 투표

        when(entityValidationUtil.validatePollExists(1)).thenReturn(deletedPoll);
        when(pollSongRepository.findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(deletedPoll))
                .thenReturn(Collections.emptyList());

        // When
        PollDetailRespDTO result = pollService.getPollDetail(1, 1);

        // Then
        assertNotNull(result);
        assertEquals("삭제된 투표", result.getTitle());
        assertTrue(result.getSongs().isEmpty());

        verify(entityValidationUtil).validatePollExists(1);
        verify(pollSongRepository).findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(deletedPoll);
    }

    @Test
    @DisplayName("53. 투표 상세 조회 - 곡이 없는 경우")
    void getPollDetail_WithNoSongs() {
        // Given
        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(pollSongRepository.findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(testPoll))
                .thenReturn(Collections.emptyList());

        // When
        PollDetailRespDTO result = pollService.getPollDetail(1, 1);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("테스트 투표", result.getTitle());
        assertNotNull(result.getSongs());
        assertTrue(result.getSongs().isEmpty());

        verify(entityValidationUtil).validatePollExists(1);
        verify(pollSongRepository).findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(testPoll);
    }

    @Test
    @DisplayName("54. 투표 상세 조회 - 클럽 정보가 null인 경우")
    void getPollDetail_WithNullClubInfo() {
        // Given
        Poll pollWithNullClub = new Poll();
        pollWithNullClub.setId(1);
        pollWithNullClub.setTitle("클럽정보없는투표");
        pollWithNullClub.setClub(null); // null 클럽
        pollWithNullClub.setCreator(testUser);

        when(entityValidationUtil.validatePollExists(1)).thenReturn(pollWithNullClub);
        when(pollSongRepository.findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(pollWithNullClub))
                .thenReturn(Collections.emptyList());

        // When
        PollDetailRespDTO result = pollService.getPollDetail(1, 1);

        // Then
        assertNotNull(result);
        assertEquals("클럽정보없는투표", result.getTitle());
        assertNull(result.getClubId());
        assertNull(result.getClubName());
        assertEquals(1, result.getCreatorId());
        assertEquals("테스트사용자", result.getCreatorName());

        verify(entityValidationUtil).validatePollExists(1);
        verify(pollSongRepository).findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(pollWithNullClub);
    }

    @Test
    @DisplayName("55. 투표 상세 조회 - 생성자 정보가 null인 경우")
    void getPollDetail_WithNullCreatorInfo() {
        // Given
        Poll pollWithNullCreator = new Poll();
        pollWithNullCreator.setId(1);
        pollWithNullCreator.setTitle("생성자정보없는투표");
        pollWithNullCreator.setClub(testClub);
        pollWithNullCreator.setCreator(null); // null 생성자

        when(entityValidationUtil.validatePollExists(1)).thenReturn(pollWithNullCreator);
        when(pollSongRepository.findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(pollWithNullCreator))
                .thenReturn(Collections.emptyList());

        // When
        PollDetailRespDTO result = pollService.getPollDetail(1, 1);

        // Then
        assertNotNull(result);
        assertEquals("생성자정보없는투표", result.getTitle());
        assertEquals(1, result.getClubId());
        assertEquals("테스트 동아리", result.getClubName());
        assertNull(result.getCreatorId());
        assertNull(result.getCreatorName());

        verify(entityValidationUtil).validatePollExists(1);
        verify(pollSongRepository).findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(pollWithNullCreator);
    }

    @Test
    @DisplayName("56. Repository 조회 중 예외 발생")
    void getPollsByClub_ThrowsException_RepositoryFailure() {
        // Given
        when(entityValidationUtil.validateClubExists(1)).thenReturn(testClub);
        when(pollRepository.findAllByClubAndDeletedAtIsNullOrderByCreatedAtDesc(testClub, pageable))
                .thenThrow(new RuntimeException("데이터베이스 조회 오류"));

        // When & Then
        assertThrows(RuntimeException.class,
                () -> pollService.getPollsByClub(1, pageable));

        verify(entityValidationUtil).validateClubExists(1);
        verify(pollRepository).findAllByClubAndDeletedAtIsNullOrderByCreatedAtDesc(testClub, pageable);
    }

    @Test
    @DisplayName("57. 투표 상세 조회 - Repository 조회 중 예외 발생")
    void getPollDetail_ThrowsException_RepositoryFailure() {
        // Given
        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(pollSongRepository.findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(testPoll))
                .thenThrow(new RuntimeException("데이터베이스 조회 오류"));

        // When & Then
        assertThrows(RuntimeException.class,
                () -> pollService.getPollDetail(1, 1));

        verify(entityValidationUtil).validatePollExists(1);
        verify(pollSongRepository).findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(testPoll);
    }

    @Test
    @DisplayName("58. null currentUserId로 투표 상세 조회")
    void getPollDetail_WithNullCurrentUserId() {
        // Given
        when(entityValidationUtil.validatePollExists(1)).thenReturn(testPoll);
        when(pollSongRepository.findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(testPoll))
                .thenReturn(List.of(testPollSong));

        // When - null currentUserId 전달
        PollDetailRespDTO result = pollService.getPollDetail(1, null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("테스트 투표", result.getTitle());

        // 곡 정보 확인 (userVoteType이 null이어야 함)
        assertNotNull(result.getSongs());
        assertEquals(1, result.getSongs().size());
        assertNull(result.getSongs().get(0).getUserVoteType());

        verify(entityValidationUtil).validatePollExists(1);
        verify(pollSongRepository).findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(testPoll);
    }
}
