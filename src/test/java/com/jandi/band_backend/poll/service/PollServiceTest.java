package com.jandi.band_backend.poll.service;

import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.global.exception.BadRequestException;
import com.jandi.band_backend.global.exception.PollNotFoundException;
import com.jandi.band_backend.global.exception.UnauthorizedClubAccessException;
import com.jandi.band_backend.global.util.EntityValidationUtil;
import com.jandi.band_backend.global.util.PermissionValidationUtil;
import com.jandi.band_backend.global.util.UserValidationUtil;
import com.jandi.band_backend.poll.dto.*;
import com.jandi.band_backend.poll.entity.Poll;
import com.jandi.band_backend.poll.entity.PollSong;
import com.jandi.band_backend.poll.entity.Vote;
import com.jandi.band_backend.poll.repository.PollRepository;
import com.jandi.band_backend.poll.repository.PollSongRepository;
import com.jandi.band_backend.poll.repository.VoteRepository;
import com.jandi.band_backend.user.entity.Users;
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
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PollService 단위 테스트")
class PollServiceTest {

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

    @Mock
    private PermissionValidationUtil permissionValidationUtil;

    @InjectMocks
    private PollService pollService;

    @Test
    @DisplayName("투표 생성 성공")
    void createPoll_Success() {
        // Given
        Integer userId = 1;
        Integer clubId = 1;
        PollReqDTO requestDto = PollReqDTO.builder()
                .title("테스트 투표")
                .clubId(clubId)
                .endDatetime(LocalDateTime.now().plusDays(7))
                .build();

        Users user = new Users();
        user.setId(userId);
        user.setNickname("testUser");

        Club club = new Club();
        club.setId(clubId);
        club.setName("테스트 클럽");

        Poll savedPoll = new Poll();
        savedPoll.setId(1);
        savedPoll.setTitle(requestDto.getTitle());
        savedPoll.setClub(club);
        savedPoll.setCreator(user);
        savedPoll.setEndDatetime(requestDto.getEndDatetime());
        savedPoll.setCreatedAt(LocalDateTime.now());

        when(entityValidationUtil.validateClubExists(clubId)).thenReturn(club);
        when(userValidationUtil.getUserById(userId)).thenReturn(user);
        when(pollRepository.save(any(Poll.class))).thenReturn(savedPoll);

        // When
        PollRespDTO result = pollService.createPoll(requestDto, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getTitle()).isEqualTo("테스트 투표");
        assertThat(result.getClubId()).isEqualTo(clubId);
        assertThat(result.getCreatorName()).isEqualTo("testUser");

        verify(entityValidationUtil).validateClubExists(clubId);
        verify(userValidationUtil).getUserById(userId);
        verify(pollRepository).save(any(Poll.class));
    }

    @Test
    @DisplayName("투표 생성 실패 - 클럽 없음")
    void createPoll_ClubNotFound() {
        // Given
        Integer userId = 1;
        Integer clubId = 999;
        PollReqDTO requestDto = PollReqDTO.builder()
                .title("테스트 투표")
                .clubId(clubId)
                .endDatetime(LocalDateTime.now().plusDays(7))
                .build();

        when(entityValidationUtil.validateClubExists(clubId))
                .thenThrow(new PollNotFoundException("클럽을 찾을 수 없습니다"));

        // When & Then
        assertThatThrownBy(() -> pollService.createPoll(requestDto, userId))
                .isInstanceOf(PollNotFoundException.class)
                .hasMessageContaining("클럽을 찾을 수 없습니다");

        verify(entityValidationUtil).validateClubExists(clubId);
        verify(userValidationUtil, never()).getUserById(anyInt());
        verify(pollRepository, never()).save(any(Poll.class));
    }

    @Test
    @DisplayName("클럽별 투표 목록 조회 성공")
    void getPollsByClub_Success() {
        // Given
        Integer clubId = 1;
        Pageable pageable = PageRequest.of(0, 5);

        Club club = new Club();
        club.setId(clubId);
        club.setName("테스트 클럽");

        Users creator = new Users();
        creator.setId(1);
        creator.setNickname("creator");

        Poll poll1 = new Poll();
        poll1.setId(1);
        poll1.setTitle("투표 1");
        poll1.setClub(club);
        poll1.setCreator(creator);
        poll1.setEndDatetime(LocalDateTime.now().plusDays(1));
        poll1.setCreatedAt(LocalDateTime.now());

        Poll poll2 = new Poll();
        poll2.setId(2);
        poll2.setTitle("투표 2");
        poll2.setClub(club);
        poll2.setCreator(creator);
        poll2.setEndDatetime(LocalDateTime.now().plusDays(2));
        poll2.setCreatedAt(LocalDateTime.now());

        List<Poll> polls = List.of(poll1, poll2);
        Page<Poll> pollPage = new PageImpl<>(polls, pageable, polls.size());

        when(entityValidationUtil.validateClubExists(clubId)).thenReturn(club);
        when(pollRepository.findAllByClubAndDeletedAtIsNullOrderByCreatedAtDesc(club, pageable))
                .thenReturn(pollPage);

        // When
        Page<PollRespDTO> result = pollService.getPollsByClub(clubId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("투표 1");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("투표 2");

        verify(entityValidationUtil).validateClubExists(clubId);
        verify(pollRepository).findAllByClubAndDeletedAtIsNullOrderByCreatedAtDesc(club, pageable);
    }

    @Test
    @DisplayName("투표 상세 조회 성공")
    void getPollDetail_Success() {
        // Given
        Integer pollId = 1;
        Integer currentUserId = 1;

        Club club = new Club();
        club.setId(1);
        club.setName("테스트 클럽");

        Users creator = new Users();
        creator.setId(1);
        creator.setNickname("creator");

        Poll poll = new Poll();
        poll.setId(pollId);
        poll.setTitle("테스트 투표");
        poll.setClub(club);
        poll.setCreator(creator);
        poll.setEndDatetime(LocalDateTime.now().plusDays(1));
        poll.setCreatedAt(LocalDateTime.now());

        PollSong song1 = new PollSong();
        song1.setId(1);
        song1.setSongName("곡 1");
        song1.setArtistName("아티스트 1");
        song1.setPoll(poll);
        song1.setSuggester(creator);

        PollSong song2 = new PollSong();
        song2.setId(2);
        song2.setSongName("곡 2");
        song2.setArtistName("아티스트 2");
        song2.setPoll(poll);
        song2.setSuggester(creator);

        List<PollSong> songs = List.of(song1, song2);

        when(entityValidationUtil.validatePollExists(pollId)).thenReturn(poll);
        when(pollSongRepository.findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(poll)).thenReturn(songs);

        // When
        PollDetailRespDTO result = pollService.getPollDetail(pollId, currentUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(pollId);
        assertThat(result.getTitle()).isEqualTo("테스트 투표");
        assertThat(result.getSongs()).hasSize(2);

        verify(entityValidationUtil).validatePollExists(pollId);
        verify(pollSongRepository).findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(poll);
    }

    @Test
    @DisplayName("투표 상세 조회 실패 - 투표 없음")
    void getPollDetail_PollNotFound() {
        // Given
        Integer pollId = 999;
        Integer currentUserId = 1;

        when(entityValidationUtil.validatePollExists(pollId))
                .thenThrow(new PollNotFoundException("투표를 찾을 수 없습니다"));

        // When & Then
        assertThatThrownBy(() -> pollService.getPollDetail(pollId, currentUserId))
                .isInstanceOf(PollNotFoundException.class)
                .hasMessageContaining("투표를 찾을 수 없습니다");

        verify(entityValidationUtil).validatePollExists(pollId);
        verify(pollSongRepository, never()).findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("투표에 곡 추가 성공")
    void addSongToPoll_Success() {
        // Given
        Integer pollId = 1;
        Integer userId = 1;
        PollSongReqDTO requestDto = PollSongReqDTO.builder()
                .songName("새 곡")
                .artistName("새 아티스트")
                .youtubeUrl("https://youtube.com/watch?v=test")
                .description("곡 설명")
                .build();

        Club club = new Club();
        club.setId(1);
        club.setName("테스트 클럽");

        Users user = new Users();
        user.setId(userId);
        user.setNickname("testUser");

        Poll poll = new Poll();
        poll.setId(pollId);
        poll.setTitle("테스트 투표");
        poll.setClub(club);
        poll.setEndDatetime(LocalDateTime.now().plusDays(1));

        PollSong savedSong = new PollSong();
        savedSong.setId(1);
        savedSong.setSongName(requestDto.getSongName());
        savedSong.setArtistName(requestDto.getArtistName());
        savedSong.setYoutubeUrl(requestDto.getYoutubeUrl());
        savedSong.setDescription(requestDto.getDescription());
        savedSong.setPoll(poll);
        savedSong.setSuggester(user);

        when(entityValidationUtil.validatePollExists(pollId)).thenReturn(poll);
        when(userValidationUtil.getUserById(userId)).thenReturn(user);
        when(pollSongRepository.save(any(PollSong.class))).thenReturn(savedSong);

        // When
        PollSongRespDTO result = pollService.addSongToPoll(pollId, requestDto, userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getSongName()).isEqualTo("새 곡");
        assertThat(result.getArtistName()).isEqualTo("새 아티스트");
        assertThat(result.getYoutubeUrl()).isEqualTo("https://youtube.com/watch?v=test");
        assertThat(result.getSuggesterName()).isEqualTo("testUser");

        verify(entityValidationUtil).validatePollExists(pollId);
        verify(userValidationUtil).getUserById(userId);
        verify(pollSongRepository).save(any(PollSong.class));
    }

    @Test
    @DisplayName("투표 삭제 성공 - 생성자 권한")
    void deletePoll_SuccessByCreator() {
        // Given
        Integer pollId = 1;
        Integer creatorId = 10;

        Club club = new Club();
        club.setId(100);

        Users creator = new Users();
        creator.setId(creatorId);

        Poll poll = new Poll();
        poll.setId(pollId);
        poll.setClub(club);
        poll.setCreator(creator);

        PollSong song1 = new PollSong();
        song1.setId(101);
        song1.setPoll(poll);
        PollSong song2 = new PollSong();
        song2.setId(102);
        song2.setPoll(poll);

        Vote vote1 = new Vote();
        vote1.setPollSong(song1);
        Vote vote2 = new Vote();
        vote2.setPollSong(song1);
        Vote vote3 = new Vote();
        vote3.setPollSong(song2);

        when(entityValidationUtil.validatePollExists(pollId)).thenReturn(poll);
        when(pollSongRepository.findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(poll))
                .thenReturn(List.of(song1, song2));
        when(voteRepository.findAllByPollSongIdAndDeletedAtIsNull(song1.getId()))
                .thenReturn(List.of(vote1, vote2));
        when(voteRepository.findAllByPollSongIdAndDeletedAtIsNull(song2.getId()))
                .thenReturn(List.of(vote3));

        // When
        pollService.deletePoll(pollId, creatorId);

        // Then
        assertThat(poll.getDeletedAt()).isNotNull();
        assertThat(song1.getDeletedAt()).isNotNull();
        assertThat(song2.getDeletedAt()).isNotNull();
        assertThat(vote1.getDeletedAt()).isNotNull();
        assertThat(vote2.getDeletedAt()).isNotNull();
        assertThat(vote3.getDeletedAt()).isNotNull();
        verify(voteRepository).findAllByPollSongIdAndDeletedAtIsNull(song1.getId());
        verify(voteRepository).findAllByPollSongIdAndDeletedAtIsNull(song2.getId());
        verify(permissionValidationUtil, never())
                .validateClubRepresentativeAccess(anyInt(), anyInt(), anyString());
    }

    @Test
    @DisplayName("투표 삭제 성공 - 동아리 대표자 권한")
    void deletePoll_SuccessByRepresentative() {
        // Given
        Integer pollId = 1;
        Integer creatorId = 10;
        Integer representativeId = 20;

        Club club = new Club();
        club.setId(100);

        Users creator = new Users();
        creator.setId(creatorId);

        Poll poll = new Poll();
        poll.setId(pollId);
        poll.setClub(club);
        poll.setCreator(creator);

        PollSong song = new PollSong();
        song.setId(201);
        song.setPoll(poll);

        when(entityValidationUtil.validatePollExists(pollId)).thenReturn(poll);
        when(pollSongRepository.findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(poll))
                .thenReturn(List.of(song));
        when(voteRepository.findAllByPollSongIdAndDeletedAtIsNull(song.getId()))
                .thenReturn(List.of());

        // When
        pollService.deletePoll(pollId, representativeId);

        // Then
        assertThat(poll.getDeletedAt()).isNotNull();
        assertThat(song.getDeletedAt()).isNotNull();
        verify(voteRepository).findAllByPollSongIdAndDeletedAtIsNull(song.getId());
        verify(permissionValidationUtil).validateClubRepresentativeAccess(
                club.getId(),
                representativeId,
                "투표를 삭제할 권한이 없습니다."
        );
    }

    @Test
    @DisplayName("투표 삭제 실패 - 권한 없음")
    void deletePoll_FailUnauthorized() {
        // Given
        Integer pollId = 1;
        Integer creatorId = 10;
        Integer unauthorizedUserId = 30;

        Club club = new Club();
        club.setId(100);

        Users creator = new Users();
        creator.setId(creatorId);

        Poll poll = new Poll();
        poll.setId(pollId);
        poll.setClub(club);
        poll.setCreator(creator);

        when(entityValidationUtil.validatePollExists(pollId)).thenReturn(poll);
        doThrow(new UnauthorizedClubAccessException("투표를 삭제할 권한이 없습니다."))
                .when(permissionValidationUtil)
                .validateClubRepresentativeAccess(club.getId(), unauthorizedUserId, "투표를 삭제할 권한이 없습니다.");

        // When & Then
        assertThatThrownBy(() -> pollService.deletePoll(pollId, unauthorizedUserId))
                .isInstanceOf(UnauthorizedClubAccessException.class)
                .hasMessageContaining("투표를 삭제할 권한이 없습니다.");

        assertThat(poll.getDeletedAt()).isNull();
        verify(pollSongRepository, never()).findAllByPollAndDeletedAtIsNullOrderByCreatedAtDesc(any());
    }
}
