package com.jandi.band_backend.team.controller;

import com.jandi.band_backend.global.dto.CommonRespDTO;
import com.jandi.band_backend.global.dto.PagedRespDTO;
import com.jandi.band_backend.security.CustomUserDetails;
import com.jandi.band_backend.team.dto.TeamDetailRespDTO;
import com.jandi.band_backend.team.dto.TeamReqDTO;
import com.jandi.band_backend.team.dto.TeamRespDTO;
import com.jandi.band_backend.team.service.TeamService;
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

@Tag(name = "Team API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @Operation(summary = "팀 생성")
    @PostMapping("/clubs/{clubId}/teams")
    public ResponseEntity<CommonRespDTO<TeamDetailRespDTO>> createTeam(
            @PathVariable Integer clubId,
            @Valid @RequestBody TeamReqDTO teamReqDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Integer currentUserId = userDetails.getUserId();
        TeamDetailRespDTO result = teamService.createTeam(clubId, teamReqDTO, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(CommonRespDTO.success("곡 팀이 성공적으로 생성되었습니다.", result));
    }

    @Operation(summary = "동아리 팀 목록 조회")
    @GetMapping("/clubs/{clubId}/teams")
    public ResponseEntity<CommonRespDTO<PagedRespDTO<TeamRespDTO>>> getTeamsByClub(
            @PathVariable Integer clubId,
            @PageableDefault(size = 5) Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Integer currentUserId = userDetails.getUserId();
        Page<TeamRespDTO> result = teamService.getTeamsByClub(clubId, pageable, currentUserId);
        return ResponseEntity.ok(CommonRespDTO.success("곡 팀 목록을 성공적으로 조회했습니다.", PagedRespDTO.from(result)));
    }

    @Operation(summary = "팀 상세 정보 조회")
    @GetMapping("/teams/{teamId}")
    public ResponseEntity<CommonRespDTO<TeamDetailRespDTO>> getTeamDetail(
            @PathVariable Integer teamId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Integer currentUserId = userDetails.getUserId();
        TeamDetailRespDTO result = teamService.getTeamDetail(teamId, currentUserId);
        return ResponseEntity.ok(CommonRespDTO.success("곡 팀 정보를 성공적으로 조회했습니다.", result));
    }

    @Operation(summary = "팀 이름 수정")
    @PatchMapping("/teams/{teamId}")
    public ResponseEntity<CommonRespDTO<TeamRespDTO>> updateTeam(
            @PathVariable Integer teamId,
            @Valid @RequestBody TeamReqDTO teamReqDTO,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Integer currentUserId = userDetails.getUserId();
        TeamRespDTO result = teamService.updateTeam(teamId, teamReqDTO, currentUserId);
        return ResponseEntity.ok(CommonRespDTO.success("곡 팀 이름이 성공적으로 수정되었습니다.", result));
    }

    @Operation(summary = "팀 삭제")
    @DeleteMapping("/teams/{teamId}")
    public ResponseEntity<CommonRespDTO<Void>> deleteTeam(
            @PathVariable Integer teamId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Integer currentUserId = userDetails.getUserId();
        teamService.deleteTeam(teamId, currentUserId);
        return ResponseEntity.ok(CommonRespDTO.success("곡 팀이 성공적으로 삭제되었습니다."));
    }

    @Operation(summary = "팀 탈퇴")
    @DeleteMapping("/teams/{teamId}/members/me")
    public ResponseEntity<CommonRespDTO<Void>> leaveTeam(
            @PathVariable Integer teamId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Integer currentUserId = userDetails.getUserId();
        teamService.leaveTeam(teamId, currentUserId);
        return ResponseEntity.ok(CommonRespDTO.success("팀에서 성공적으로 탈퇴했습니다."));
    }
}
