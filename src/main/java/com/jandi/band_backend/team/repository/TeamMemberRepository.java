package com.jandi.band_backend.team.repository;

import com.jandi.band_backend.team.entity.TeamMember;
import com.jandi.band_backend.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Integer> {
    // 사용자 ID로 팀 멤버 조회
    List<TeamMember> findByUserIdAndTeamDeletedAtIsNullAndDeletedAtIsNullOrderByJoinedAtDesc(Integer userId);
    // 팀 ID로 멤버 조회
    List<TeamMember> findByTeamIdAndDeletedAtIsNull(Integer teamId);
    // 팀 ID로 멤버 수 조회
    Integer countByTeamIdAndDeletedAtIsNull(Integer teamId);
    // 팀 ID와 사용자 ID로 멤버 조회
    Optional<TeamMember> findByTeamIdAndUserIdAndDeletedAtIsNull(Integer teamId, Integer userId);

    @Modifying
    @Query("UPDATE TeamMember tm SET tm.deletedAt = :deletedAt WHERE tm.user.id = :userId AND tm.deletedAt IS NULL")
    int softDeleteByUserId(@Param("userId") Integer userId, @Param("deletedAt") LocalDateTime deletedAt);

    // deleted_at 상태와 관계없이 팀 ID와 사용자 ID로 멤버 조회 (재가입 처리용)
    Optional<TeamMember> findByTeamIdAndUserId(Integer teamId, Integer userId);

    int deleteAllByUser(Users user);
}