package com.jandi.band_backend.club.repository;

import com.jandi.band_backend.club.entity.ClubEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import java.util.Optional;


@Repository
public interface ClubEventRepository extends JpaRepository<ClubEvent, Long> {
    List<ClubEvent> findByClubId(Long clubId);

    //    Optional<ClubEvent> findByIdAndClubId(Long eventId, Integer clubId);
    Optional<ClubEvent> findByIdAndClubIdAndDeletedAtIsNull(Long eventId, Integer clubId);

//    @Query("SELECT e FROM ClubEvent e WHERE e.club.id = :clubId AND " +
//            "(e.startDatetime <= :end AND e.endDatetime >= :start)")
//    List<ClubEvent> findByClubIdAndOverlappingDate(@Param("clubId") Long clubId,
//                                                   @Param("start") Instant start,
//                                                   @Param("end") Instant end);

//    @Query("SELECT e FROM ClubEvent e WHERE e.club.id = :clubId AND e.deletedAt IS NULL AND " +
//            "(e.startDatetime <= :end AND e.endDatetime >= :start)")
//    List<ClubEvent> findByClubIdAndOverlappingDate(@Param("clubId") Long clubId,
//                                                   @Param("start") Instant start,
//                                                   @Param("end") Instant end);

    @Query("SELECT e FROM ClubEvent e WHERE e.club.id = :clubId AND e.deletedAt IS NULL AND " +
            "(e.startDatetime <= :end AND e.endDatetime >= :start)")
    List<ClubEvent> findByClubIdAndOverlappingDate(
            @Param("clubId") Long clubId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

}