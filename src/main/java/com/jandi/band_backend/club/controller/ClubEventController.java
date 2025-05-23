// ClubEventController.java
package com.jandi.band_backend.club.controller;

import com.jandi.band_backend.club.dto.ClubEventReqDTO;
import com.jandi.band_backend.club.dto.ClubEventRespDTO;
import com.jandi.band_backend.club.service.ClubEventService;
import com.jandi.band_backend.global.ApiResponse;
import com.jandi.band_backend.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/clubs/{clubId}/events")
@RequiredArgsConstructor
public class ClubEventController {

    private final ClubEventService clubEventService;
    private final JwtTokenProvider jwtTokenProvider;

    // 동아리 일정 추가 API
    @PostMapping("/add")
    public ClubEventRespDTO createClubEvent(
            @PathVariable Integer clubId,
            @RequestHeader("Authorization") String token,
            @RequestBody ClubEventReqDTO dto
    ) {
        String accessToken = token.replace("Bearer ", "");
        String kakaoOauthId = jwtTokenProvider.getKakaoOauthId(accessToken);
        return clubEventService.createClubEvent(clubId, kakaoOauthId, dto);

    }


    // 동아리 일정 상세 조회 API
    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<ClubEventRespDTO>> getClubEventDetail(
            @PathVariable Integer clubId,
            @PathVariable Long eventId,
            @RequestHeader("Authorization") String token
    ) {
        String accessToken = token.replace("Bearer ", "");
        String kakaoOauthId = jwtTokenProvider.getKakaoOauthId(accessToken);

        ClubEventRespDTO response = clubEventService.getClubEventDetail(clubId, eventId, kakaoOauthId);

        return ResponseEntity.ok(ApiResponse.success("동아리 일정 상세 조회 성공", response));
    }


    // 동아리 일정 목록 조회 API
    @GetMapping("/list/{timezone}/{year}/{month}")
    public ResponseEntity<ApiResponse<List<ClubEventRespDTO>>> getClubEventsByTimezoneAndMonth(
            @PathVariable Integer clubId,
            @PathVariable String timezone,
            @PathVariable int year,
            @PathVariable int month,
            @RequestHeader("Authorization") String token
    ) {
        String accessToken = token.replace("Bearer ", "");
        String kakaoOauthId = jwtTokenProvider.getKakaoOauthId(accessToken);

        List<ClubEventRespDTO> response = clubEventService.getClubEventListByMonthAndTimezone(clubId, kakaoOauthId, timezone, year, month);

        // ✅ ApiResponse로 감싸서 반환
        return ResponseEntity.ok(ApiResponse.success("동아리 일정 목록 조회 성공", response));
    }


    // 동아리 일정 삭제 API
    @DeleteMapping("/{eventId}/delete")
    public ResponseEntity<ApiResponse<Void>> deleteClubEvent(
            @PathVariable Integer clubId,
            @PathVariable Long eventId,
            @RequestHeader("Authorization") String token
    ) {
        String accessToken = token.replace("Bearer ", "");
        String kakaoOauthId = jwtTokenProvider.getKakaoOauthId(accessToken);

        clubEventService.deleteClubEvent(clubId, eventId, kakaoOauthId);

        // ✅ ApiResponse.success(String message) 사용
        return ResponseEntity.ok(ApiResponse.success("동아리 일정이 삭제되었습니다."));

    }


}