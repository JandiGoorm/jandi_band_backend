package com.jandi.band_backend.poll.controller;

import com.jandi.band_backend.global.ApiResponse;
import com.jandi.band_backend.poll.dto.*;
import com.jandi.band_backend.poll.service.PollService;
import com.jandi.band_backend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Poll API")
@RestController
@RequestMapping("/api/polls")
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;

    @Operation(summary = "투표 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<PollRespDTO>> createPoll(
            @Valid @RequestBody PollReqDTO requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PollRespDTO responseDto = pollService.createPoll(requestDto, userDetails.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("투표가 성공적으로 생성되었습니다.", responseDto));
    }

    @Operation(summary = "클럽별 투표 목록 조회")
    @GetMapping("/clubs/{clubId}")
    public ResponseEntity<ApiResponse<Page<PollRespDTO>>> getPollList(
            @PathVariable Integer clubId,
            @PageableDefault(size = 5) Pageable pageable) {
        Page<PollRespDTO> polls = pollService.getPollsByClub(clubId, pageable);
        return ResponseEntity.ok(ApiResponse.success("투표 목록을 조회했습니다.", polls));
    }

    @Operation(summary = "투표 상세 조회")
    @GetMapping("/{pollId}")
    public ResponseEntity<ApiResponse<PollDetailRespDTO>> getPollDetail(
            @PathVariable Integer pollId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Integer currentUserId = userDetails != null ? userDetails.getUserId() : null;
        PollDetailRespDTO responseDto = pollService.getPollDetail(pollId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("투표 상세 정보를 조회했습니다.", responseDto));
    }

    @Operation(summary = "투표에 곡 추가")
    @PostMapping("/{pollId}/songs")
    public ResponseEntity<ApiResponse<PollSongRespDTO>> addSongToPoll(
            @PathVariable Integer pollId,
            @Valid @RequestBody PollSongReqDTO requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PollSongRespDTO responseDto = pollService.addSongToPoll(pollId, requestDto, userDetails.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("곡이 성공적으로 투표에 추가되었습니다.", responseDto));
    }

    @Operation(summary = "곡에 투표하기")
    @PutMapping("/{pollId}/songs/{songId}/votes/{emoji}")
    public ResponseEntity<ApiResponse<PollSongRespDTO>> setVoteForSong(
            @PathVariable Integer pollId,
            @PathVariable Integer songId,
            @PathVariable String emoji,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PollSongRespDTO responseDto = pollService.setVoteForSong(pollId, songId, emoji, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("투표가 설정되었습니다.", responseDto));
    }

    @Operation(summary = "곡 투표 취소")
    @DeleteMapping("/{pollId}/songs/{songId}/votes/{emoji}")
    public ResponseEntity<ApiResponse<PollSongRespDTO>> removeVoteFromSong(
            @PathVariable Integer pollId,
            @PathVariable Integer songId,
            @PathVariable String emoji,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        PollSongRespDTO responseDto = pollService.removeVoteFromSong(pollId, songId, emoji, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("투표가 취소되었습니다.", responseDto));
    }
}
