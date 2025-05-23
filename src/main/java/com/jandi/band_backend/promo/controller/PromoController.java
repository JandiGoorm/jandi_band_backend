package com.jandi.band_backend.promo.controller;

import com.jandi.band_backend.global.ApiResponse;
import com.jandi.band_backend.promo.dto.PromoRequest;
import com.jandi.band_backend.promo.dto.PromoResponse;
import com.jandi.band_backend.promo.entity.Promo;
import com.jandi.band_backend.promo.service.PromoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/promos")
@RequiredArgsConstructor
public class PromoController {

    private final PromoService promoService;

    // 공연 홍보 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PromoResponse>>> getPromos(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("공연 홍보 목록 조회 성공", promoService.getPromos(pageable)));
    }

    // 클럽별 공연 홍보 목록 조회
    @GetMapping("/club/{clubId}")
    public ResponseEntity<ApiResponse<Page<PromoResponse>>> getPromosByClub(
            @PathVariable Integer clubId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("클럽별 공연 홍보 목록 조회 성공", 
                promoService.getPromosByClub(clubId, pageable)));
    }

    // 공연 홍보 상세 조회
    @GetMapping("/{promoId}")
    public ResponseEntity<ApiResponse<PromoResponse>> getPromo(@PathVariable Integer promoId) {
        return ResponseEntity.ok(ApiResponse.success("공연 홍보 상세 조회 성공", 
                promoService.getPromo(promoId)));
    }

    // 공연 홍보 생성
    @PostMapping
    public ResponseEntity<ApiResponse<PromoResponse>> createPromo(
            @Valid @RequestBody PromoRequest request,
            @RequestAttribute("userId") Integer userId) {
        return ResponseEntity.ok(ApiResponse.success("공연 홍보 생성 성공", 
                promoService.createPromo(request, userId)));
    }

    // 공연 홍보 수정
    @PutMapping("/{promoId}")
    public ResponseEntity<ApiResponse<PromoResponse>> updatePromo(
            @PathVariable Integer promoId,
            @Valid @RequestBody PromoRequest request,
            @RequestAttribute("userId") Integer userId) {
        return ResponseEntity.ok(ApiResponse.success("공연 홍보 수정 성공", 
                promoService.updatePromo(promoId, request, userId)));
    }

    // 공연 홍보 삭제
    @DeleteMapping("/{promoId}")
    public ResponseEntity<ApiResponse<Void>> deletePromo(
            @PathVariable Integer promoId,
            @RequestAttribute("userId") Integer userId) {
        promoService.deletePromo(promoId, userId);
        return ResponseEntity.ok(ApiResponse.success("공연 홍보 삭제 성공"));
    }

    // 공연 홍보 이미지 업로드
    @PostMapping(value = "/{promoId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadPromoImage(
            @PathVariable Integer promoId,
            @RequestParam("image") MultipartFile image,
            @RequestAttribute("userId") Integer userId) throws IOException {
        String imageUrl = promoService.uploadPromoImage(promoId, image, userId);
        return ResponseEntity.ok(ApiResponse.success("공연 홍보 이미지 업로드 성공", imageUrl));
    }

    // 공연 홍보 이미지 삭제
    @DeleteMapping("/{promoId}/images")
    public ResponseEntity<ApiResponse<Void>> deletePromoImage(
            @PathVariable Integer promoId,
            @RequestParam String imageUrl,
            @RequestAttribute("userId") Integer userId) {
        promoService.deletePromoImage(promoId, imageUrl, userId);
        return ResponseEntity.ok(ApiResponse.success("공연 홍보 이미지 삭제 성공"));
    }

    // 공연 홍보 검색
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<PromoResponse>>> searchPromos(
            @RequestParam String keyword,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("공연 홍보 검색 성공", 
                promoService.searchPromos(keyword, pageable)));
    }

    // 공연 홍보 필터링
    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<Page<PromoResponse>>> filterPromos(
            @RequestParam(required = false) Promo.PromoStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Integer clubId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("공연 홍보 필터링 성공", 
                promoService.filterPromos(status, startDate, endDate, clubId, pageable)));
    }
} 