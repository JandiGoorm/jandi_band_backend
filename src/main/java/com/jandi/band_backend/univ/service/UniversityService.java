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

    /// 모든 대학 리스트 조회
    // 기본적으로 전체 조회이나, filter 값에 따라 필터링 조회도 가능
    @Transactional(readOnly = true)
    public List<UniversityRespDTO> getAllUniversity(String filter, String type, String region) {
        UnivFilter enumFilter = UnivFilter.valueOf(filter.toUpperCase()); // ← enum 변환
        List<University> univList = switch (enumFilter) {
            case ALL -> universityRepository.findAll();
            case TYPE -> getUniversityByType(type);
            case REGION -> getUniversityByRegion(region);
            default -> throw new IllegalArgumentException("잘못된 filter: " + filter);
        };
        return univList.stream().map(UniversityRespDTO::new).collect(Collectors.toList());
    }

    /// 특정 대학 상세 조회
    @Transactional(readOnly = true)
    public UniversityDetailRespDTO getUniversityById(Integer id) {
        University university = universityRepository.findById(id)
                .orElseThrow(() -> new UniversityNotFoundException("대학 정보가 없습니다"));
        return new UniversityDetailRespDTO(university);
    }

    /// 내부 메서드
    // 타입 필터: 대학 종류(ex. 대학교, 대학원)에 따른 필터링 조회
    private List<University> getUniversityByType(String type) {
        UnivType univType = UnivType.from(type);
        return universityRepository.findByNameContains(univType.getKeyword());
    }

    // 지역 필터: 대학 소재지(ex. 서울, 강원)에 따른 필터링 조회
    private List<University> getUniversityByRegion(String region) {
        Set<String> regionCodes = regionRepository.findAllRegionCodes();
        if (!regionCodes.contains(region))
            throw new IllegalArgumentException("잘못된 지역명: " + region);
        return universityRepository.findByRegion_Code(region);
    }
}