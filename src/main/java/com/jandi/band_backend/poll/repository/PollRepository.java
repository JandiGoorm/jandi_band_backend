package com.jandi.band_backend.poll.repository;

import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.poll.entity.Poll;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PollRepository extends JpaRepository<Poll, Integer> {
    Page<Poll> findAllByClubAndDeletedAtIsNullOrderByCreatedAtDesc(Club club, Pageable pageable);

    Page<Poll> findAllByClubAndEndDatetimeAfterAndDeletedAtIsNullOrderByEndDatetimeAsc(Club club, Instant now, Pageable pageable);

    Optional<Poll> findByIdAndDeletedAtIsNull(Integer id);
}
