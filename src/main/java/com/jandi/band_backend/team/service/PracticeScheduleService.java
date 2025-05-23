package com.jandi.band_backend.team.service;

import com.jandi.band_backend.global.exception.ResourceNotFoundException;
import com.jandi.band_backend.team.dto.PracticeScheduleRequest;
import com.jandi.band_backend.team.dto.PracticeScheduleResponse;
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

    // 팀별 곡 연습 일정 목록 조회
    public Page<PracticeScheduleResponse> getPracticeSchedulesByTeam(Integer teamId, Pageable pageable) {
        return teamEventRepository.findPracticeSchedulesByTeamId(teamId, pageable)
                .map(PracticeScheduleResponse::from);
    }

    // 곡 연습 일정 상세 조회
    public PracticeScheduleResponse getPracticeSchedule(Integer scheduleId) {
        TeamEvent teamEvent = teamEventRepository.findByIdAndNotDeleted(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("연습 일정을 찾을 수 없습니다."));
        
        // 곡 연습 일정인지 확인 (name에 " - "가 포함되어 있는지)
        if (!teamEvent.getName().contains(" - ")) {
            throw new ResourceNotFoundException("곡 연습 일정이 아닙니다.");
        }
        
        return PracticeScheduleResponse.from(teamEvent);
    }

    // 곡 연습 일정 생성
    @Transactional
    public PracticeScheduleResponse createPracticeSchedule(PracticeScheduleRequest request, Integer creatorId) {
        Team team = teamRepository.findByIdAndNotDeleted(request.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("팀을 찾을 수 없습니다."));
        
        Users creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다."));

        TeamEvent teamEvent = new TeamEvent();
        teamEvent.setTeam(team);
        teamEvent.setCreator(creator);
        
        // 곡명과 아티스트명을 " - "로 연결하여 name 필드에 저장
        String eventName = request.getSongName();
        if (request.getArtistName() != null && !request.getArtistName().trim().isEmpty()) {
            eventName += " - " + request.getArtistName();
        }
        teamEvent.setName(eventName);
        
        teamEvent.setStartDatetime(request.getStartDatetime());
        teamEvent.setEndDatetime(request.getEndDatetime());
        teamEvent.setLocation(request.getLocation());
        teamEvent.setAddress(request.getAddress());
        
        // YouTube URL과 추가 설명을 description에 저장
        String description = "";
        if (request.getYoutubeUrl() != null && !request.getYoutubeUrl().trim().isEmpty()) {
            description = request.getYoutubeUrl();
        }
        if (request.getAdditionalDescription() != null && !request.getAdditionalDescription().trim().isEmpty()) {
            if (!description.isEmpty()) {
                description += "\n" + request.getAdditionalDescription();
            } else {
                description = request.getAdditionalDescription();
            }
        }
        teamEvent.setDescription(description);

        return PracticeScheduleResponse.from(teamEventRepository.save(teamEvent));
    }

    // 곡 연습 일정 삭제 (소프트 삭제)
    @Transactional
    public void deletePracticeSchedule(Integer scheduleId, Integer userId) {
        TeamEvent teamEvent = teamEventRepository.findByIdAndNotDeleted(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("연습 일정을 찾을 수 없습니다."));

        // 곡 연습 일정인지 확인
        if (!teamEvent.getName().contains(" - ")) {
            throw new ResourceNotFoundException("곡 연습 일정이 아닙니다.");
        }

        // 권한 체크
        if (!teamEvent.getCreator().getId().equals(userId)) {
            throw new IllegalStateException("연습 일정을 삭제할 권한이 없습니다.");
        }

        teamEvent.setDeletedAt(LocalDateTime.now());
    }
} 