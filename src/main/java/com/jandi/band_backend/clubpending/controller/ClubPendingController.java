package com.jandi.band_backend.clubpending.controller;

import com.jandi.band_backend.clubpending.dto.ClubPendingListRespDTO;
import com.jandi.band_backend.clubpending.dto.ClubPendingProcessReqDTO;
import com.jandi.band_backend.clubpending.dto.ClubPendingRespDTO;
import com.jandi.band_backend.clubpending.service.ClubPendingService;
import com.jandi.band_backend.global.dto.CommonRespDTO;
import com.jandi.band_backend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "ClubPending API", description = "동아리 가입 신청 관리 API")
@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
@Validated
public class ClubPendingController {

    private final ClubPendingService clubPendingService;

    @Operation(summary = "동아리 가입 신청", description = "사용자가 동아리에 가입 신청을 합니다.")
    @PostMapping("/{clubId}/pendings")
    public ResponseEntity<CommonRespDTO<ClubPendingRespDTO>> applyToClub(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable @Positive Integer clubId) {

        ClubPendingRespDTO respDTO = clubPendingService.applyToClub(userDetails.getUserId(), clubId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonRespDTO.success("가입 신청이 완료되었습니다.", respDTO));
    }

    @Operation(summary = "동아리 대기 목록 조회", description = "동아리장이 대기중인 가입 신청 목록을 조회합니다.")
    @GetMapping("/{clubId}/pendings")
    public ResponseEntity<CommonRespDTO<ClubPendingListRespDTO>> getPendingListByClub(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable @Positive Integer clubId) {

        ClubPendingListRespDTO respDTO = clubPendingService.getPendingListByClub(clubId, userDetails.getUserId());
        return ResponseEntity.ok(CommonRespDTO.success("대기 목록 조회 성공", respDTO));
    }

    @Operation(summary = "특정 동아리에 대한 내 신청 조회", description = "사용자가 특정 동아리에 대한 본인의 가입 신청 상태를 조회합니다. 대기중인 신청이 없으면 null을 반환합니다.")
    @GetMapping("/{clubId}/pendings/my")
    public ResponseEntity<CommonRespDTO<ClubPendingRespDTO>> getMyPendingForClub(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable @Positive Integer clubId) {

        ClubPendingRespDTO respDTO = clubPendingService.getMyPendingForClub(clubId, userDetails.getUserId());
        String message = respDTO != null ? "신청 상태 조회 성공" : "대기중인 신청이 없습니다";
        return ResponseEntity.ok(CommonRespDTO.success(message, respDTO));
    }

    @Operation(summary = "가입 신청 승인/거부", description = "동아리장이 가입 신청을 승인하거나 거부합니다.")
    @PatchMapping("/pendings/{pendingId}")
    public ResponseEntity<CommonRespDTO<ClubPendingRespDTO>> processPending(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable @Positive Integer pendingId,
            @Valid @RequestBody ClubPendingProcessReqDTO reqDTO) {

        ClubPendingRespDTO respDTO = clubPendingService.processPending(pendingId, userDetails.getUserId(), reqDTO);
        String message = reqDTO.getApprove() ? "가입 신청이 승인되었습니다." : "가입 신청이 거부되었습니다.";
        return ResponseEntity.ok(CommonRespDTO.success(message, respDTO));
    }

    @Operation(summary = "가입 신청 취소", description = "사용자가 본인의 가입 신청을 취소합니다.")
    @DeleteMapping("/pendings/{pendingId}")
    public ResponseEntity<CommonRespDTO<Void>> cancelPending(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable @Positive Integer pendingId) {

        clubPendingService.cancelPending(pendingId, userDetails.getUserId());
        return ResponseEntity.ok(CommonRespDTO.success("가입 신청이 취소되었습니다."));
    }
}
