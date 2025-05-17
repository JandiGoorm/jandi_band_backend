package com.jandi.band_backend.club.repository;

import com.jandi.band_backend.club.entity.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubRepository extends JpaRepository<Club, Integer> {
    List<Club> findAll();
    Optional<Club> findById(Integer clubId);
}
