package com.jandi.band_backend.promo.repository;

import com.jandi.band_backend.promo.entity.Promo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Repository
public interface PromoRepository extends JpaRepository<Promo, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Promo p WHERE p.id = :id AND p.deletedAt IS NULL")
    Promo findForUpdate(@Param("id") Integer id);
    
    @Query("SELECT p FROM Promo p LEFT JOIN FETCH p.photos WHERE p.deletedAt IS NULL")
    Page<Promo> findAllNotDeleted(Pageable pageable);
    
    @Query("SELECT p FROM Promo p WHERE p.deletedAt IS NULL AND p.club.id = :clubId")
    Page<Promo> findAllByClubId(@Param("clubId") Integer clubId, Pageable pageable);

    @EntityGraph(attributePaths = {"photos"})
    @Query("""
    SELECT p FROM Promo p
    WHERE p.deletedAt IS NULL
    ORDER BY
        CASE
            WHEN FUNCTION('DATE', p.eventDatetime) = CURRENT_DATE THEN 0
            WHEN FUNCTION('DATE', p.eventDatetime) > CURRENT_DATE THEN 1
            ELSE 2
        END,
        p.eventDatetime ASC
    """)
    Page<Promo> findAllSortedByEventDatetime(Pageable pageable);

    @Query("SELECT p FROM Promo p WHERE p.deletedAt IS NULL AND function('DATE', p.eventDatetime ) > :today ORDER BY p.eventDatetime ASC")
    Page<Promo> findUpcomingPromos(@Param("today") LocalDate today, Pageable pageable);
    
    @Query("SELECT p FROM Promo p WHERE p.deletedAt IS NULL AND function('DATE', p.eventDatetime ) = :today ORDER BY p.eventDatetime ASC")
    Page<Promo> findOngoingPromos(@Param("today") LocalDate today, Pageable pageable);

    @Query("SELECT p FROM Promo p WHERE p.deletedAt IS NULL AND function('DATE', p.eventDatetime ) < :today ORDER BY p.eventDatetime DESC")
    Page<Promo> findEndedPromos(@Param("today") LocalDate today, Pageable pageable);
    
    @Query("SELECT p FROM Promo p WHERE p.deletedAt IS NULL AND p.creator.id = :userId")
    Page<Promo> findAllByCreatorId(@Param("userId") Integer userId, Pageable pageable);
    
    @Query("SELECT p FROM Promo p LEFT JOIN FETCH p.photos WHERE p.deletedAt IS NULL AND p.id = :id")
    Promo findByIdAndNotDeleted(@Param("id") Integer id);

    // 키워드 검색
    @Query("SELECT p FROM Promo p WHERE p.deletedAt IS NULL AND " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.location) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(p.teamName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Promo> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 필터링 (팀명과 클럽ID 모두 지원)
    @Query("SELECT p FROM Promo p WHERE p.deletedAt IS NULL " +
           "AND (:startDate IS NULL OR p.eventDatetime >= :startDate) " +
           "AND (:endDate IS NULL OR p.eventDatetime <= :endDate) " +
           "AND (:teamName IS NULL OR LOWER(p.teamName) LIKE LOWER(CONCAT('%', :teamName, '%'))) " +
           "AND (:clubId IS NULL OR p.club.id = :clubId)")
    Page<Promo> filterPromos(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("teamName") String teamName,
        @Param("clubId") Integer clubId,
        Pageable pageable);

    // 팀명만으로 필터링 (기존 호환성)
    @Query("SELECT p FROM Promo p WHERE p.deletedAt IS NULL " +
           "AND (:startDate IS NULL OR p.eventDatetime >= :startDate) " +
           "AND (:endDate IS NULL OR p.eventDatetime <= :endDate) " +
           "AND (:teamName IS NULL OR LOWER(p.teamName) LIKE LOWER(CONCAT('%', :teamName, '%')))")
    Page<Promo> filterPromosByTeamName(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("teamName") String teamName,
        Pageable pageable);

    // 특정 지역 범위에 속한 것만 필터링
    @Query("SELECT p FROM Promo p " +
            "WHERE p.latitude BETWEEN :minLat AND :maxLat " +
            "AND p.longitude BETWEEN :minLng AND :maxLng " +
            "AND p.deletedAt IS NULL")
    Page<Promo> filterPromosInSpecArea(
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLng") BigDecimal minLng,
            @Param("maxLng") BigDecimal maxLng,
            Pageable pageable
    );

    @Modifying
    @Query(value = "UPDATE promo SET creator_user_id = -1 WHERE creator_user_id = :userId", nativeQuery = true)
    int anonymizeByCreatorId(@Param("userId") Integer userId);

    @Modifying
    @Query("UPDATE Promo p SET p.likeCount = p.likeCount - 1 WHERE p.id = :promoId AND p.likeCount > 0")
    void decrementLikeCount(@Param("promoId") Integer promoId);

    // 상태별 필터링 + 키워드/팀명 조건
    @Query("SELECT p FROM Promo p WHERE p.deletedAt IS NULL " +
           "AND (:status = 'ongoing' AND p.eventDatetime = :now) " +
           "OR (:status = 'upcoming' AND p.eventDatetime > :now) " +
           "OR (:status = 'ended' AND p.eventDatetime < :now) " +
           "AND (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.location) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(p.teamName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND (:teamName IS NULL OR LOWER(p.teamName) LIKE LOWER(CONCAT('%', :teamName, '%'))) " +
           "ORDER BY p.eventDatetime ASC")
    Page<Promo> filterPromosByStatusAndConditions(
        @Param("status") String status,
        @Param("keyword") String keyword,
        @Param("teamName") String teamName,
        @Param("now") LocalDateTime now,
        Pageable pageable);
}
