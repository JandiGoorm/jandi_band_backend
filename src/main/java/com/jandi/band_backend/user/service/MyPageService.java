package com.jandi.band_backend.user.service;

import com.jandi.band_backend.club.entity.ClubMember;
import com.jandi.band_backend.club.entity.ClubPhoto;
import com.jandi.band_backend.club.repository.ClubMemberRepository;
import com.jandi.band_backend.club.repository.ClubPhotoRepository;
import com.jandi.band_backend.team.entity.TeamMember;
import com.jandi.band_backend.team.repository.TeamMemberRepository;
import com.jandi.band_backend.user.dto.MyClubResponse;
import com.jandi.band_backend.user.dto.MyTeamResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final ClubMemberRepository clubMemberRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ClubPhotoRepository clubPhotoRepository;

    /**
     * 내가 참가한 동아리 목록 조회
     */
    public List<MyClubResponse> getMyClubs(Integer userId) {
        List<ClubMember> clubMembers = clubMemberRepository.findByUserId(userId);
        
        return clubMembers.stream()
                .map(clubMember -> {
                    // 동아리 대표 사진 URL 조회
                    String photoUrl = getClubMainPhotoUrl(clubMember.getClub().getId());
                    
                    // 동아리 멤버 수 조회
                    Integer memberCount = clubMemberRepository.countByClubId(clubMember.getClub().getId());
                    
                    return MyClubResponse.from(clubMember, photoUrl, memberCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * 내가 참가한 팀 목록 조회
     */
    public List<MyTeamResponse> getMyTeams(Integer userId) {
        List<TeamMember> teamMembers = teamMemberRepository.findByUserId(userId);
        
        return teamMembers.stream()
                .map(teamMember -> {
                    // 팀 멤버 수 조회
                    Integer memberCount = teamMemberRepository.findByTeamId(teamMember.getTeam().getId()).size();
                    
                    return MyTeamResponse.from(teamMember, memberCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * 동아리 대표 사진 URL 조회 헬퍼 메서드
     */
    private String getClubMainPhotoUrl(Integer clubId) {
        return clubPhotoRepository.findByClubIdAndIsCurrentTrueAndDeletedAtIsNull(clubId)
                .map(ClubPhoto::getImageUrl)
                .orElse(null);
    }
} 