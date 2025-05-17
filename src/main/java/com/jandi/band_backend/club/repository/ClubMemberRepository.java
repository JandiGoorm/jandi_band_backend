package com.jandi.band_backend.club.repository;

import com.jandi.band_backend.club.entity.ClubMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubMemberRepository extends JpaRepository<ClubMember, Integer> {
    List<ClubMember> findByClubId(Integer clubId);
    Optional<ClubMember> findByClubIdAndUserId(Integer clubId, Integer userId);
    void deleteByClubIdAndUserId(Integer clubId, Integer userId);

    /**
     * 특정 동아리의 회원 수를 조회하는 메서드
     */
    @Query("SELECT COUNT(cm) FROM ClubMember cm WHERE cm.club.id = :clubId")
    int countByClubId(@Param("clubId") Integer clubId);
}
