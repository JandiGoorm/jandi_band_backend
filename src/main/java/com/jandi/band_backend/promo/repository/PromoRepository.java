package com.jandi.band_backend.promo.repository;

import com.jandi.band_backend.promo.entity.Promo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PromoRepository extends JpaRepository<Promo, Integer> {
    
    @Query("SELECT p FROM Promo p WHERE p.deletedAt IS NULL")
    Page<Promo> findAllNotDeleted(Pageable pageable);
    
    @Query("SELECT p FROM Promo p WHERE p.deletedAt IS NULL AND p.club.id = :clubId")
    Page<Promo> findAllByClubId(@Param("clubId") Integer clubId, Pageable pageable);
    
    @Query("SELECT p FROM Promo p WHERE p.deletedAt IS NULL AND p.eventDatetime >= :now ORDER BY p.eventDatetime ASC")
    Page<Promo> findUpcomingPromos(@Param("now") LocalDateTime now, Pageable pageable);
    
    @Query("SELECT p FROM Promo p WHERE p.deletedAt IS NULL AND p.creator.id = :userId")
    Page<Promo> findAllByCreatorId(@Param("userId") Integer userId, Pageable pageable);
    
    @Query("SELECT p FROM Promo p WHERE p.deletedAt IS NULL AND p.id = :id")
    Promo findByIdAndNotDeleted(@Param("id") Integer id);

    // 상태와 시간 기준으로 공연 조회
    @Query("SELECT p FROM Promo p WHERE p.deletedAt IS NULL AND p.status = :status AND p.eventDatetime <= :datetime")
    List<Promo> findByStatusAndEventDatetimeBefore(
        @Param("status") Promo.PromoStatus status,
        @Param("datetime") LocalDateTime datetime);

    // 키워드 검색
    @Query("SELECT p FROM Promo p WHERE p.deletedAt IS NULL AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.location) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Promo> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 필터링
    @Query("SELECT p FROM Promo p WHERE p.deletedAt IS NULL " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:startDate IS NULL OR p.eventDatetime >= :startDate) " +
           "AND (:endDate IS NULL OR p.eventDatetime <= :endDate) " +
           "AND (:clubId IS NULL OR p.club.id = :clubId)")
    Page<Promo> filterPromos(
        @Param("status") Promo.PromoStatus status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("clubId") Integer clubId,
        Pageable pageable);
} 