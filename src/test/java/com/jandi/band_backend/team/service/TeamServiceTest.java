package com.jandi.band_backend.team.service;

import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.repository.ClubRepository;
import com.jandi.band_backend.global.exception.ClubNotFoundException;
import com.jandi.band_backend.global.exception.TeamNotFoundException;
import com.jandi.band_backend.global.exception.InvalidAccessException;
import com.jandi.band_backend.global.util.EntityValidationUtil;
import com.jandi.band_backend.global.util.PermissionValidationUtil;
import com.jandi.band_backend.global.util.TimetableValidationUtil;
import com.jandi.band_backend.global.util.UserValidationUtil;
import com.jandi.band_backend.team.dto.TeamDetailRespDTO;
import com.jandi.band_backend.team.dto.TeamReqDTO;
import com.jandi.band_backend.team.dto.TeamRespDTO;
import com.jandi.band_backend.team.entity.Team;
import com.jandi.band_backend.team.entity.TeamMember;
import com.jandi.band_backend.team.repository.TeamEventRepository;
import com.jandi.band_backend.team.repository.TeamMemberRepository;
import com.jandi.band_backend.team.repository.TeamRepository;
import com.jandi.band_backend.team.util.TeamTimetableUtil;
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
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamService 팀 관리 테스트")
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;

    @Mock
    private TeamMemberRepository teamMemberRepository;

    @Mock
    private TeamEventRepository teamEventRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private TeamTimetableUtil teamTimetableUtil;

    @Mock
    private PermissionValidationUtil permissionValidationUtil;

    @Mock
    private UserValidationUtil userValidationUtil;

    @Mock
    private EntityValidationUtil entityValidationUtil;

    @Mock
    private TimetableValidationUtil timetableValidationUtil;

    @InjectMocks
    private TeamService teamService;

    @Test
    @DisplayName("팀 생성 성공")
    void createTeam_Success() {
        // Given
        Integer clubId = 1;
        Integer currentUserId = 1;
        TeamReqDTO teamReqDTO = new TeamReqDTO();
        teamReqDTO.setName("테스트 팀");

        Club mockClub = createMockClub();
        Users mockUser = createMockUser();
        Team mockTeam = createMockTeam(mockClub, mockUser);
        TeamMember mockTeamMember = createMockTeamMember(mockTeam, mockUser);

        when(entityValidationUtil.validateClubExists(clubId)).thenReturn(mockClub);
        when(userValidationUtil.getUserById(currentUserId)).thenReturn(mockUser);
        doNothing().when(permissionValidationUtil).validateClubMemberAccess(anyInt(), anyInt(), any());
        when(teamRepository.save(any(Team.class))).thenReturn(mockTeam);
        when(teamMemberRepository.save(any(TeamMember.class))).thenReturn(mockTeamMember);
        when(teamMemberRepository.findByTeamIdAndDeletedAtIsNull(anyInt())).thenReturn(Arrays.asList(mockTeamMember));

        // When
        TeamDetailRespDTO result = teamService.createTeam(clubId, teamReqDTO, currentUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("테스트 팀");
        assertThat(result.getClubId()).isEqualTo(clubId);
        assertThat(result.getCreatorId()).isEqualTo(currentUserId);
        assertThat(result.getMembers()).hasSize(1);

        verify(teamRepository).save(any(Team.class));
        verify(teamMemberRepository).save(any(TeamMember.class));
    }

    @Test
    @DisplayName("팀 생성 실패 - 존재하지 않는 동아리")
    void createTeam_ClubNotFound() {
        // Given
        Integer clubId = 999;
        Integer currentUserId = 1;
        TeamReqDTO teamReqDTO = new TeamReqDTO();
        teamReqDTO.setName("테스트 팀");

        when(entityValidationUtil.validateClubExists(clubId))
                .thenThrow(new ClubNotFoundException("동아리가 존재하지 않습니다"));

        // When & Then
        assertThatThrownBy(() -> teamService.createTeam(clubId, teamReqDTO, currentUserId))
                .isInstanceOf(ClubNotFoundException.class)
                .hasMessage("동아리가 존재하지 않습니다");

        verify(teamRepository, never()).save(any());
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("팀 생성 실패 - 권한 없음")
    void createTeam_PermissionDenied() {
        // Given
        Integer clubId = 1;
        Integer currentUserId = 1;
        TeamReqDTO teamReqDTO = new TeamReqDTO();
        teamReqDTO.setName("테스트 팀");

        Club mockClub = createMockClub();
        Users mockUser = createMockUser();

        when(entityValidationUtil.validateClubExists(clubId)).thenReturn(mockClub);
        when(userValidationUtil.getUserById(currentUserId)).thenReturn(mockUser);
        doThrow(new InvalidAccessException("동아리 부원만 팀을 생성할 수 있습니다"))
                .when(permissionValidationUtil).validateClubMemberAccess(anyInt(), anyInt(), any());

        // When & Then
        assertThatThrownBy(() -> teamService.createTeam(clubId, teamReqDTO, currentUserId))
                .isInstanceOf(InvalidAccessException.class)
                .hasMessage("동아리 부원만 팀을 생성할 수 있습니다");

        verify(teamRepository, never()).save(any());
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("동아리별 팀 목록 조회 성공")
    void getTeamsByClub_Success() {
        // Given
        Integer clubId = 1;
        Integer currentUserId = 1;
        Pageable pageable = PageRequest.of(0, 5);

        Club mockClub = createMockClub();
        Users mockUser = createMockUser();
        Team mockTeam = createMockTeam(mockClub, mockUser);
        Page<Team> teamPage = new PageImpl<>(Arrays.asList(mockTeam));

        when(entityValidationUtil.validateClubExists(clubId)).thenReturn(mockClub);
        when(teamRepository.findAllByClubAndDeletedAtIsNullOrderByCreatedAtDesc(mockClub, pageable))
                .thenReturn(teamPage);
        when(teamMemberRepository.countByTeamIdAndDeletedAtIsNull(anyInt())).thenReturn(5);

        // When
        Page<TeamRespDTO> result = teamService.getTeamsByClub(clubId, pageable, currentUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("테스트 팀");
        assertThat(result.getContent().get(0).getMemberCount()).isEqualTo(5);

        verify(entityValidationUtil).validateClubExists(clubId);
        verify(teamRepository).findAllByClubAndDeletedAtIsNullOrderByCreatedAtDesc(mockClub, pageable);
    }

    @Test
    @DisplayName("팀 상세 정보 조회 성공")
    void getTeamDetail_Success() {
        // Given
        Integer teamId = 1;
        Integer currentUserId = 1;

        Club mockClub = createMockClub();
        Users mockUser = createMockUser();
        Team mockTeam = createMockTeam(mockClub, mockUser);
        TeamMember mockTeamMember = createMockTeamMember(mockTeam, mockUser);

        when(entityValidationUtil.validateTeamExists(teamId)).thenReturn(mockTeam);
        doNothing().when(permissionValidationUtil).validateClubMemberAccess(anyInt(), anyInt(), any());
        when(teamMemberRepository.findByTeamIdAndDeletedAtIsNull(teamId))
                .thenReturn(Arrays.asList(mockTeamMember));

        // When
        TeamDetailRespDTO result = teamService.getTeamDetail(teamId, currentUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(teamId);
        assertThat(result.getName()).isEqualTo("테스트 팀");
        assertThat(result.getMembers()).hasSize(1);
        assertThat(result.getMembers().get(0).getName()).isEqualTo("테스트 사용자");

        verify(entityValidationUtil).validateTeamExists(teamId);
        verify(permissionValidationUtil).validateClubMemberAccess(anyInt(), anyInt(), any());
    }

    @Test
    @DisplayName("팀 상세 정보 조회 실패 - 존재하지 않는 팀")
    void getTeamDetail_TeamNotFound() {
        // Given
        Integer teamId = 999;
        Integer currentUserId = 1;

        when(entityValidationUtil.validateTeamExists(teamId))
                .thenThrow(new TeamNotFoundException("팀이 존재하지 않습니다"));

        // When & Then
        assertThatThrownBy(() -> teamService.getTeamDetail(teamId, currentUserId))
                .isInstanceOf(TeamNotFoundException.class)
                .hasMessage("팀이 존재하지 않습니다");

        verify(entityValidationUtil).validateTeamExists(teamId);
        verify(teamMemberRepository, never()).findByTeamIdAndDeletedAtIsNull(anyInt());
    }

    @Test
    @DisplayName("팀 정보 수정 성공")
    void updateTeam_Success() {
        // Given
        Integer teamId = 1;
        Integer currentUserId = 1;
        TeamReqDTO teamReqDTO = new TeamReqDTO();
        teamReqDTO.setName("수정된 팀명");

        Club mockClub = createMockClub();
        Users mockUser = createMockUser();
        Team mockTeam = createMockTeam(mockClub, mockUser);
        TeamMember mockTeamMember = createMockTeamMember(mockTeam, mockUser);

        when(entityValidationUtil.validateTeamExists(teamId)).thenReturn(mockTeam);
        when(permissionValidationUtil.validateTeamMemberAccess(anyInt(), anyInt(), any()))
                .thenReturn(mockTeamMember);
        when(teamRepository.save(any(Team.class))).thenReturn(mockTeam);
        when(teamMemberRepository.countByTeamIdAndDeletedAtIsNull(teamId)).thenReturn(3);

        // When
        TeamRespDTO result = teamService.updateTeam(teamId, teamReqDTO, currentUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("수정된 팀명");
        assertThat(result.getMemberCount()).isEqualTo(3);

        verify(entityValidationUtil).validateTeamExists(teamId);
        verify(permissionValidationUtil).validateTeamMemberAccess(anyInt(), anyInt(), any());
        verify(teamRepository).save(mockTeam);
    }

    @Test
    @DisplayName("팀 정보 수정 실패 - 권한 없음")
    void updateTeam_PermissionDenied() {
        // Given
        Integer teamId = 1;
        Integer currentUserId = 1;
        TeamReqDTO teamReqDTO = new TeamReqDTO();
        teamReqDTO.setName("수정된 팀명");

        Club mockClub = createMockClub();
        Users mockUser = createMockUser();
        Team mockTeam = createMockTeam(mockClub, mockUser);

        when(entityValidationUtil.validateTeamExists(teamId)).thenReturn(mockTeam);
        doThrow(new InvalidAccessException("팀 멤버만 팀 이름을 수정할 수 있습니다"))
                .when(permissionValidationUtil).validateTeamMemberAccess(anyInt(), anyInt(), any());

        // When & Then
        assertThatThrownBy(() -> teamService.updateTeam(teamId, teamReqDTO, currentUserId))
                .isInstanceOf(InvalidAccessException.class)
                .hasMessage("팀 멤버만 팀 이름을 수정할 수 있습니다");

        verify(teamRepository, never()).save(any());
    }

    // Helper methods for creating mock objects
    private Club createMockClub() {
        Club club = new Club();
        club.setId(1);
        club.setName("테스트 동아리");
        return club;
    }

    private Users createMockUser() {
        Users user = new Users();
        user.setId(1);
        user.setNickname("테스트 사용자");
        return user;
    }

    private Team createMockTeam(Club club, Users creator) {
        Team team = new Team();
        team.setId(1);
        team.setName("테스트 팀");
        team.setClub(club);
        team.setCreator(creator);
        team.setCreatedAt(LocalDateTime.now());
        team.setUpdatedAt(LocalDateTime.now());
        team.setSuggestedScheduleAt(LocalDateTime.now());
        return team;
    }

    private TeamMember createMockTeamMember(Team team, Users user) {
        TeamMember teamMember = new TeamMember();
        teamMember.setId(1);
        teamMember.setTeam(team);
        teamMember.setUser(user);
        teamMember.setJoinedAt(LocalDateTime.now());
        return teamMember;
    }
}