package com.jandi.band_backend.user.controller;

import com.jandi.band_backend.global.dto.CommonRespDTO;
import com.jandi.band_backend.security.CustomUserDetails;
import com.jandi.band_backend.user.dto.UpdateUserInfoReqDTO;
import com.jandi.band_backend.user.dto.UserInfoDTO;
import com.jandi.band_backend.user.service.UserPhotoService;
import com.jandi.band_backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

@Tag(name = "User API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserPhotoService userPhotoService;

    @Operation(summary = "내 정보 조회")
    @GetMapping("/me/info")
    public CommonRespDTO<UserInfoDTO> getMyInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Integer userId = userDetails.getUserId();

        UserInfoDTO userInfo = new UserInfoDTO(
                userService.getMyInfo(userId),
                userPhotoService.getMyPhoto(userId)
        );
        return CommonRespDTO.success("내 정보 조회 성공", userInfo);
    }

    @Operation(summary = "내 정보 수정")
    @PatchMapping("/me/info")
    @Transactional
    public CommonRespDTO<?> updateMyInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(value = "nickname", required = false) String nickname,
            @RequestParam(value = "university", required = false) String university,
            @RequestParam(value = "position", required = false) String position,
            @RequestParam(value = "profilePhoto", required = false) MultipartFile profilePhoto
    ) {
        Integer userId = userDetails.getUserId();

        // DTO 생성
        UpdateUserInfoReqDTO updateDTO = new UpdateUserInfoReqDTO();
        updateDTO.setNickname(nickname);
        updateDTO.setUniversity(university);
        updateDTO.setPosition(position);
        updateDTO.setProfilePhoto(profilePhoto);

        userService.updateMyInfo(userId, updateDTO);
        userPhotoService.updateMyPhoto(userId, profilePhoto);

        UserInfoDTO userInfo = new UserInfoDTO(
                userService.getMyInfo(userId),
                userPhotoService.getMyPhoto(userId)
        );
        return CommonRespDTO.success("내 정보 수정 성공", userInfo);
    }
}
