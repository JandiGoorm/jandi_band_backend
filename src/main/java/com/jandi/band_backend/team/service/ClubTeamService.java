package com.jandi.band_backend.team.service;

import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.repository.ClubRepository;
import com.jandi.band_backend.global.exception.ResourceNotFoundException;
import com.jandi.band_backend.team.dto.ClubTeamResponse;
import com.jandi.band_backend.team.entity.Team;
import com.jandi.band_backend.team.entity.TeamEvent;
import com.jandi.band_backend.team.repository.TeamEventRepository;
import com.jandi.band_backend.team.repository.TeamMemberRepository;
import com.jandi.band_backend.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubTeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamEventRepository teamEventRepository;
    private final ClubRepository clubRepository;

    /**
     * 동아리별 팀 목록 조회
     */
    public Page<ClubTeamResponse> getTeamsByClub(Integer clubId, Pageable pageable) {
        // 동아리 존재 확인
        clubRepository.findByIdAndDeletedAtIsNull(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("동아리를 찾을 수 없습니다."));

        Page<Team> teams = teamRepository.findAllByClubId(clubId, pageable);
        
        return teams.map(team -> {
            // 팀 멤버 수 조회
            Integer memberCount = teamMemberRepository.findByTeamId(team.getId()).size();
            
            // 현재 연습 중인 곡 조회 (가장 가까운 미래 연습 일정)
            String currentPracticeSong = getCurrentPracticeSong(team.getId());
            
            return ClubTeamResponse.from(team, memberCount, currentPracticeSong);
        });
    }

    /**
     * 팀의 현재 연습 중인 곡 조회
     */
    private String getCurrentPracticeSong(Integer teamId) {
        List<TeamEvent> latestPractices = teamEventRepository
                .findLatestPracticeSchedulesByTeamId(teamId, LocalDateTime.now(), PageRequest.of(0, 1));
        
        if (!latestPractices.isEmpty()) {
            // "곡명 - 아티스트" 형태에서 곡명만 추출하거나 전체 반환
            return latestPractices.getFirst().getName();
        }
        
        return null; // 연습 일정이 없으면 null
    }
} 