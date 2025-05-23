package com.jandi.band_backend.team.controller;

import com.jandi.band_backend.global.ApiResponse;
import com.jandi.band_backend.team.dto.PracticeScheduleRequest;
import com.jandi.band_backend.team.dto.PracticeScheduleResponse;
import com.jandi.band_backend.team.service.PracticeScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PracticeScheduleController {

    private final PracticeScheduleService practiceScheduleService;

    // 팀별 곡 연습 일정 목록 조회
    @GetMapping("/teams/{teamId}/practice-schedules")
    public ResponseEntity<ApiResponse<Page<PracticeScheduleResponse>>> getPracticeSchedulesByTeam(
            @PathVariable Integer teamId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("팀별 곡 연습 일정 목록 조회 성공", 
                practiceScheduleService.getPracticeSchedulesByTeam(teamId, pageable)));
    }

    // 곡 연습 일정 상세 조회
    @GetMapping("/practice-schedules/{scheduleId}")
    public ResponseEntity<ApiResponse<PracticeScheduleResponse>> getPracticeSchedule(
            @PathVariable Integer scheduleId) {
        return ResponseEntity.ok(ApiResponse.success("곡 연습 일정 상세 조회 성공", 
                practiceScheduleService.getPracticeSchedule(scheduleId)));
    }

    // 곡 연습 일정 생성
    @PostMapping("/teams/{teamId}/practice-schedules")
    public ResponseEntity<ApiResponse<PracticeScheduleResponse>> createPracticeSchedule(
            @PathVariable Integer teamId,
            @Valid @RequestBody PracticeScheduleRequest request,
            @RequestAttribute("userId") Integer userId) {
        // teamId를 request에 설정
        request.setTeamId(teamId);
        
        return ResponseEntity.ok(ApiResponse.success("곡 연습 일정 생성 성공", 
                practiceScheduleService.createPracticeSchedule(request, userId)));
    }

    // 곡 연습 일정 삭제
    @DeleteMapping("/practice-schedules/{scheduleId}")
    public ResponseEntity<ApiResponse<Void>> deletePracticeSchedule(
            @PathVariable Integer scheduleId,
            @RequestAttribute("userId") Integer userId) {
        practiceScheduleService.deletePracticeSchedule(scheduleId, userId);
        return ResponseEntity.ok(ApiResponse.success("곡 연습 일정 삭제 성공"));
    }
} 