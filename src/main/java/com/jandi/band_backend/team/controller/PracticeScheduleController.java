package com.jandi.band_backend.team.controller;

import com.jandi.band_backend.global.dto.CommonRespDTO;
import com.jandi.band_backend.global.dto.PagedRespDTO;
import com.jandi.band_backend.security.CustomUserDetails;
import com.jandi.band_backend.team.dto.PracticeScheduleReqDTO;
import com.jandi.band_backend.team.dto.PracticeScheduleRespDTO;
import com.jandi.band_backend.team.service.PracticeScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Practice Schedule API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PracticeScheduleController {

    private final PracticeScheduleService practiceScheduleService;

    @Operation(summary = "팀별 연습 일정 목록 조회")
    @GetMapping("/teams/{teamId}/practice-schedules")
    public ResponseEntity<CommonRespDTO<PagedRespDTO<PracticeScheduleRespDTO>>> getPracticeSchedulesByTeam(
            @PathVariable Integer teamId,
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer userId = userDetails.getUserId();
        Page<PracticeScheduleRespDTO> result = practiceScheduleService.getPracticeSchedulesByTeam(teamId, pageable, userId);
        return ResponseEntity.ok(CommonRespDTO.success("팀별 곡 연습 일정 목록 조회 성공", PagedRespDTO.from(result)));
    }

    @Operation(summary = "연습 일정 상세 조회")
    @GetMapping("/practice-schedules/{scheduleId}")
    public ResponseEntity<CommonRespDTO<PracticeScheduleRespDTO>> getPracticeSchedule(
            @PathVariable Integer scheduleId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer userId = userDetails.getUserId();
        return ResponseEntity.ok(CommonRespDTO.success("곡 연습 일정 상세 조회 성공",
                practiceScheduleService.getPracticeSchedule(scheduleId, userId)));
    }

    @Operation(summary = "연습 일정 생성")
    @PostMapping("/teams/{teamId}/practice-schedules")
    public ResponseEntity<CommonRespDTO<PracticeScheduleRespDTO>> createPracticeSchedule(
            @PathVariable Integer teamId,
            @Valid @RequestBody PracticeScheduleReqDTO request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // teamId를 request에 설정
        request.setTeamId(teamId);
        Integer userId = userDetails.getUserId();

        return ResponseEntity.ok(CommonRespDTO.success("곡 연습 일정 생성 성공",
                practiceScheduleService.createPracticeSchedule(request, userId)));
    }

    @Operation(summary = "연습 일정 삭제")
    @DeleteMapping("/practice-schedules/{scheduleId}")
    public ResponseEntity<CommonRespDTO<Void>> deletePracticeSchedule(
            @PathVariable Integer scheduleId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer userId = userDetails.getUserId();
        practiceScheduleService.deletePracticeSchedule(scheduleId, userId);
        return ResponseEntity.ok(CommonRespDTO.success("곡 연습 일정 삭제 성공"));
    }
}
