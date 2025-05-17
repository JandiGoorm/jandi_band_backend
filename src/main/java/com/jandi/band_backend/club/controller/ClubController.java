package com.jandi.band_backend.club.controller;

import com.jandi.band_backend.club.dto.ClubDto;
import com.jandi.band_backend.club.dto.PageResponseDto;
import com.jandi.band_backend.club.service.ClubService;
import com.jandi.band_backend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;

    /**
     * 동아리 추가 API
     * 사용자가 새로운 동아리를 생성합니다.
     */
    @PostMapping
    public ResponseEntity<ClubDto.Response> createClub(
            @Valid @RequestBody ClubDto.Request request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer userId = userDetails.getUserId();
        ClubDto.Response response = clubService.createClub(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 동아리 목록 조회 API
     * 페이지네이션을 지원하는 동아리 목록을 반환합니다.
     */
    @GetMapping
    public ResponseEntity<PageResponseDto<ClubDto.SimpleResponse>> getClubList(
            @PageableDefault(size = 5) Pageable pageable) {
        PageResponseDto<ClubDto.SimpleResponse> response = clubService.getClubList(pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * 동아리 상세 조회 API
     * 특정 동아리의 상세 정보를 조회합니다.
     */
    @GetMapping("/{clubId}")
    public ResponseEntity<ClubDto.Response> getClubDetail(
            @PathVariable Integer clubId) {
        ClubDto.Response response = clubService.getClubDetail(clubId);
        return ResponseEntity.ok(response);
    }

    /**
     * 동아리 정보 수정 API
     * 대표자가 동아리 정보를 수정합니다.
     */
    @PatchMapping("/{clubId}")
    public ResponseEntity<ClubDto.Response> updateClub(
            @PathVariable Integer clubId,
            @Valid @RequestBody ClubDto.UpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer userId = userDetails.getUserId();
        ClubDto.Response response = clubService.updateClub(clubId, request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 동아리 삭제 API
     * 대표자가 동아리를 삭제합니다.
     */
    @DeleteMapping("/{clubId}")
    public ResponseEntity<Void> deleteClub(
            @PathVariable Integer clubId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer userId = userDetails.getUserId();
        clubService.deleteClub(clubId, userId);
        return ResponseEntity.noContent().build();
    }
}
