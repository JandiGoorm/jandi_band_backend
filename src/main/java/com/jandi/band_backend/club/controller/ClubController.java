package com.jandi.band_backend.club.controller;

import com.jandi.band_backend.club.dto.ClubReqDTO;
import com.jandi.band_backend.club.dto.ClubDetailRespDTO;
import com.jandi.band_backend.club.dto.ClubRespDTO;
import com.jandi.band_backend.club.dto.ClubUpdateReqDTO;
import com.jandi.band_backend.club.dto.ClubMembersRespDTO;
import com.jandi.band_backend.club.dto.TransferRepresentativeReqDTO;
import com.jandi.band_backend.club.service.ClubService;
import com.jandi.band_backend.global.dto.CommonRespDTO;
import com.jandi.band_backend.global.dto.PagedRespDTO;
import com.jandi.band_backend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

@Tag(name = "Club API")
@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubController {

    private final ClubService clubService;

    @Operation(summary = "동아리 생성")
    @PostMapping
    public ResponseEntity<CommonRespDTO<ClubDetailRespDTO>> createClub(
            @Valid @RequestBody ClubReqDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer userId = userDetails.getUserId();
        ClubDetailRespDTO response = clubService.createClub(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonRespDTO.success("동아리가 성공적으로 생성되었습니다", response));
    }

    @Operation(summary = "동아리 목록 조회")
    @GetMapping
    public ResponseEntity<CommonRespDTO<PagedRespDTO<ClubRespDTO>>> getClubList(
            @PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ClubRespDTO> response = clubService.getClubList(pageable);
        return ResponseEntity.ok(CommonRespDTO.success("동아리 목록 조회 성공", PagedRespDTO.from(response)));
    }

    @Operation(summary = "동아리 상세 조회")
    @GetMapping("/{clubId}")
    public ResponseEntity<CommonRespDTO<ClubDetailRespDTO>> getClubDetail(@PathVariable Integer clubId) {
        ClubDetailRespDTO response = clubService.getClubDetail(clubId);
        return ResponseEntity.ok(CommonRespDTO.success("동아리 상세 정보 조회 성공", response));
    }

    @Operation(summary = "동아리 부원 명단 조회")
    @GetMapping("/{clubId}/members")
    public ResponseEntity<CommonRespDTO<ClubMembersRespDTO>> getClubMembers(@PathVariable Integer clubId) {
        ClubMembersRespDTO response = clubService.getClubMembers(clubId);
        return ResponseEntity.ok(CommonRespDTO.success("동아리 부원 명단 조회 성공", response));
    }

    @Operation(summary = "동아리 정보 수정")
    @PatchMapping("/{clubId}")
    public ResponseEntity<CommonRespDTO<ClubDetailRespDTO>> updateClub(
            @PathVariable Integer clubId,
            @Valid @RequestBody ClubUpdateReqDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer userId = userDetails.getUserId();
        ClubDetailRespDTO response = clubService.updateClub(clubId, request, userId);
        return ResponseEntity.ok(CommonRespDTO.success("동아리 정보가 성공적으로 수정되었습니다", response));
    }

    @Operation(summary = "동아리 대표자 위임")
    @PatchMapping("/{clubId}/representative")
    public ResponseEntity<CommonRespDTO<Void>> transferRepresentative(
            @PathVariable Integer clubId,
            @Valid @RequestBody TransferRepresentativeReqDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer userId = userDetails.getUserId();
        clubService.transferRepresentative(clubId, userId, request.getNewRepresentativeUserId());
        return ResponseEntity.ok(CommonRespDTO.success("동아리 대표자 권한이 성공적으로 위임되었습니다"));
    }

    @Operation(summary = "동아리 삭제")
    @DeleteMapping("/{clubId}")
    public ResponseEntity<CommonRespDTO<Void>> deleteClub(
            @PathVariable Integer clubId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer userId = userDetails.getUserId();
        clubService.deleteClub(clubId, userId);
        return ResponseEntity.ok(CommonRespDTO.success("동아리가 성공적으로 삭제되었습니다"));
    }

    @Operation(summary = "동아리 탈퇴")
    @DeleteMapping("/{clubId}/members/me")
    public ResponseEntity<CommonRespDTO<Void>> leaveClub(
            @PathVariable Integer clubId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer userId = userDetails.getUserId();
        clubService.leaveClub(clubId, userId);
        return ResponseEntity.ok(CommonRespDTO.success("동아리에서 성공적으로 탈퇴했습니다"));
    }

    @Operation(summary = "동아리 부원 강퇴")
    @DeleteMapping("/{clubId}/members/{userId}")
    public ResponseEntity<CommonRespDTO<Void>> kickMember(
            @PathVariable Integer clubId,
            @PathVariable Integer userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer currentUserId = userDetails.getUserId();
        clubService.kickMember(clubId, currentUserId, userId);
        return ResponseEntity.ok(CommonRespDTO.success("해당 부원이 성공적으로 강퇴되었습니다"));
    }

    @Operation(summary = "동아리 대표 사진 업로드")
    @PostMapping(value = "/{clubId}/main-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommonRespDTO<String>> uploadClubPhoto(
            @PathVariable Integer clubId,
            @RequestParam("image") MultipartFile image,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer userId = userDetails.getUserId();
        String imageUrl = clubService.uploadClubPhoto(clubId, image, userId);
        return ResponseEntity.ok(CommonRespDTO.success("동아리 대표 사진이 성공적으로 업로드되었습니다", imageUrl));
    }

    @Operation(summary = "동아리 대표 사진 삭제")
    @DeleteMapping("/{clubId}/main-image")
    public ResponseEntity<CommonRespDTO<Void>> deleteClubPhoto(
            @PathVariable Integer clubId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer userId = userDetails.getUserId();
        clubService.deleteClubPhoto(clubId, userId);
        return ResponseEntity.ok(CommonRespDTO.success("동아리 대표 사진이 성공적으로 삭제되었습니다"));
    }
}
