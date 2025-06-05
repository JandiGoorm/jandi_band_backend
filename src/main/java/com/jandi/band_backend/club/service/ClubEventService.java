package com.jandi.band_backend.club.service;

import com.jandi.band_backend.club.dto.CalendarEventRespDTO;
import com.jandi.band_backend.club.dto.ClubEventReqDTO;
import com.jandi.band_backend.club.dto.ClubEventRespDTO;
import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.entity.ClubEvent;
import com.jandi.band_backend.club.entity.ClubMember;
import com.jandi.band_backend.club.repository.ClubEventRepository;
import com.jandi.band_backend.club.repository.ClubRepository;
import com.jandi.band_backend.club.repository.ClubMemberRepository;
import com.jandi.band_backend.global.util.EntityValidationUtil;
import com.jandi.band_backend.global.util.PermissionValidationUtil;
import com.jandi.band_backend.global.util.UserValidationUtil;
import com.jandi.band_backend.team.entity.Team;
import com.jandi.band_backend.team.entity.TeamEvent;
import com.jandi.band_backend.team.repository.TeamRepository;
import com.jandi.band_backend.team.repository.TeamEventRepository;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClubEventService {

    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final ClubEventRepository clubEventRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final TeamRepository teamRepository;
    private final TeamEventRepository teamEventRepository;
    private final EntityValidationUtil entityValidationUtil;
    private final PermissionValidationUtil permissionValidationUtil;
    private final UserValidationUtil userValidationUtil;

    @Transactional
    @CacheEvict(value = "calendarEvents", allEntries = true)
    public ClubEventRespDTO createClubEvent(Integer clubId, Integer userId, ClubEventReqDTO dto) {
        Club club = entityValidationUtil.validateClubExists(clubId);
        Users creator = userValidationUtil.getUserById(userId);

        permissionValidationUtil.validateClubMemberAccess(clubId, userId, "동아리 부원만 이벤트를 생성할 수 있습니다.");

        ClubEvent clubEvent = new ClubEvent();
        clubEvent.setClub(club);
        clubEvent.setCreator(creator);
        clubEvent.setName(dto.getName());
        clubEvent.setStartDatetime(dto.getStartDatetime());
        clubEvent.setEndDatetime(dto.getEndDatetime());

        ClubEvent saved = clubEventRepository.save(clubEvent);

        return convertToClubEventRespDTO(saved);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "calendarEvents", key = "'club_event_' + #clubId + '_' + #eventId")
    public ClubEventRespDTO getClubEventDetail(Integer clubId, Integer eventId, Integer userId) {
        ClubEvent event = clubEventRepository
                .findByIdAndClubIdAndDeletedAtIsNull(eventId, clubId)
                .orElseThrow(() -> new IllegalArgumentException("해당 동아리에 속한 일정을 찾을 수 없습니다."));

        userValidationUtil.getUserById(userId);

        return convertToClubEventRespDTO(event);
    }

    // 캘린더용 통합 일정 조회 (동아리 일정 + 모든 하위 팀 일정)
    @Transactional(readOnly = true)
    @Cacheable(value = "calendarEvents", key = "'calendar_' + #clubId + '_' + #year + '_' + #month")
    public List<CalendarEventRespDTO> getCalendarEventsForClub(Integer clubId, Integer userId, int year, int month) {
        userValidationUtil.getUserById(userId);

        Club club = entityValidationUtil.validateClubExists(clubId);

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<CalendarEventRespDTO> calendarEvents = new ArrayList<>();

        // 1. 동아리 일정 조회
        List<ClubEvent> clubEvents = clubEventRepository.findByClubIdAndOverlappingDate(clubId, start, end);
        clubEvents.stream()
                .map(CalendarEventRespDTO::fromClubEvent)
                .forEach(calendarEvents::add);

        // 2. 해당 동아리의 모든 팀 조회
        List<Team> teams = teamRepository.findAllByClubIdAndDeletedAtIsNull(clubId);

        // 3. 각 팀의 일정 조회
        for (Team team : teams) {
            List<TeamEvent> teamEvents = teamEventRepository.findTeamEventsByTeamIdAndDateRange(team.getId(), start, end);
            teamEvents.stream()
                    .map(CalendarEventRespDTO::fromTeamEvent)
                    .forEach(calendarEvents::add);
        }

        // 4. 시작 시간 순으로 정렬
        calendarEvents.sort(Comparator.comparing(CalendarEventRespDTO::getStartDatetime));

        return calendarEvents;
    }

    // 클럽 이벤트 삭제 (ADMIN은 모든 이벤트 삭제 가능)
    @Transactional
    @CacheEvict(value = "calendarEvents", allEntries = true)
    public void deleteClubEvent(Integer clubId, Integer eventId, Integer userId) {
        Users user = userValidationUtil.getUserById(userId);

        ClubEvent event = clubEventRepository.findByIdAndClubIdAndDeletedAtIsNull(eventId, clubId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        if (!canDeleteEvent(clubId, userId, event)) {
            throw new IllegalArgumentException("일정을 삭제할 권한이 없습니다.");
        }

        event.setDeletedAt(LocalDateTime.now());
        clubEventRepository.save(event);
    }

    private ClubEventRespDTO convertToClubEventRespDTO(ClubEvent event) {
        return ClubEventRespDTO.builder()
                .id(event.getId().longValue())
                .name(event.getName())
                .startDatetime(event.getStartDatetime())
                .endDatetime(event.getEndDatetime())
                .build();
    }

    private boolean isAdmin(Integer userId) {
        Users user = userValidationUtil.getUserById(userId);
        return user.getAdminRole() == Users.AdminRole.ADMIN;
    }

    private boolean canDeleteEvent(Integer clubId, Integer userId, ClubEvent event) {
        if (isAdmin(userId)) {
            return true;
        }
        if (event.getCreator().getId().equals(userId)) {
            return true;
        }
        ClubMember member = clubMemberRepository.findByClubIdAndUserIdAndDeletedAtIsNull(clubId, userId)
                .orElse(null);
        if (member != null && member.getRole() == ClubMember.MemberRole.REPRESENTATIVE) {
            return true;
        }

        return false;
    }
}
