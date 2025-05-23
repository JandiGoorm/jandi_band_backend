package com.jandi.band_backend.univ.repository;

import com.jandi.band_backend.univ.entity.University;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UniversityRepository extends JpaRepository<University, Integer> {
    University findByName(String university);

    List<University> findByNameContains(String name);

    List<University> findByRegion_Code(String region);
}
