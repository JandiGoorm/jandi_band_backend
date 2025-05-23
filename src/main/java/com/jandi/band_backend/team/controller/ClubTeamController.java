package com.jandi.band_backend.team.controller;

import com.jandi.band_backend.global.ApiResponse;
import com.jandi.band_backend.team.dto.ClubTeamResponse;
import com.jandi.band_backend.team.service.ClubTeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubTeamController {

    private final ClubTeamService clubTeamService;

    /**
     * 동아리 팀 목록 조회
     */
    @GetMapping("/{clubId}/teams")
    public ApiResponse<Page<ClubTeamResponse>> getClubTeams(
            @PathVariable Integer clubId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Page<ClubTeamResponse> teams = clubTeamService.getTeamsByClub(clubId, pageable);
        return ApiResponse.success("동아리 팀 목록 조회 성공", teams);
    }
} 