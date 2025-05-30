package com.jandi.band_backend.club.service;

import com.jandi.band_backend.club.dto.ClubEventReqDTO;
import com.jandi.band_backend.club.dto.ClubEventRespDTO;
import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.entity.ClubEvent;
import com.jandi.band_backend.club.entity.ClubMember;
import com.jandi.band_backend.club.repository.ClubEventRepository;
import com.jandi.band_backend.club.repository.ClubRepository;
import com.jandi.band_backend.club.repository.ClubMemberRepository;
import com.jandi.band_backend.global.util.UserValidationUtil;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ClubEventService {

    private final ClubRepository clubRepository;
    private final UserRepository userRepository;
    private final ClubEventRepository clubEventRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final UserValidationUtil userValidationUtil;

    @Transactional
    public ClubEventRespDTO createClubEvent(Integer clubId, Integer userId, ClubEventReqDTO dto) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("동아리를 찾을 수 없습니다."));
        Users creator = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        boolean isMember = clubMemberRepository.findByClubIdAndUserIdAndDeletedAtIsNull(clubId, userId).isPresent();
        if (!isMember) {
            throw new IllegalArgumentException("동아리 부원만 이벤트를 생성할 수 있습니다.");
        }

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
    public ClubEventRespDTO getClubEventDetail(Integer clubId, Integer eventId, Integer userId) {
        ClubEvent event = clubEventRepository
                .findByIdAndClubIdAndDeletedAtIsNull(eventId, clubId)
                .orElseThrow(() -> new IllegalArgumentException("해당 동아리에 속한 일정을 찾을 수 없습니다."));

        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return convertToClubEventRespDTO(event);
    }

    @Transactional(readOnly = true)
    public List<ClubEventRespDTO> getClubEventListByMonth(Integer clubId, Integer userId, int year, int month) {
        userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(23, 59, 59);

        List<ClubEvent> events = clubEventRepository.findByClubIdAndOverlappingDate(clubId, start, end);

        return events.stream()
                .map(this::convertToClubEventRespDTO)
                .toList();
    }

    // 클럽 이벤트 삭제 (ADMIN은 모든 이벤트 삭제 가능)
    @Transactional
    public void deleteClubEvent(Integer clubId, Integer eventId, Integer userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        ClubEvent event = clubEventRepository.findByIdAndClubIdAndDeletedAtIsNull(eventId, clubId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        // 권한 체크 (작성자 또는 ADMIN 또는 동아리 대표자)
        if (!canDeleteEvent(clubId, userId, event)) {
            throw new IllegalArgumentException("일정을 삭제할 권한이 없습니다.");
        }

        // 이벤트 소프트 삭제
        event.setDeletedAt(LocalDateTime.now());
        clubEventRepository.save(event);
    }

    // ClubEvent 엔터티를 ClubEventRespDTO로 변환하는 헬퍼 메서드
    private ClubEventRespDTO convertToClubEventRespDTO(ClubEvent event) {
        return ClubEventRespDTO.builder()
                .id(event.getId().longValue())
                .name(event.getName())
                .startDatetime(event.getStartDatetime())
                .endDatetime(event.getEndDatetime())
                .build();
    }

    // ADMIN 권한 확인
    private boolean isAdmin(Integer userId) {
        Users user = userValidationUtil.getUserById(userId);
        return user.getAdminRole() == Users.AdminRole.ADMIN;
    }

    // 동아리 대표자 또는 이벤트 작성자인지 확인하는 헬퍼 메서드
    private boolean canDeleteEvent(Integer clubId, Integer userId, ClubEvent event) {
        // ADMIN은 모든 이벤트 삭제 가능
        if (isAdmin(userId)) {
            return true;
        }
        // 이벤트 작성자인 경우
        if (event.getCreator().getId().equals(userId)) {
            return true;
        }
        // 동아리 대표자인 경우
        ClubMember member = clubMemberRepository.findByClubIdAndUserIdAndDeletedAtIsNull(clubId, userId)
                .orElse(null);
        if (member != null && member.getRole() == ClubMember.MemberRole.REPRESENTATIVE) {
            return true;
        }

        return false;
    }
}
