package com.jandi.band_backend.team.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.repository.ClubRepository;
import com.jandi.band_backend.global.exception.ClubNotFoundException;
import com.jandi.band_backend.global.exception.ResourceNotFoundException;
import com.jandi.band_backend.global.exception.TeamLeaveNotAllowedException;
import com.jandi.band_backend.team.dto.TeamDetailRespDTO;
import com.jandi.band_backend.team.dto.TeamReqDTO;
import com.jandi.band_backend.team.dto.TeamRespDTO;
import com.jandi.band_backend.team.entity.Team;
import com.jandi.band_backend.team.entity.TeamMember;
import com.jandi.band_backend.team.entity.TeamEvent;
import com.jandi.band_backend.team.repository.TeamMemberRepository;
import com.jandi.band_backend.team.repository.TeamRepository;
import com.jandi.band_backend.team.repository.TeamEventRepository;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.team.util.TeamTimetableUtil;
import com.jandi.band_backend.global.util.PermissionValidationUtil;
import com.jandi.band_backend.global.util.UserValidationUtil;
import com.jandi.band_backend.global.util.EntityValidationUtil;
import com.jandi.band_backend.global.util.TimetableValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamEventRepository teamEventRepository;
    private final ClubRepository clubRepository;
    private final TeamTimetableUtil teamTimetableUtil;
    private final PermissionValidationUtil permissionValidationUtil;
    private final UserValidationUtil userValidationUtil;
    private final EntityValidationUtil entityValidationUtil;
    private final TimetableValidationUtil timetableValidationUtil;

    @Transactional
    public TeamDetailRespDTO createTeam(Integer clubId, TeamReqDTO teamReqDTO, Integer currentUserId) {
        Club club = entityValidationUtil.validateClubExists(clubId);
        Users currentUser = userValidationUtil.getUserById(currentUserId);

        permissionValidationUtil.validateClubMemberAccess(
                clubId,
                currentUserId,
                "동아리 부원만 팀을 생성할 수 있습니다."
        );

        Team team = createNewTeam(club, currentUser, teamReqDTO);
        Team savedTeam = teamRepository.save(team);

        TeamMember teamMember = createTeamMember(savedTeam, currentUser);
        teamMemberRepository.save(teamMember);

        List<TeamMember> teamMembers = teamMemberRepository.findByTeamIdAndDeletedAtIsNull(savedTeam.getId());

        return createTeamDetailRespDTO(savedTeam, teamMembers);
    }

    public Page<TeamRespDTO> getTeamsByClub(Integer clubId, Pageable pageable, Integer currentUserId) {
        Club club = entityValidationUtil.validateClubExists(clubId);

        permissionValidationUtil.validateClubMemberAccess(
                clubId,
                currentUserId,
                "동아리 부원만 팀 목록을 조회할 수 있습니다."
        );

        Page<Team> teams = teamRepository.findAllByClubAndDeletedAtIsNullOrderByCreatedAtDesc(club, pageable);

        return teams.map(team -> {
            Integer memberCount = teamMemberRepository.countByTeamIdAndDeletedAtIsNull(team.getId());
            return createTeamRespDTO(team, memberCount);
        });
    }

    public TeamDetailRespDTO getTeamDetail(Integer teamId, Integer currentUserId) {
        Team team = entityValidationUtil.validateTeamExists(teamId);

        permissionValidationUtil.validateClubMemberAccess(
                team.getClub().getId(),
                currentUserId,
                "해당 동아리 부원만 팀 정보를 조회할 수 있습니다."
        );

        List<TeamMember> teamMembers = teamMemberRepository.findByTeamIdAndDeletedAtIsNull(teamId);

        return createTeamDetailRespDTOWithTimetable(team, teamMembers);
    }

    @Transactional
    public TeamRespDTO updateTeam(Integer teamId, TeamReqDTO teamReqDTO, Integer currentUserId) {
        Team team = entityValidationUtil.validateTeamExists(teamId);

        permissionValidationUtil.validateTeamMemberAccess(
                teamId,
                currentUserId,
                "팀 멤버만 팀 이름을 수정할 수 있습니다."
        );

        team.setName(teamReqDTO.getName());

        Team updatedTeam = teamRepository.save(team);

        Integer memberCount = teamMemberRepository.countByTeamIdAndDeletedAtIsNull(teamId);

        return createTeamRespDTO(updatedTeam, memberCount);
    }

    @Transactional
    public void deleteTeam(Integer teamId, Integer currentUserId) {
        Team team = entityValidationUtil.validateTeamExists(teamId);

        permissionValidationUtil.validateTeamMemberAccess(
                teamId,
                currentUserId,
                "팀 멤버만 팀을 삭제할 수 있습니다."
        );

        performTeamSoftDelete(teamId);
    }

    @Transactional
    public void leaveTeam(Integer teamId, Integer currentUserId) {
        Team team = entityValidationUtil.validateTeamExists(teamId);

        TeamMember teamMember = permissionValidationUtil.validateTeamMemberAccess(
                teamId,
                currentUserId,
                "해당 팀의 멤버가 아닙니다."
        );

        if (teamMemberRepository.countByTeamIdAndDeletedAtIsNull(teamId) == 1) {
            throw new TeamLeaveNotAllowedException("마지막 남은 팀원은 탈퇴할 수 없습니다. 팀을 삭제해주세요.");
        }

        teamMember.setDeletedAt(LocalDateTime.now());
        teamMemberRepository.save(teamMember);
    }

    private Team createNewTeam(Club club, Users creator, TeamReqDTO teamReqDTO) {
        Team team = new Team();
        team.setClub(club);
        team.setName(teamReqDTO.getName());
        team.setCreator(creator);
        team.setSuggestedScheduleAt(LocalDateTime.now());
        return team;
    }

    private TeamMember createTeamMember(Team team, Users user) {
        TeamMember teamMember = new TeamMember();
        teamMember.setTeam(team);
        teamMember.setUser(user);
        return teamMember;
    }

    private void performTeamSoftDelete(Integer teamId) {
        performTeamSoftDelete(teamId, LocalDateTime.now());
    }

    @Transactional
    public void performTeamSoftDelete(Integer teamId, LocalDateTime deletedTime) {
        List<TeamMember> teamMembers = teamMemberRepository.findByTeamIdAndDeletedAtIsNull(teamId);
        teamMembers.forEach(teamMember -> teamMember.setDeletedAt(deletedTime));
        teamMemberRepository.saveAll(teamMembers);

        List<TeamEvent> teamEvents = teamEventRepository.findAllByTeamIdAndDeletedAtIsNull(teamId);
        teamEvents.forEach(teamEvent -> teamEvent.setDeletedAt(deletedTime));
        teamEventRepository.saveAll(teamEvents);

        Team team = entityValidationUtil.validateTeamExists(teamId);
        team.setDeletedAt(deletedTime);
        teamRepository.save(team);
    }

    private TeamRespDTO createTeamRespDTO(Team team, Integer memberCount) {
        return TeamRespDTO.builder()
                .id(team.getId())
                .name(team.getName())
                .clubId(team.getClub().getId())
                .clubName(team.getClub().getName())
                .creatorId(team.getCreator().getId())
                .creatorName(team.getCreator().getNickname())
                .memberCount(memberCount)
                .createdAt(team.getCreatedAt())
                .build();
    }

    private TeamDetailRespDTO createTeamDetailRespDTO(Team team, List<TeamMember> teamMembers) {
        return TeamDetailRespDTO.builder()
                .id(team.getId())
                .name(team.getName())
                .clubId(team.getClub().getId())
                .clubName(team.getClub().getName())
                .creatorId(team.getCreator().getId())
                .creatorName(team.getCreator().getNickname())
                .members(teamMembers.stream()
                        .map(this::createMemberInfoDTO)
                        .collect(Collectors.toList()))
                .suggestedScheduleAt(team.getSuggestedScheduleAt())
                .createdAt(team.getCreatedAt())
                .updatedAt(team.getUpdatedAt())
                .build();
    }

    private TeamDetailRespDTO createTeamDetailRespDTOWithTimetable(Team team, List<TeamMember> teamMembers) {
        int submittedCount = calculateSubmittedCount(teamMembers, team.getSuggestedScheduleAt());

        TeamDetailRespDTO.SubmissionProgressDTO submissionProgress = TeamDetailRespDTO.SubmissionProgressDTO.builder()
                .submittedMember(submittedCount)
                .totalMember(teamMembers.size())
                .build();

        return TeamDetailRespDTO.builder()
                .id(team.getId())
                .name(team.getName())
                .clubId(team.getClub().getId())
                .clubName(team.getClub().getName())
                .creatorId(team.getCreator().getId())
                .creatorName(team.getCreator().getNickname())
                .members(teamMembers.stream()
                        .map(teamMember -> createMemberInfoDTOWithTimetable(
                                teamMember,
                                team.getSuggestedScheduleAt()
                        ))
                        .collect(Collectors.toList()))
                .suggestedScheduleAt(team.getSuggestedScheduleAt())
                .submissionProgress(submissionProgress)
                .createdAt(team.getCreatedAt())
                .updatedAt(team.getUpdatedAt())
                .build();
    }

    private int calculateSubmittedCount(List<TeamMember> teamMembers, LocalDateTime suggestedScheduleAt) {
        int submittedCount = 0;
        if (suggestedScheduleAt != null) {
            for (TeamMember teamMember : teamMembers) {
                boolean isSubmitted = teamMember.getUpdatedTimetableAt() != null &&
                        teamMember.getUpdatedTimetableAt().isAfter(suggestedScheduleAt);
                if (isSubmitted) {
                    submittedCount++;
                }
            }
        }
        return submittedCount;
    }

    private TeamDetailRespDTO.MemberInfoDTO createMemberInfoDTO(TeamMember teamMember) {
        return TeamDetailRespDTO.MemberInfoDTO.builder()
                .userId(teamMember.getUser().getId())
                .name(teamMember.getUser().getNickname())
                .position(teamMember.getUser().getPosition() != null ?
                        teamMember.getUser().getPosition().name() : null)
                .build();
    }

    private TeamDetailRespDTO.MemberInfoDTO createMemberInfoDTOWithTimetable(
            TeamMember teamMember,
            LocalDateTime suggestedScheduleAt
    ) {
        JsonNode timetableData = null;
        if (teamMember.getTimetableData() != null) {
            timetableData = timetableValidationUtil.stringToJson(teamMember.getTimetableData());
        }

        boolean isSubmitted = false;
        if (suggestedScheduleAt != null && teamMember.getUpdatedTimetableAt() != null) {
            isSubmitted = teamMember.getUpdatedTimetableAt().isAfter(suggestedScheduleAt);
        }

        return TeamDetailRespDTO.MemberInfoDTO.builder()
                .userId(teamMember.getUser().getId())
                .name(teamMember.getUser().getNickname())
                .position(teamMember.getUser().getPosition() != null ?
                        teamMember.getUser().getPosition().name() : null)
                .timetableUpdatedAt(teamMember.getUpdatedTimetableAt())
                .isSubmitted(isSubmitted)
                .timetableData(timetableData)
                .build();
    }
}
