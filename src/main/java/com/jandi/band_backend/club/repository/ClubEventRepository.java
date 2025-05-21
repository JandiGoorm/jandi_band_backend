package com.jandi.band_backend.club.repository;

import com.jandi.band_backend.club.entity.ClubEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ClubEventRepository extends JpaRepository<ClubEvent, Long> {
    List<ClubEvent> findByClubId(Long clubId);
}