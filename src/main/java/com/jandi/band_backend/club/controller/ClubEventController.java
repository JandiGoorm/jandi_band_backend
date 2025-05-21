// ClubEventController.java
package com.jandi.band_backend.club.controller;

import com.jandi.band_backend.club.dto.ClubEventReqDTO;
import com.jandi.band_backend.club.dto.ClubEventRespDTO;
import com.jandi.band_backend.club.service.ClubEventService;
import com.jandi.band_backend.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/clubs/{clubId}/events")
@RequiredArgsConstructor
public class ClubEventController {

    private final ClubEventService clubEventService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/add")
    public ClubEventRespDTO createClubEvent(
            @PathVariable Integer clubId,
            @RequestHeader("Authorization") String token,
            @RequestBody ClubEventReqDTO dto
    ) {
        String accessToken = token.replace("Bearer ", "");
        // 원본
        String kakaoOauthId = jwtTokenProvider.getKakaoOauthId(accessToken);
        // 예시 데이터
//        String kakaoOauthId = "4264188474";
        return clubEventService.createClubEvent(clubId, kakaoOauthId, dto);

    }
}