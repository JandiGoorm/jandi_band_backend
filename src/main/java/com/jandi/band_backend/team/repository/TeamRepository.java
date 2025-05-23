package com.jandi.band_backend.team.repository;

import com.jandi.band_backend.team.entity.Team;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Integer> {
    
    @Query("SELECT t FROM Team t WHERE t.deletedAt IS NULL AND t.id = :id")
    Optional<Team> findByIdAndNotDeleted(@Param("id") Integer id);
    
    @Query("SELECT t FROM Team t WHERE t.deletedAt IS NULL AND t.club.id = :clubId ORDER BY t.createdAt DESC")
    Page<Team> findAllByClubId(@Param("clubId") Integer clubId, Pageable pageable);
} 