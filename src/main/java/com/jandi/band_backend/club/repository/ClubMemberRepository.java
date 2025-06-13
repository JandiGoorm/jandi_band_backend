package com.jandi.band_backend.club.repository;

import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.entity.ClubMember;
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
public interface ClubMemberRepository extends JpaRepository<ClubMember, Integer> {
    List<ClubMember> findByClubIdAndDeletedAtIsNull(Integer clubId);
    Optional<ClubMember> findByClubIdAndUserIdAndDeletedAtIsNull(Integer clubId, Integer userId);
    Integer countByClubIdAndDeletedAtIsNull(Integer clubId);
    boolean existsByClubAndUserAndDeletedAtIsNull(Club club, Users user);
    // 사용자 ID로 동아리 멤버 조회 (동아리와 멤버 모두 삭제되지 않은 것만)
    List<ClubMember> findByUserIdAndClubDeletedAtIsNullAndDeletedAtIsNullOrderByJoinedAtDesc(Integer userId);

    @Query("""
    SELECT cm.club.name
    FROM ClubMember cm
    WHERE cm.user.id = :userId
      AND cm.role = :role
      AND cm.deletedAt IS NULL
      AND cm.club.deletedAt IS NULL
""")
    List<String> findClubNamesByUserRole(@Param("userId") Integer userId, @Param("role") ClubMember.MemberRole role);

    Integer user(Users user);

    Boolean existsByUserIdAndClub_IdAndDeletedAtIsNullAndRole(Integer userId, Integer clubId, ClubMember.MemberRole memberRole);

    @Modifying
    @Query("UPDATE ClubMember cm SET cm.deletedAt = :deletedAt WHERE cm.user.id = :userId AND cm.deletedAt IS NULL")
    int softDeleteByUserId(@Param("userId") Integer userId, @Param("deletedAt") LocalDateTime deletedAt);

    // deleted_at 상태와 관계없이 동아리 ID와 사용자 ID로 멤버 조회 (재가입 처리용)
    Optional<ClubMember> findByClubIdAndUserId(Integer clubId, Integer userId);

    int deleteAllByUser(Users user);
}
