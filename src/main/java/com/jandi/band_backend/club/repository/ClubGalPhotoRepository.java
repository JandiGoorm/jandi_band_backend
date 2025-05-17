package com.jandi.band_backend.club.repository;

import com.jandi.band_backend.club.entity.ClubGalPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClubGalPhotoRepository extends JpaRepository<ClubGalPhoto, Integer> {
    List<ClubGalPhoto> findByClubId(Integer clubId);
    List<ClubGalPhoto> findByClubIdAndIsPublic(Integer clubId, boolean isPublic);
    List<ClubGalPhoto> findByClubIdAndIsPinned(Integer clubId, boolean isPinned);
}
