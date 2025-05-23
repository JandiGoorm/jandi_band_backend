package com.jandi.band_backend.user.controller;

import com.jandi.band_backend.global.ApiResponse;
import com.jandi.band_backend.security.CustomUserDetails;
import com.jandi.band_backend.user.dto.MyClubResponse;
import com.jandi.band_backend.user.dto.MyTeamResponse;
import com.jandi.band_backend.user.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/my")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    /**
     * 내가 참가한 동아리 목록 조회
     */
    @GetMapping("/clubs")
    public ApiResponse<List<MyClubResponse>> getMyClubs(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        List<MyClubResponse> myClubs = myPageService.getMyClubs(currentUser.getUserId());
        return ApiResponse.success("내가 참가한 동아리 목록 조회 성공", myClubs);
    }

    /**
     * 내가 참가한 팀 목록 조회
     */
    @GetMapping("/teams")
    public ApiResponse<List<MyTeamResponse>> getMyTeams(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        List<MyTeamResponse> myTeams = myPageService.getMyTeams(currentUser.getUserId());
        return ApiResponse.success("내가 참가한 팀 목록 조회 성공", myTeams);
    }
} 