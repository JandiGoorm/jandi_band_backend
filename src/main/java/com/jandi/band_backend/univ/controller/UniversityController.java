package com.jandi.band_backend.univ.controller;

import com.jandi.band_backend.global.ApiResponse;
import com.jandi.band_backend.univ.dto.UniversityDetailRespDTO;
import com.jandi.band_backend.univ.dto.UniversityRespDTO;
import com.jandi.band_backend.univ.service.UniversityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/univ")
@RequiredArgsConstructor
public class UniversityController {
    private final UniversityService universityService;

    @GetMapping("/all")
    public ApiResponse<List<UniversityRespDTO>> getAllUniversity(
            @RequestParam(defaultValue = "ALL") String filter, // 필터링 범위
            @RequestParam(required = false) String type, // 대학 유형
            @RequestParam(required = false) String region // 대학 소재지
    ) {
        List<UniversityRespDTO> univList = universityService.getAllUniversity(filter, type, region);
        return ApiResponse.success("대학 정보 조회 성공", univList);
    }

    @GetMapping("/{univId}")
    public ApiResponse<UniversityDetailRespDTO> getUniversityById(
            @PathVariable Integer univId
    ) {
        UniversityDetailRespDTO universityById = universityService.getUniversityById(univId);
        return ApiResponse.success("대학 상세 정보 조회 성공", universityById);
    }
}
