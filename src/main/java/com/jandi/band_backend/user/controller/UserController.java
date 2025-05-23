package com.jandi.band_backend.user.controller;

import com.jandi.band_backend.global.ApiResponse;
import com.jandi.band_backend.security.jwt.JwtTokenProvider;
import com.jandi.band_backend.user.dto.UpdateUserInfoReqDTO;
import com.jandi.band_backend.user.dto.UserInfoDTO;
import com.jandi.band_backend.user.service.UserPhotoService;
import com.jandi.band_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserPhotoService userPhotoService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("/me/info")
    public ApiResponse<UserInfoDTO> getMyInfo(
            @RequestHeader("Authorization") String token
    ) {
        String accessToken = token.replace("Bearer ", "");
        String kakaoOauthId = jwtTokenProvider.getKakaoOauthId(accessToken);

        // 유저 기본 정보 및 프로필 정보 조회
        UserInfoDTO userInfo = new UserInfoDTO(
                userService.getMyInfo(kakaoOauthId),
                userPhotoService.getMyPhoto(kakaoOauthId)
        );
        return ApiResponse.success("내 정보 조회 성공", userInfo);
    }

    @PatchMapping("/me/info")
    public ApiResponse<UserInfoDTO> updateMyInfo(
            @RequestHeader("Authorization") String token,
            @ModelAttribute UpdateUserInfoReqDTO updateDTO,
            @RequestPart(value = "profilePhoto", required = false) MultipartFile profilePhoto

    ) {
        String accessToken = token.replace("Bearer ", "");
        String kakaoOauthId = jwtTokenProvider.getKakaoOauthId(accessToken);

        // 유저 기본 정보 및 프로필 사진 수정
        userService.updateMyInfo(kakaoOauthId, updateDTO);
        userPhotoService.updateMyPhoto(kakaoOauthId, profilePhoto);
        return ApiResponse.success("내 정보 수정 성공");
    }
}
