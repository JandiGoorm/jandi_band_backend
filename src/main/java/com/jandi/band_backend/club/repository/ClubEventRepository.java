package com.jandi.band_backend.club.repository;

import com.jandi.band_backend.club.entity.ClubEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClubEventRepository extends JpaRepository<ClubEvent, Long> {
    // 특정 동아리의 모든 삭제되지 않은 이벤트 조회
    List<ClubEvent> findByClubIdAndDeletedAtIsNull(Integer clubId);

    // 특정 동아리의 특정 이벤트를 ID로 조회
    Optional<ClubEvent> findByIdAndClubIdAndDeletedAtIsNull(Integer id, Integer clubId);

    @Query("SELECT e FROM ClubEvent e WHERE e.club.id = :clubId AND e.deletedAt IS NULL AND " +
            "(e.startDatetime <= :end AND e.endDatetime >= :start)")
    List<ClubEvent> findByClubIdAndOverlappingDate(
            @Param("clubId") Integer clubId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
