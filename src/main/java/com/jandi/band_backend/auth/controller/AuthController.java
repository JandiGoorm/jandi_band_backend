package com.jandi.band_backend.auth.controller;

import com.jandi.band_backend.auth.dto.TokenRespDTO;
import com.jandi.band_backend.auth.dto.RefreshReqDTO;
import com.jandi.band_backend.auth.dto.SignUpReqDTO;
import com.jandi.band_backend.auth.dto.kakao.KakaoTokenRespDTO;
import com.jandi.band_backend.auth.dto.kakao.KakaoUserInfoDTO;
import com.jandi.band_backend.auth.service.kakao.KaKaoTokenService;
import com.jandi.band_backend.auth.service.kakao.KakaoUserService;
import com.jandi.band_backend.global.dto.CommonRespDTO;
import com.jandi.band_backend.security.CustomUserDetails;
import com.jandi.band_backend.user.dto.UserInfoDTO;
import com.jandi.band_backend.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final KaKaoTokenService kaKaoTokenService;
    private final KakaoUserService kakaoUserService;

    @Operation(summary = "카카오 로그인")
    @GetMapping("/login")
    public CommonRespDTO<TokenRespDTO> kakaoLogin(
            @RequestParam String code
    ){
        KakaoTokenRespDTO kakaoToken = kaKaoTokenService.getKakaoToken(code);
        KakaoUserInfoDTO kakaoUserInfo = kakaoUserService.getKakaoUserInfo(kakaoToken.getAccessToken());

        TokenRespDTO tokens = authService.login(kakaoUserInfo);
        return CommonRespDTO.success("로그인 성공", tokens);
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public CommonRespDTO<String> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        Integer userId = userDetails.getUserId();
        authService.logout(userId);
        return CommonRespDTO.success("로그아웃 완료");
    }

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    public CommonRespDTO<UserInfoDTO> signUp(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody SignUpReqDTO signUpReqDTO
    ){
        Integer userId = userDetails.getUserId();
        UserInfoDTO userInfo = authService.signup(userId, signUpReqDTO);
        return CommonRespDTO.success("회원가입 성공", userInfo);
    }

    @Operation(summary = "회원탈퇴")
    @PostMapping("/cancel")
    public CommonRespDTO<UserInfoDTO> cancel(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        Integer userId = userDetails.getUserId();
        authService.cancel(userId);
        return CommonRespDTO.success("회원탈퇴 성공");
    }

    @Operation(summary = "토큰 재발급")
    @PostMapping("/refresh")
    public CommonRespDTO<TokenRespDTO> refresh(
            @RequestBody RefreshReqDTO refreshReqDTO
    ){
        String refreshToken = refreshReqDTO.getRefreshToken();
        TokenRespDTO tokens = authService.refresh(refreshToken);
        return CommonRespDTO.success("토큰 재발급 성공", tokens);
    }
}
