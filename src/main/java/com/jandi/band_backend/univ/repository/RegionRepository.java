package com.jandi.band_backend.univ.repository;

import com.jandi.band_backend.univ.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface RegionRepository extends JpaRepository<Region, Integer> {
    @Query("SELECT r.code FROM Region r")
    Set<String> findAllRegionCodes();

    Region findByCode(String region);
}
