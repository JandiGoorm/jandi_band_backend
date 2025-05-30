package com.jandi.band_backend.club.repository;

import com.jandi.band_backend.club.entity.ClubEventParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubEventParticipantRepository extends JpaRepository<ClubEventParticipant, Integer> {

    // 특정 이벤트의 모든 참여자 조회 (삭제되지 않은 것만)
    List<ClubEventParticipant> findByClubEventIdAndDeletedAtIsNull(Integer clubEventId);

    // 특정 이벤트에 특정 사용자가 참여하고 있는지 확인
    Optional<ClubEventParticipant> findByClubEventIdAndUserIdAndDeletedAtIsNull(Integer clubEventId, Integer userId);

    // 특정 이벤트의 참여자 수 조회
    @Query("SELECT COUNT(cep) FROM ClubEventParticipant cep WHERE cep.clubEvent.id = :clubEventId AND cep.deletedAt IS NULL")
    Integer countByClubEventIdAndDeletedAtIsNull(@Param("clubEventId") Integer clubEventId);
}
