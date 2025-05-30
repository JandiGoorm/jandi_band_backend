package com.jandi.band_backend.team.service;

import com.jandi.band_backend.global.exception.ResourceNotFoundException;
import com.jandi.band_backend.global.exception.TeamNotFoundException;
import com.jandi.band_backend.global.exception.UserNotFoundException;
import com.jandi.band_backend.global.util.PermissionValidationUtil;
import com.jandi.band_backend.team.dto.PracticeScheduleReqDTO;
import com.jandi.band_backend.team.dto.PracticeScheduleRespDTO;
import com.jandi.band_backend.team.entity.Team;
import com.jandi.band_backend.team.entity.TeamEvent;
import com.jandi.band_backend.team.repository.TeamEventRepository;
import com.jandi.band_backend.team.repository.TeamRepository;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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

    // 팀별 곡 연습 일정 목록 조회 (동아리 멤버면 조회 가능)
    public Page<PracticeScheduleRespDTO> getPracticeSchedulesByTeam(Integer teamId, Pageable pageable, Integer userId) {
        // 팀 존재 여부 확인
        Team team = teamRepository.findByIdAndDeletedAtIsNull(teamId)
                .orElseThrow(() -> new TeamNotFoundException("팀을 찾을 수 없습니다."));

        // 동아리 멤버십 확인 (ADMIN은 자동 통과)
        permissionValidationUtil.validateClubMemberAccess(
            team.getClub().getId(),
            userId,
            "해당 팀의 일정을 조회할 권한이 없습니다."
        );

        return teamEventRepository.findPracticeSchedulesByTeamId(teamId, pageable)
                .map(PracticeScheduleRespDTO::from);
    }

    // 곡 연습 일정 상세 조회 (동아리 멤버면 조회 가능)
    public PracticeScheduleRespDTO getPracticeSchedule(Integer scheduleId, Integer userId) {
        TeamEvent teamEvent = teamEventRepository.findByIdAndNotDeleted(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("연습 일정을 찾을 수 없습니다."));

        // 동아리 멤버십 확인 (ADMIN은 자동 통과)
        permissionValidationUtil.validateClubMemberAccess(
            teamEvent.getTeam().getClub().getId(),
            userId,
            "해당 연습 일정을 조회할 권한이 없습니다."
        );

        return PracticeScheduleRespDTO.from(teamEvent);
    }

    // 곡 연습 일정 생성 (팀 멤버만 가능)
    @Transactional
    public PracticeScheduleRespDTO createPracticeSchedule(PracticeScheduleReqDTO request, Integer creatorId) {
        Team team = teamRepository.findByIdAndDeletedAtIsNull(request.getTeamId())
                .orElseThrow(() -> new TeamNotFoundException("팀을 찾을 수 없습니다."));

        Users creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다."));

        // 팀 멤버십 확인 (ADMIN은 자동 통과)
        permissionValidationUtil.validateTeamMemberAccess(
            request.getTeamId(),
            creatorId,
            "해당 팀에 연습 일정을 생성할 권한이 없습니다."
        );

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

        return PracticeScheduleRespDTO.from(teamEventRepository.save(teamEvent));
    }

    // 곡 연습 일정 삭제 (팀 멤버만 가능)
    @Transactional
    public void deletePracticeSchedule(Integer scheduleId, Integer userId) {
        TeamEvent teamEvent = teamEventRepository.findByIdAndNotDeleted(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("연습 일정을 찾을 수 없습니다."));

        // 팀 멤버십 확인 (ADMIN은 자동 통과)
        permissionValidationUtil.validateTeamMemberAccess(
            teamEvent.getTeam().getId(),
            userId,
            "연습 일정을 삭제할 권한이 없습니다."
        );

        teamEvent.setDeletedAt(LocalDateTime.now());
    }
}
