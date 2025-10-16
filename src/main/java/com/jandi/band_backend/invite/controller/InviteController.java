package com.jandi.band_backend.invite.controller;

import com.jandi.band_backend.global.dto.CommonRespDTO;
import com.jandi.band_backend.invite.dto.InviteLinkRespDTO;
import com.jandi.band_backend.invite.service.InviteService;
import com.jandi.band_backend.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Invite API")
@RestController
@RequestMapping("api/invite")
@RequiredArgsConstructor
public class InviteController {
    private final InviteService inviteService;

    @Operation(summary = "동아리 초대 링크 생성")
    @PostMapping("/clubs/{clubId}")
    public CommonRespDTO<InviteLinkRespDTO> inviteClub(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("clubId") Integer clubId
    ) {
        Integer userId = userDetails.getUserId();
        InviteLinkRespDTO inviteLinkRespDTO = inviteService.generateInviteClubLink(clubId, userId);
        return CommonRespDTO.success("동아리 초대 링크 생성 성공", inviteLinkRespDTO);
    }

    @Operation(summary = "팀 초대 링크 생성")
    @PostMapping("/teams/{teamId}")
    public CommonRespDTO<InviteLinkRespDTO> inviteTeam(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("teamId") Integer teamId
    ) {
        Integer userId = userDetails.getUserId();
        InviteLinkRespDTO inviteLinkRespDTO = inviteService.generateInviteTeamLink(teamId, userId);
        return CommonRespDTO.success("팀 초대 링크 생성 성공", inviteLinkRespDTO);
    }
}
