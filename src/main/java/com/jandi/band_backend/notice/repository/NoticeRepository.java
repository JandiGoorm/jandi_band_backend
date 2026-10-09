package com.jandi.band_backend.notice.repository;

import com.jandi.band_backend.notice.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM Notice n WHERE n.id = :id AND n.deletedAt IS NULL")
    Optional<Notice> findForUpdate(@Param("id") Integer id);

    // 전체 목록 조회 (소프트 삭제되지 않은 것만)
    @Query("SELECT n FROM Notice n LEFT JOIN FETCH n.creator WHERE n.deletedAt IS NULL ORDER BY n.createdAt DESC")
    Page<Notice> findAllByDeletedAtIsNullOrderByCreatedAtDesc(Pageable pageable);

    // 단건 조회 (소프트 삭제되지 않은 것만)
    @Query("SELECT n FROM Notice n LEFT JOIN FETCH n.creator WHERE n.id = :id AND n.deletedAt IS NULL")
    Optional<Notice> findByIdAndDeletedAtIsNull(@Param("id") Integer id);

    // 현재 활성화된 공지사항들 조회 (팝업용)
    @Query("SELECT n FROM Notice n LEFT JOIN FETCH n.creator WHERE n.deletedAt IS NULL " +
           "AND n.isPaused = false " +
           "AND n.startDatetime <= :now " +
           "AND n.endDatetime >= :now " +
           "ORDER BY n.createdAt DESC")
    List<Notice> findActiveNotices(@Param("now") LocalDateTime now);
}
