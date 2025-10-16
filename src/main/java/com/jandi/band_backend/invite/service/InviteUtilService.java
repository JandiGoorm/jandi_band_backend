package com.jandi.band_backend.invite.service;

import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.entity.ClubMember;
import com.jandi.band_backend.club.repository.ClubMemberRepository;
import com.jandi.band_backend.club.repository.ClubRepository;
import com.jandi.band_backend.global.exception.ClubNotFoundException;
import com.jandi.band_backend.global.exception.TeamNotFoundException;
import com.jandi.band_backend.invite.redis.InviteType;
import com.jandi.band_backend.team.entity.Team;
import com.jandi.band_backend.team.entity.TeamMember;
import com.jandi.band_backend.team.repository.TeamMemberRepository;
import com.jandi.band_backend.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InviteUtilService {
    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;

    /// 동아리 관련
    // 동아리 존재 확인
    protected void isExistClub(Integer clubId) {
        clubRepository.findByIdAndDeletedAtIsNull(clubId)
                .orElseThrow(() -> new ClubNotFoundException("동아리가 존재하지 않습니다"));
    }

    // 동아리 부원인지 확인
    protected boolean isMemberOfClub(Integer clubId, Integer userId) {
        Optional<ClubMember> Optional = clubMemberRepository.findByClubIdAndUserIdAndDeletedAtIsNull(clubId, userId);
        return Optional.isPresent();
    }

    // key에서 동아리 객체 반환
    protected Club getClub(String keyId) {
        Integer clubId = getOriginalId(keyId, InviteType.CLUB);
        return clubRepository.findByIdAndDeletedAtIsNull(clubId)
                .orElseThrow(() -> new ClubNotFoundException("동아리가 존재하지 않습니다"));
    }

    /// 팀 관련
    // 팀 존재 확인
    public void isExistTeam(Integer teamId) {
        teamRepository.findByIdAndDeletedAtIsNull(teamId)
                .orElseThrow(()-> new TeamNotFoundException("팀이 존재하지 않습니다"));
    }

    // 팀원인지 확인
    public boolean isMemberOfTeam(Integer teamId, Integer userId) {
        Optional<TeamMember> Optional = teamMemberRepository.findByTeamIdAndUserIdAndDeletedAtIsNull(teamId, userId);
        return Optional.isPresent();
    }

    // key에서 팀 객체 반환
    public Team getTeam(String keyId) {
        Integer teamId = getOriginalId(keyId, InviteType.TEAM);
        return teamRepository.findByIdAndDeletedAtIsNull(teamId)
                .orElseThrow(() -> new TeamNotFoundException("팀이 존재하지 않습니다"));
    }

    /// 기타
    // key에서 id 추출
    private Integer getOriginalId(String keyId, InviteType inviteType) {
        String[] splitKey = keyId.split(":");
        try {
            if(splitKey.length != 2) {
                throw new IllegalArgumentException("잘못된 KeyId 형식: " + keyId);
            }
            if(!splitKey[0].equals(inviteType.name())) {
                throw new IllegalArgumentException("잘못된 초대 타입: " + splitKey[0]);
            }
            if(!splitKey[1].chars().allMatch(Character::isDigit)) {
                throw new IllegalArgumentException("잘못된 ID 형식: " + splitKey[1]);
            }
            return Integer.parseInt(splitKey[1]);
        }catch (Exception e){
            throw new IllegalArgumentException("잘못된 KeyId 형식: " + keyId);
        }
    }
}
