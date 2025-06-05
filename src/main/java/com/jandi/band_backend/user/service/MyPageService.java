package com.jandi.band_backend.user.service;

import com.jandi.band_backend.club.entity.ClubMember;
import com.jandi.band_backend.club.entity.ClubPhoto;
import com.jandi.band_backend.club.repository.ClubMemberRepository;
import com.jandi.band_backend.club.repository.ClubPhotoRepository;
import com.jandi.band_backend.team.entity.TeamMember;
import com.jandi.band_backend.team.repository.TeamMemberRepository;
import com.jandi.band_backend.user.dto.MyClubRespDTO;
import com.jandi.band_backend.user.dto.MyTeamRespDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
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
    @Cacheable(value = "myPages", key = "'clubs_' + #userId")
    public List<MyClubRespDTO> getMyClubs(Integer userId) {
        List<ClubMember> clubMembers = clubMemberRepository.findByUserIdAndClubDeletedAtIsNullAndDeletedAtIsNullOrderByJoinedAtDesc(userId);

        return clubMembers.stream()
                .map(clubMember -> {
                    String photoUrl = getClubMainPhotoUrl(clubMember.getClub().getId());
                    Integer memberCount = clubMemberRepository.countByClubIdAndDeletedAtIsNull(clubMember.getClub().getId());
                    return MyClubRespDTO.from(clubMember, photoUrl, memberCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * 내가 참가한 팀 목록 조회
     */
    @Cacheable(value = "myPages", key = "'teams_' + #userId")
    public List<MyTeamRespDTO> getMyTeams(Integer userId) {
        List<TeamMember> teamMembers = teamMemberRepository.findByUserIdAndTeamDeletedAtIsNullAndDeletedAtIsNullOrderByJoinedAtDesc(userId);

        return teamMembers.stream()
                .map(teamMember -> {
                    Integer memberCount = teamMemberRepository.countByTeamIdAndDeletedAtIsNull(teamMember.getTeam().getId());
                    return MyTeamRespDTO.from(teamMember, memberCount);
                })
                .collect(Collectors.toList());
    }

    private String getClubMainPhotoUrl(Integer clubId) {
        return clubPhotoRepository.findByClubIdAndIsCurrentTrueAndDeletedAtIsNull(clubId)
                .map(ClubPhoto::getImageUrl)
                .orElse(null);
    }
}
