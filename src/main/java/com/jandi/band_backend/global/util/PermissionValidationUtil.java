package com.jandi.band_backend.global.util;

import com.jandi.band_backend.club.entity.ClubMember;
import com.jandi.band_backend.club.repository.ClubMemberRepository;
import com.jandi.band_backend.global.exception.UnauthorizedClubAccessException;
import com.jandi.band_backend.team.entity.Team;
import com.jandi.band_backend.team.entity.TeamMember;
import com.jandi.band_backend.team.repository.TeamMemberRepository;
import com.jandi.band_backend.user.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PermissionValidationUtil {
    private final ClubMemberRepository clubMemberRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserValidationUtil userValidationUtil;
    private final EntityValidationUtil entityValidationUtil;

    /**
     * ADMIN 권한 확인
     */
    private boolean isAdmin(Integer userId) {
        Users user = userValidationUtil.getUserById(userId);
        return user.getAdminRole() == Users.AdminRole.ADMIN;
    }

    /**
     * 동아리 멤버 권한 확인 (ADMIN은 항상 통과)
     */
    public void validateClubMemberAccess(Integer clubId, Integer userId, String errorMessage) {
        // ADMIN 권한이 있으면 바로 통과
        if (isAdmin(userId)) {
            return;
        }

        clubMemberRepository.findByClubIdAndUserIdAndDeletedAtIsNull(clubId, userId)
                .orElseThrow(() -> new UnauthorizedClubAccessException(errorMessage));
    }

    /**
     * 동아리 대표자 권한 확인 (ADMIN은 항상 통과)
     */
    public void validateClubRepresentativeAccess(Integer clubId, Integer userId, String errorMessage) {
        // ADMIN 권한이 있으면 바로 통과
        if (isAdmin(userId)) {
            return;
        }

        clubMemberRepository.findByClubIdAndUserIdAndDeletedAtIsNull(clubId, userId)
                .filter(member -> member.getRole() == ClubMember.MemberRole.REPRESENTATIVE)
                .orElseThrow(() -> new UnauthorizedClubAccessException(errorMessage));
    }

    /**
     * 팀 멤버 권한 확인 (ADMIN은 항상 통과)
     */
    public TeamMember validateTeamMemberAccess(Integer teamId, Integer userId, String errorMessage) {
        // ADMIN 권한이 있으면 임시 TeamMember 객체 반환 (Team 정보 포함)
        if (isAdmin(userId)) {
            TeamMember adminTeamMember = new TeamMember();
            Users adminUser = userValidationUtil.getUserById(userId);
            adminTeamMember.setUser(adminUser);
            
            // Team 엔티티 조회 및 설정 (team_id null 방지)
            Team team = entityValidationUtil.validateTeamExists(teamId);
            adminTeamMember.setTeam(team);
            
            return adminTeamMember;
        }

        return teamMemberRepository.findByTeamIdAndUserIdAndDeletedAtIsNull(teamId, userId)
                .orElseThrow(() -> new UnauthorizedClubAccessException(errorMessage));
    }

    /**
     * 컨텐츠 작성자 권한 확인 (일반적인 패턴, ADMIN은 항상 통과)
     */
    public void validateContentOwnership(Integer contentOwnerId, Integer userId, String errorMessage) {
        // ADMIN 권한이 있으면 바로 통과
        if (isAdmin(userId)) {
            return;
        }

        if (!contentOwnerId.equals(userId)) {
            throw new IllegalStateException(errorMessage);
        }
    }
}
