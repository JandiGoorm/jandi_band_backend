package com.jandi.band_backend.poll.controller;

import com.jandi.band_backend.global.ApiResponse;
import com.jandi.band_backend.poll.dto.*;
import com.jandi.band_backend.poll.service.PollService;
import com.jandi.band_backend.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/polls")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;

    @PostMapping
    public ResponseEntity<ApiResponse<PollRespDTO>> createPoll(
            @Valid @RequestBody PollCreateReqDTO requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PollRespDTO responseDto = pollService.createPoll(requestDto, userDetails.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("투표가 성공적으로 생성되었습니다.", responseDto));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<PollRespDTO>>> getPollList(
            @RequestParam(required = true) Integer clubId,
            @PageableDefault(size = 10) Pageable pageable) {

        Page<PollRespDTO> polls = pollService.getPollsByClub(clubId, pageable);
        return ResponseEntity.ok(ApiResponse.success("투표 목록을 조회했습니다.", polls));
    }

    @GetMapping("/{pollId}")
    public ResponseEntity<ApiResponse<PollDetailRespDTO>> getPollDetail(@PathVariable Integer pollId) {
        PollDetailRespDTO responseDto = pollService.getPollDetail(pollId);
        return ResponseEntity.ok(ApiResponse.success("투표 상세 정보를 조회했습니다.", responseDto));
    }

    @PostMapping("/{pollId}/songs")
    public ResponseEntity<ApiResponse<PollSongRespDTO>> addSongToPoll(
            @PathVariable Integer pollId,
            @Valid @RequestBody PollSongCreateReqDTO requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PollSongRespDTO responseDto = pollService.addSongToPoll(pollId, requestDto, userDetails.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("곡이 성공적으로 투표에 추가되었습니다.", responseDto));
    }
}
