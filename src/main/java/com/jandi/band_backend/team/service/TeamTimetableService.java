package com.jandi.band_backend.team.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.jandi.band_backend.global.exception.BadRequestException;
import com.jandi.band_backend.global.exception.InvalidAccessException;
import com.jandi.band_backend.global.exception.ResourceNotFoundException;
import com.jandi.band_backend.global.exception.TimetableNotFoundException;
import com.jandi.band_backend.team.dto.ScheduleSuggestionRespDTO;
import com.jandi.band_backend.team.dto.TimetableReqDTO;
import com.jandi.band_backend.team.dto.TimetableUpdateReqDTO;
import com.jandi.band_backend.team.dto.TimetableRespDTO;
import com.jandi.band_backend.team.entity.Team;
import com.jandi.band_backend.team.entity.TeamMember;
import com.jandi.band_backend.team.repository.TeamMemberRepository;
import com.jandi.band_backend.team.repository.TeamRepository;
import com.jandi.band_backend.user.entity.UserTimetable;
import com.jandi.band_backend.user.repository.UserTimetableRepository;
import com.jandi.band_backend.team.util.TeamTimetableUtil;
import com.jandi.band_backend.user.util.UserTimetableUtil;
import com.jandi.band_backend.global.util.PermissionValidationUtil;
import com.jandi.band_backend.global.util.UserValidationUtil;
import com.jandi.band_backend.global.util.TimetableValidationUtil;
import com.jandi.band_backend.global.util.EntityValidationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamTimetableService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserTimetableRepository userTimetableRepository;
    private final ObjectMapper objectMapper;
    private final TeamTimetableUtil teamTimetableUtil;
    private final UserTimetableUtil userTimetableUtil;
    private final PermissionValidationUtil permissionValidationUtil;
    private final UserValidationUtil userValidationUtil;
    private final TimetableValidationUtil timetableValidationUtil;
    private final EntityValidationUtil entityValidationUtil;

    @Transactional
    public ScheduleSuggestionRespDTO startScheduleSuggestion(Integer teamId, Integer currentUserId) {
        Team team = entityValidationUtil.validateTeamExists(teamId);

        TeamMember teamMember = permissionValidationUtil.validateTeamMemberAccess(
                teamId,
                currentUserId,
                "팀원만 접근할 수 있습니다."
        );

        LocalDateTime now = LocalDateTime.now();
        team.setSuggestedScheduleAt(now);
        teamRepository.save(team);

        return ScheduleSuggestionRespDTO.builder()
                .teamId(teamId)
                .suggestedScheduleAt(now)
                .suggesterUserId(currentUserId)
                .suggesterName(teamMember.getUser().getNickname())
                .build();
    }

    @Transactional
    public TimetableRespDTO registerMyTimetable(Integer teamId, TimetableReqDTO reqDTO, Integer currentUserId) {
        TeamMember teamMember = validateTeamAndGetTeamMember(teamId, currentUserId);
        UserTimetable userTimetable = getUserTimetableWithPermissionCheck(
                currentUserId,
                reqDTO.getUserTimetableId()
        );
        JsonNode timetableData = timetableValidationUtil.stringToJson(userTimetable.getTimetableData());

        return saveTeamMemberTimetableAndBuildResponse(teamMember, timetableData, currentUserId, teamId);
    }

    @Transactional
    public TimetableRespDTO updateMyTimetable(Integer teamId, TimetableUpdateReqDTO reqDTO, Integer currentUserId) {
        TeamMember teamMember = validateTeamAndGetTeamMember(teamId, currentUserId);

        teamTimetableUtil.validateTimetableRequest(reqDTO);

        return saveTeamMemberTimetableAndBuildResponse(
                teamMember,
                reqDTO.getTimetableData(),
                currentUserId,
                teamId
        );
    }

    private TeamMember validateTeamAndGetTeamMember(Integer teamId, Integer currentUserId) {
        entityValidationUtil.validateTeamExists(teamId);
        return permissionValidationUtil.validateTeamMemberAccess(
                teamId,
                currentUserId,
                "본인의 시간표만 입력할 수 있습니다."
        );
    }

    private UserTimetable getUserTimetableWithPermissionCheck(Integer currentUserId, Integer userTimetableId) {
        if (userTimetableId == null) {
            throw new BadRequestException("시간표 ID는 필수입니다.");
        }

        // JOIN FETCH를 사용하여 User 정보도 함께 조회
        UserTimetable userTimetable = userTimetableRepository
                .findByIdWithUserAndDeletedAtIsNull(userTimetableId)
                .orElseThrow(() -> new TimetableNotFoundException("존재하지 않는 시간표입니다."));

        if (userTimetable.getUser() == null || userTimetable.getUser().getId() == null) {
            throw new BadRequestException("시간표 소유자 정보를 찾을 수 없습니다.");
        }

        Integer ownerId = userTimetable.getUser().getId();

        permissionValidationUtil.validateContentOwnership(
                ownerId,
                currentUserId,
                "권한이 없습니다: 본인의 시간표가 아닙니다"
        );

        return userTimetable;
    }

    private TimetableRespDTO saveTeamMemberTimetableAndBuildResponse(
            TeamMember teamMember,
            JsonNode timetableData,
            Integer currentUserId,
            Integer teamId
    ) {
        try {
            String timetableJson = objectMapper.writeValueAsString(timetableData);
            teamMember.setTimetableData(timetableJson);
            teamMember.setUpdatedTimetableAt(LocalDateTime.now());

            teamMemberRepository.save(teamMember);

            return TimetableRespDTO.builder()
                    .userId(currentUserId)
                    .teamId(teamId)
                    .timetableData(timetableData)
                    .updatedTimetableAt(teamMember.getUpdatedTimetableAt())
                    .build();

        } catch (JacksonException e) {
            log.error("시간표 데이터 JSON 변환 오류: {}", e.getMessage());
            throw new BadRequestException("시간표 데이터 처리 중 오류가 발생했습니다.");
        }
    }
}
