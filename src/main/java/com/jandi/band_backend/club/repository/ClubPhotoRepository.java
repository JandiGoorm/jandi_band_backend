package com.jandi.band_backend.club.repository;

import com.jandi.band_backend.club.entity.ClubPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubPhotoRepository extends JpaRepository<ClubPhoto, Integer> {
    List<ClubPhoto> findByClubId(Integer clubId);
    Optional<ClubPhoto> findByClubIdAndDeletedAtIsNull(Integer clubId);
}
