package com.jandi.band_backend.univ.service;

import com.jandi.band_backend.univ.dto.RegionRespDTO;
import com.jandi.band_backend.univ.entity.Region;
import com.jandi.band_backend.univ.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RegionService {
    private final RegionRepository regionRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "regions", key = "'all'")
    public List<RegionRespDTO> getAllRegions() {
        List<Region> regionList = regionRepository.findAll();
        return regionList.stream()
                .map(RegionRespDTO::new).collect(Collectors.toList());
    }
}
