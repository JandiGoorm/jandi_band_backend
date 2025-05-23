package com.jandi.band_backend.club.repository;

import com.jandi.band_backend.club.entity.Club;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubRepository extends JpaRepository<Club, Integer> {
    List<Club> findAll();
    Optional<Club> findById(Integer clubId);
    // 소프트 삭제되지 않은 동아리를 조회하는 메서드
    List<Club> findAllByDeletedAtIsNull();
    Page<Club> findAllByDeletedAtIsNull(Pageable pageable);
    Optional<Club> findByIdAndDeletedAtIsNull(Integer clubId);
}
