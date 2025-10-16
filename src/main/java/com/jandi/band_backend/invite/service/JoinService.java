package com.jandi.band_backend.invite.service;

import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.entity.ClubMember;
import com.jandi.band_backend.club.repository.ClubMemberRepository;
import com.jandi.band_backend.global.exception.BannedMemberJoinAttemptException;
import com.jandi.band_backend.global.exception.InvalidAccessException;
import com.jandi.band_backend.global.exception.UserNotFoundException;
import com.jandi.band_backend.invite.dto.JoinRespDTO;
import com.jandi.band_backend.invite.redis.InviteCodeService;
import com.jandi.band_backend.team.entity.Team;
import com.jandi.band_backend.team.entity.TeamMember;
import com.jandi.band_backend.team.repository.TeamMemberRepository;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JoinService {
    private final InviteCodeService inviteCodeService;
    private final InviteUtilService inviteUtilService;
    private final UserRepository userRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Transactional
    public JoinRespDTO joinClub(Integer userId, String code) {
        String keyId = inviteCodeService.getKeyId(code);
        Club club = inviteUtilService.getClub(keyId);

        if(inviteUtilService.isMemberOfClub(club.getId(), userId)) {
            throw new InvalidAccessException("이미 가입한 동아리입니다");
        }

        Users user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        createNewClubMember(user, club);
        return new JoinRespDTO(club.getId(), null);
    }

    @Transactional
    public JoinRespDTO joinTeam(Integer userId, String code) {
        String keyId = inviteCodeService.getKeyId(code);
        Team team = inviteUtilService.getTeam(keyId);
        Club club = team.getClub();

        if(inviteUtilService.isMemberOfTeam(team.getId(), userId)) {
            throw new InvalidAccessException("이미 가입한 팀입니다");
        }

        // 팀 초대 + 동아리원이 아니라면 동아리원 초대도 자동으로 진행
        Users user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        if(!inviteUtilService.isMemberOfClub(club.getId(), userId)) {
            createNewClubMember(user, club);
        }
        createNewTeamMember(user, team);
        return new JoinRespDTO(club.getId(), team.getId());
    }

    private void createNewClubMember(Users user, Club club) {
        // 기존 회원 여부 확인 (deleted_at 상태와 관계없이)
        Optional<ClubMember> existingMember = clubMemberRepository.findByClubIdAndUserId(club.getId(), user.getId());

        if (existingMember.isPresent()) {
            ClubMember clubMember = existingMember.get();

            if (clubMember.getRole() == ClubMember.MemberRole.BANNED) {
                throw new BannedMemberJoinAttemptException("강퇴된 사용자는 해당 동아리에 재가입할 수 없습니다.");
            }

            if (clubMember.getDeletedAt() != null) {
                // 소프트 삭제된 상태라면 재활성화
                clubMember.setDeletedAt(null);
                clubMember.setRole(ClubMember.MemberRole.MEMBER);
                clubMember.setUpdatedAt(LocalDateTime.now());
                clubMemberRepository.save(clubMember);
            }
        } else {
            ClubMember clubMember = new ClubMember();
            clubMember.setClub(club);
            clubMember.setUser(user);
            clubMemberRepository.save(clubMember);
        }
    }

    private void createNewTeamMember(Users user, Team team) {
        Optional<TeamMember> existingMember = teamMemberRepository.findByTeamIdAndUserId(team.getId(), user.getId());

        if (existingMember.isPresent()) {
            TeamMember teamMember = existingMember.get();

            if (teamMember.getDeletedAt() != null) {
                teamMember.setDeletedAt(null);
                teamMemberRepository.save(teamMember);
            }
        } else {
            TeamMember teamMember = new TeamMember();
            teamMember.setTeam(team);
            teamMember.setUser(user);
            teamMemberRepository.save(teamMember);
        }
    }
}
