// ClubEventService.java
package com.jandi.band_backend.club.service;

import com.jandi.band_backend.club.dto.ClubEventReqDTO;
import com.jandi.band_backend.club.dto.ClubEventRespDTO;
import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.entity.ClubEvent;
import com.jandi.band_backend.club.repository.ClubEventRepository;
import com.jandi.band_backend.club.repository.ClubRepository;
import com.jandi.band_backend.global.util.TimeZoneUtil;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClubEventService {

    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final ClubEventRepository clubEventRepository;

    @Transactional
    public ClubEventRespDTO createClubEvent(Integer clubId, String kakaoOauthId, ClubEventReqDTO dto) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("Club not found"));

        Users creator = userRepository.findByKakaoOauthId(kakaoOauthId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        ClubEvent clubEvent = new ClubEvent();
        clubEvent.setClub(club);
        clubEvent.setCreator(creator);
        clubEvent.setName(dto.getName());
        clubEvent.setStartDatetime(dto.getStartDatetime());
        clubEvent.setEndDatetime(dto.getEndDatetime());
        clubEvent.setLocation(dto.getLocation());
        clubEvent.setAddress(dto.getAddress());
        clubEvent.setDescription(dto.getDescription());

        ClubEvent saved = clubEventRepository.save(clubEvent);

        ClubEventRespDTO response = new ClubEventRespDTO();
        response.setId(saved.getId().longValue());
        response.setName(saved.getName());
        response.setStartDatetime(saved.getStartDatetime());
        response.setEndDatetime(saved.getEndDatetime());
        response.setLocation(saved.getLocation());
        response.setAddress(saved.getAddress());
        response.setDescription(saved.getDescription());

        return response;
    }


    @Transactional(readOnly = true)
    public ClubEventRespDTO getClubEventDetail(Integer clubId, Long eventId, String kakaoOauthId) {
        ClubEvent event = clubEventRepository
                .findByIdAndClubIdAndDeletedAtIsNull(eventId, clubId)
                .orElseThrow(() -> new IllegalArgumentException("해당 동아리에 속한 일정을 찾을 수 없습니다."));

        // Optional: 사용자 존재 여부 검증
        userRepository.findByKakaoOauthId(kakaoOauthId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        ClubEventRespDTO response = new ClubEventRespDTO();
        response.setId(event.getId().longValue());
        response.setName(event.getName());
        response.setStartDatetime(event.getStartDatetime());
        response.setEndDatetime(event.getEndDatetime());
        response.setLocation(event.getLocation());
        response.setAddress(event.getAddress());
        response.setDescription(event.getDescription());

        return response;
    }

    @Transactional(readOnly = true)
    public List<ClubEventRespDTO> getClubEventListByMonthAndTimezone(Integer clubId, String kakaoOauthId, String timezone, int year, int month) {
        userRepository.findByKakaoOauthId(kakaoOauthId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        ZoneId zoneId = TimeZoneUtil.parseZoneId(timezone);

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        // ✅ UTC 기준 Instant 대신 해당 시간대를 기준으로 한 LocalDateTime으로 비교
        LocalDateTime start = startDate.atStartOfDay();  // zoneId 사용해도 무방
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<ClubEvent> events = clubEventRepository.findByClubIdAndOverlappingDate(clubId.longValue(), start, end);

        return events.stream().map(event -> {
            ClubEventRespDTO dto = new ClubEventRespDTO();
            dto.setId(event.getId().longValue());
            dto.setName(event.getName());
            dto.setStartDatetime(event.getStartDatetime());
            dto.setEndDatetime(event.getEndDatetime());
            dto.setLocation(event.getLocation());
            dto.setAddress(event.getAddress());
            dto.setDescription(event.getDescription());
            return dto;
        }).toList();
    }


    @Transactional
    public void deleteClubEvent(Integer clubId, Long eventId, String kakaoOauthId) {
        Users user = userRepository.findByKakaoOauthId(kakaoOauthId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        ClubEvent event = clubEventRepository.findByIdAndClubIdAndDeletedAtIsNull(eventId, clubId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        if (!event.getCreator().getId().equals(user.getId())) {
            throw new IllegalArgumentException("일정을 삭제할 권한이 없습니다.");
        }

        // ✅ Instant.now() 대신 명확한 타임존 지정 또는 서버 시간 기준 사용
        event.setDeletedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        clubEventRepository.save(event);
    }

}