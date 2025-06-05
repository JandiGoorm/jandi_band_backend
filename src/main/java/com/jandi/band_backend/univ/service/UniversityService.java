package com.jandi.band_backend.univ.service;

import com.jandi.band_backend.global.exception.UniversityNotFoundException;
import com.jandi.band_backend.univ.dto.UniversityDetailRespDTO;
import com.jandi.band_backend.univ.dto.UniversityRespDTO;
import com.jandi.band_backend.univ.entity.University;
import com.jandi.band_backend.univ.enums.UnivFilter;
import com.jandi.band_backend.univ.enums.UnivType;
import com.jandi.band_backend.univ.repository.RegionRepository;
import com.jandi.band_backend.univ.repository.UniversityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UniversityService {
    private final RegionRepository regionRepository;
    private final UniversityRepository universityRepository;

    @Transactional(readOnly = true)
    @Cacheable(value = "universities", key = "#filter + '_' + (#type != null ? #type : 'null') + '_' + (#region != null ? #region : 'null')")
    public List<UniversityRespDTO> getAllUniversity(String filter, String type, String region) {
        UnivFilter enumFilter = UnivFilter.valueOf(filter.toUpperCase());
        List<University> univList = switch (enumFilter) {
            case ALL -> universityRepository.findAll();
            case TYPE -> getUniversityByType(type);
            case REGION -> getUniversityByRegion(region);
            default -> throw new IllegalArgumentException("잘못된 filter: " + filter);
        };
        return univList.stream().map(UniversityRespDTO::new).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "universities", key = "'detail_' + #id")
    public UniversityDetailRespDTO getUniversityById(Integer id) {
        University university = universityRepository.findById(id)
                .orElseThrow(() -> new UniversityNotFoundException("대학 정보가 없습니다"));
        return new UniversityDetailRespDTO(university);
    }

    private List<University> getUniversityByType(String type) {
        UnivType univType = UnivType.from(type);
        return universityRepository.findByNameContains(univType.getKeyword());
    }

    private List<University> getUniversityByRegion(String region) {
        Set<String> regionCodes = regionRepository.findAllRegionCodes();
        if (!regionCodes.contains(region))
            throw new IllegalArgumentException("잘못된 지역명: " + region);
        return universityRepository.findByRegion_Code(region);
    }
}
