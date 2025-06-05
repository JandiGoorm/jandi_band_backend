package com.jandi.band_backend.team.service;

import com.jandi.band_backend.global.exception.ResourceNotFoundException;
import com.jandi.band_backend.global.exception.TeamNotFoundException;
import com.jandi.band_backend.global.exception.UserNotFoundException;
import com.jandi.band_backend.global.util.PermissionValidationUtil;
import com.jandi.band_backend.global.util.EntityValidationUtil;
import com.jandi.band_backend.global.util.UserValidationUtil;
import com.jandi.band_backend.team.dto.PracticeScheduleReqDTO;
import com.jandi.band_backend.team.dto.PracticeScheduleRespDTO;
import com.jandi.band_backend.team.entity.Team;
import com.jandi.band_backend.team.entity.TeamEvent;
import com.jandi.band_backend.team.repository.TeamEventRepository;
import com.jandi.band_backend.team.repository.TeamRepository;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PracticeScheduleService {

    private final TeamEventRepository teamEventRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final PermissionValidationUtil permissionValidationUtil;
    private final EntityValidationUtil entityValidationUtil;
    private final UserValidationUtil userValidationUtil;

    @Cacheable(value = "schedules", key = "'team_' + #teamId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<PracticeScheduleRespDTO> getPracticeSchedulesByTeam(Integer teamId, Pageable pageable, Integer userId) {
        Team team = entityValidationUtil.validateTeamExists(teamId);

        permissionValidationUtil.validateClubMemberAccess(
            team.getClub().getId(),
            userId,
            "해당 팀의 일정을 조회할 권한이 없습니다."
        );

        return teamEventRepository.findPracticeSchedulesByTeamId(teamId, pageable)
                .map(PracticeScheduleRespDTO::from);
    }

    @Cacheable(value = "schedules", key = "'schedule_' + #scheduleId")
    public PracticeScheduleRespDTO getPracticeSchedule(Integer scheduleId, Integer userId) {
        TeamEvent teamEvent = entityValidationUtil.validateTeamEventExists(scheduleId);

        permissionValidationUtil.validateClubMemberAccess(
            teamEvent.getTeam().getClub().getId(),
            userId,
            "해당 연습 일정을 조회할 권한이 없습니다."
        );

        return PracticeScheduleRespDTO.from(teamEvent);
    }

    @Cacheable(value = "schedules", key = "'detail_' + #teamId + '_' + #scheduleId")
    public PracticeScheduleRespDTO getPracticeScheduleDetail(Integer teamId, Integer scheduleId, Integer userId) {
        TeamEvent teamEvent = entityValidationUtil.validateTeamEventBelongsToTeam(teamId, scheduleId);

        permissionValidationUtil.validateClubMemberAccess(
            teamEvent.getTeam().getClub().getId(),
            userId,
            "해당 연습 일정을 조회할 권한이 없습니다."
        );

        return PracticeScheduleRespDTO.from(teamEvent);
    }

    @Transactional
    @CacheEvict(value = {"schedules", "calendarEvents"}, allEntries = true)
    public PracticeScheduleRespDTO createPracticeSchedule(Integer teamId, PracticeScheduleReqDTO request, Integer creatorId) {
        Team team = entityValidationUtil.validateTeamExists(teamId);
        Users creator = userValidationUtil.getUserById(creatorId);

        permissionValidationUtil.validateTeamMemberAccess(
            teamId,
            creatorId,
            "해당 팀에 연습 일정을 생성할 권한이 없습니다."
        );

        TeamEvent teamEvent = createTeamEventFromRequest(team, creator, request);
        return PracticeScheduleRespDTO.from(teamEventRepository.save(teamEvent));
    }

    @Transactional
    @CacheEvict(value = {"schedules", "calendarEvents"}, allEntries = true)
    public void deletePracticeSchedule(Integer scheduleId, Integer userId) {
        TeamEvent teamEvent = entityValidationUtil.validateTeamEventExists(scheduleId);

        permissionValidationUtil.validateTeamMemberAccess(
            teamEvent.getTeam().getId(),
            userId,
            "연습 일정을 삭제할 권한이 없습니다."
        );

        teamEvent.setDeletedAt(LocalDateTime.now());
    }

    @Transactional
    @CacheEvict(value = {"schedules", "calendarEvents"}, allEntries = true)
    public void deletePracticeScheduleByTeam(Integer teamId, Integer scheduleId, Integer userId) {
        TeamEvent teamEvent = entityValidationUtil.validateTeamEventBelongsToTeam(teamId, scheduleId);

        permissionValidationUtil.validateTeamMemberAccess(
            teamId,
            userId,
            "연습 일정을 삭제할 권한이 없습니다."
        );

        teamEvent.setDeletedAt(LocalDateTime.now());
    }

    private TeamEvent createTeamEventFromRequest(Team team, Users creator, PracticeScheduleReqDTO request) {
        TeamEvent teamEvent = new TeamEvent();
        teamEvent.setTeam(team);
        teamEvent.setCreator(creator);
        teamEvent.setName(request.getName());
        teamEvent.setStartDatetime(request.getStartDatetime());
        teamEvent.setEndDatetime(request.getEndDatetime());

        // noPosition 설정 (String을 enum으로 변환)
        if (request.getNoPosition() != null && !request.getNoPosition().trim().isEmpty()) {
            try {
                TeamEvent.NoPosition noPosition = TeamEvent.NoPosition.valueOf(request.getNoPosition().toUpperCase());
                teamEvent.setNoPosition(noPosition);
            } catch (IllegalArgumentException e) {
                teamEvent.setNoPosition(TeamEvent.NoPosition.NONE);
            }
        } else {
            teamEvent.setNoPosition(TeamEvent.NoPosition.NONE);
        }

        return teamEvent;
    }
}
