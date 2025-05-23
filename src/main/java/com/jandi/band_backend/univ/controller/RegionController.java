package com.jandi.band_backend.univ.controller;

import com.jandi.band_backend.global.ApiResponse;
import com.jandi.band_backend.univ.dto.RegionRespDTO;
import com.jandi.band_backend.univ.service.RegionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/region")
@RequiredArgsConstructor
public class RegionController {
    private final RegionService regionService;

    /// 전체 지역 조회
    @GetMapping("/all")
    public ApiResponse<List<RegionRespDTO>> getAllRegion() {
        List<RegionRespDTO> regionList = regionService.getAllRegions();
        return ApiResponse.success("지역 리스트 조회 성공", regionList);
    }
}
