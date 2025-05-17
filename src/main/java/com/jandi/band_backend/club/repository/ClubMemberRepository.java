package com.jandi.band_backend.club.repository;

import com.jandi.band_backend.club.entity.ClubMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubMemberRepository extends JpaRepository<ClubMember, Integer> {
    List<ClubMember> findByClubId(Integer clubId);
    Optional<ClubMember> findByClubIdAndUserId(Integer clubId, Integer userId);
    void deleteByClubIdAndUserId(Integer clubId, Integer userId);
}
