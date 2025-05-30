package com.jandi.band_backend.team.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.repository.ClubRepository;
import com.jandi.band_backend.global.exception.BadRequestException;
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

    /**
     * 곡 팀 생성
     */
    @Transactional
    public TeamDetailRespDTO createTeam(Integer clubId, TeamReqDTO teamReqDTO, Integer currentUserId) {
        // 동아리 존재 확인
        Club club = clubRepository.findByIdAndDeletedAtIsNull(clubId)
                .orElseThrow(() -> new ClubNotFoundException("동아리를 찾을 수 없습니다."));

        // 요청 사용자 확인
        Users currentUser = userValidationUtil.getUserById(currentUserId);

        // 동아리 부원 권한 확인
        permissionValidationUtil.validateClubMemberAccess(clubId, currentUserId, "동아리 부원만 팀을 생성할 수 있습니다.");

        // 팀 생성
        Team team = new Team();
        team.setClub(club);
        team.setName(teamReqDTO.getName());
        team.setCreator(currentUser);
        // 팀 생성과 동시에 suggestedScheduleAt에 현재 시간 설정
        team.setSuggestedScheduleAt(LocalDateTime.now());

        Team savedTeam = teamRepository.save(team);

        // 생성자를 팀원으로 추가
        TeamMember teamMember = new TeamMember();
        teamMember.setTeam(savedTeam);
        teamMember.setUser(currentUser);
        teamMemberRepository.save(teamMember);

        // 팀 멤버 목록 조회
        List<TeamMember> teamMembers = teamMemberRepository.findByTeamIdAndDeletedAtIsNull(savedTeam.getId());

        return createTeamDetailRespDTO(savedTeam, teamMembers);
    }

    /**
     * 동아리별 팀 목록 조회
     */
    public Page<TeamRespDTO> getTeamsByClub(Integer clubId, Pageable pageable, Integer currentUserId) {
        // 동아리 존재 확인
        Club club = clubRepository.findByIdAndDeletedAtIsNull(clubId)
                .orElseThrow(() -> new ClubNotFoundException("동아리를 찾을 수 없습니다."));

        // 동아리 부원 권한 확인
        permissionValidationUtil.validateClubMemberAccess(clubId, currentUserId, "동아리 부원만 팀 목록을 조회할 수 있습니다.");

        Page<Team> teams = teamRepository.findAllByClubAndDeletedAtIsNullOrderByCreatedAtDesc(club, pageable);

        return teams.map(team -> {
            Integer memberCount = teamMemberRepository.countByTeamIdAndDeletedAtIsNull(team.getId());
            return createTeamRespDTO(team, memberCount);
        });
    }

    /**
     * 팀 상세 조회 (시간표 정보 포함)
     */
    public TeamDetailRespDTO getTeamDetail(Integer teamId, Integer currentUserId) {
        // 팀 존재 확인
        Team team = teamRepository.findByIdAndDeletedAtIsNull(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 팀입니다."));

        // 동아리 부원 권한 확인
        permissionValidationUtil.validateClubMemberAccess(team.getClub().getId(), currentUserId, "해당 동아리 부원만 팀 정보를 조회할 수 있습니다.");

        // 팀 멤버 목록 조회
        List<TeamMember> teamMembers = teamMemberRepository.findByTeamIdAndDeletedAtIsNull(teamId);

        return createTeamDetailRespDTOWithTimetable(team, teamMembers);
    }

    /**
     * 팀 이름 수정
     */
    @Transactional
    public TeamRespDTO updateTeam(Integer teamId, TeamReqDTO teamReqDTO, Integer currentUserId) {
        // 팀 존재 확인
        Team team = teamRepository.findByIdAndDeletedAtIsNull(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 팀입니다."));

        // 팀 멤버 권한 확인
        permissionValidationUtil.validateTeamMemberAccess(teamId, currentUserId, "팀 멤버만 팀 이름을 수정할 수 있습니다.");

        // 팀 이름 수정
        team.setName(teamReqDTO.getName());

        Team updatedTeam = teamRepository.save(team);

        // 팀 멤버 수 조회
        Integer memberCount = teamMemberRepository.countByTeamIdAndDeletedAtIsNull(teamId);

        return createTeamRespDTO(updatedTeam, memberCount);
    }

    /**
     * 팀 삭제 (소프트 삭제)
     */
    @Transactional
    public void deleteTeam(Integer teamId, Integer currentUserId) {
        // 팀 존재 확인
        Team team = teamRepository.findByIdAndDeletedAtIsNull(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 팀입니다."));

        // 팀 멤버 권한 확인
        permissionValidationUtil.validateTeamMemberAccess(teamId, currentUserId, "팀 멤버만 팀을 삭제할 수 있습니다.");

        // 팀 멤버들 소프트 삭제
        LocalDateTime deletedTime = LocalDateTime.now();
        List<TeamMember> teamMembers = teamMemberRepository.findByTeamIdAndDeletedAtIsNull(teamId);
        teamMembers.forEach(teamMember -> teamMember.setDeletedAt(deletedTime));
        teamMemberRepository.saveAll(teamMembers);

        // 팀 이벤트들 소프트 삭제
        List<TeamEvent> teamEvents = teamEventRepository.findAllByTeamIdAndDeletedAtIsNull(teamId);
        teamEvents.forEach(teamEvent -> teamEvent.setDeletedAt(deletedTime));
        teamEventRepository.saveAll(teamEvents);

        // 팀 소프트 삭제
        team.setDeletedAt(deletedTime);
        teamRepository.save(team);
    }

    /**
     * 팀 탈퇴
     */
    @Transactional
    public void leaveTeam(Integer teamId, Integer currentUserId) {
        // 팀 존재 확인
        Team team = teamRepository.findByIdAndDeletedAtIsNull(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 팀입니다."));

        // 팀 멤버 권한 확인
        TeamMember teamMember = permissionValidationUtil.validateTeamMemberAccess(teamId, currentUserId, "해당 팀의 멤버가 아닙니다.");

        // 팀원이 한명인 경우?
        if (teamMemberRepository.countByTeamIdAndDeletedAtIsNull(teamId) == 1) {
            throw new TeamLeaveNotAllowedException("마지막 남은 팀원은 탈퇴할 수 없습니다. 팀을 삭제해주세요.");
        }

        // 팀 멤버 소프트 삭제
        teamMember.setDeletedAt(LocalDateTime.now());
        teamMemberRepository.save(teamMember);
    }

    /**
     * TeamRespDTO 생성
     */
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

    /**
     * TeamDetailRespDTO 생성
     */
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

    /**
     * TeamDetailRespDTO 생성 (시간표 정보 포함)
     */
    private TeamDetailRespDTO createTeamDetailRespDTOWithTimetable(Team team, List<TeamMember> teamMembers) {
        // 제출 현황 계산
        int submittedCount = 0;
        if (team.getSuggestedScheduleAt() != null) {
            for (TeamMember teamMember : teamMembers) {
                boolean isSubmitted = teamMember.getUpdatedTimetableAt() != null &&
                        teamMember.getUpdatedTimetableAt().isAfter(team.getSuggestedScheduleAt());
                if (isSubmitted) {
                    submittedCount++;
                }
            }
        }

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
                        .map(teamMember -> createMemberInfoDTOWithTimetable(teamMember, team.getSuggestedScheduleAt()))
                        .collect(Collectors.toList()))
                .suggestedScheduleAt(team.getSuggestedScheduleAt())
                .submissionProgress(submissionProgress)
                .createdAt(team.getCreatedAt())
                .updatedAt(team.getUpdatedAt())
                .build();
    }

    /**
     * MemberInfoDTO 생성
     */
    private TeamDetailRespDTO.MemberInfoDTO createMemberInfoDTO(TeamMember teamMember) {
        return TeamDetailRespDTO.MemberInfoDTO.builder()
                .userId(teamMember.getUser().getId())
                .name(teamMember.getUser().getNickname())
                .position(teamMember.getUser().getPosition() != null ?
                        teamMember.getUser().getPosition().name() : null)
                .build();
    }

    /**
     * MemberInfoDTO 생성 (시간표 정보 포함)
     */
    private TeamDetailRespDTO.MemberInfoDTO createMemberInfoDTOWithTimetable(TeamMember teamMember, LocalDateTime suggestedScheduleAt) {
        // 시간표 데이터 파싱
        JsonNode timetableData = null;
        if (teamMember.getTimetableData() != null) {
            timetableData = teamTimetableUtil.stringToJson(teamMember.getTimetableData());
        }

        // 제출 여부 확인
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
