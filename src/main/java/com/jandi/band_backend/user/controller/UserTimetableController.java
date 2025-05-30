package com.jandi.band_backend.user.controller;

import com.jandi.band_backend.global.dto.CommonRespDTO;
import com.jandi.band_backend.security.CustomUserDetails;
import com.jandi.band_backend.user.dto.UserTimetableRespDTO;
import com.jandi.band_backend.user.dto.UserTimetableReqDTO;
import com.jandi.band_backend.user.dto.UserTimetableDetailsRespDTO;
import com.jandi.band_backend.user.service.UserTimetableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "User Timetable API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserTimetableController {
    private final UserTimetableService userTimetableService;

    @Operation(summary = "내 시간표 목록 조회")
    @GetMapping("/me/timetables")
    public CommonRespDTO<List<UserTimetableRespDTO>> getMyTimetables(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Integer userId = userDetails.getUserId();

        List<UserTimetableRespDTO> myTimetables = userTimetableService.getMyTimetables(userId);
        return CommonRespDTO.success("내 시간표 목록 조회 성공", myTimetables);
    }

    @Operation(summary = "내 특정 시간표 조회")
    @GetMapping("me/timetables/{timetableId}")
    public CommonRespDTO<UserTimetableDetailsRespDTO> getTimetableById(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Integer timetableId
    ) {
        Integer userId = userDetails.getUserId();
        UserTimetableDetailsRespDTO myTimetable = userTimetableService.getMyTimetableById(userId, timetableId);
        return CommonRespDTO.success("내 시간표 조회 성공", myTimetable);
    }

    @Operation(summary = "시간표 생성")
    @PostMapping("/me/timetables")
    public CommonRespDTO<UserTimetableDetailsRespDTO> createTimetable(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody UserTimetableReqDTO userTimetableReqDTO
    ) {
        Integer userId = userDetails.getUserId();

        UserTimetableDetailsRespDTO createdTimetable = userTimetableService.createTimetable(userId, userTimetableReqDTO);
        return CommonRespDTO.success("시간표 생성 성공", createdTimetable);
    }

    @Operation(summary = "시간표 수정")
    @PatchMapping("/me/timetables/{timetableId}")
    public CommonRespDTO<UserTimetableDetailsRespDTO> updateTimetable(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Integer timetableId,
        @RequestBody UserTimetableReqDTO userTimetableReqDTO
    ) {
        Integer userId = userDetails.getUserId();

        UserTimetableDetailsRespDTO updatedTimetable = userTimetableService.updateTimetable(userId, timetableId, userTimetableReqDTO);
        return CommonRespDTO.success("시간표 수정 성공", updatedTimetable);
    }

    @Operation(summary = "시간표 삭제")
    @DeleteMapping("/me/timetables/{timetableId}")
    public CommonRespDTO<Void> deleteTimetable(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Integer timetableId
    ) {
        Integer userId = userDetails.getUserId();

        userTimetableService.deleteMyTimetable(userId, timetableId);
        return CommonRespDTO.success("시간표 삭제 성공");
    }
}
