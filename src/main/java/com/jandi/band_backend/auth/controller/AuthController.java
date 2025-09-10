package com.jandi.band_backend.auth.controller;

import com.jandi.band_backend.auth.dto.LoginRespDTO;
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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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
    @Value("${jwt.refresh-token-validity}")
    private long refreshValidityInMilliseconds;

    @Transactional
    @Operation(summary = "카카오 로그인")
    @GetMapping("/login")
    public ResponseEntity<CommonRespDTO<Object>> kakaoLogin(
            @RequestParam String code
    ){
        KakaoTokenRespDTO kakaoToken = kaKaoTokenService.getKakaoToken(code);
        KakaoUserInfoDTO kakaoUserInfo = kakaoUserService.getKakaoUserInfo(kakaoToken.getAccessToken());

        LoginRespDTO loginRespDTO = authService.login(kakaoUserInfo);

        // 리프레시 토큰은 쿠키에 저장
        ResponseCookie refreshTokenCookie = setRefreshTokenInCookie(loginRespDTO.getRefreshToken());

        // 헤더에 엑세스 토큰과 쿠키(리프레시 토큰)를 담아 응답
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .header("AccessToken", loginRespDTO.getAccessToken())
                .body(CommonRespDTO.success("로그인 성공", loginRespDTO));
    }

    @Transactional
    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public CommonRespDTO<String> logout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody RefreshReqDTO refreshReqDTO
    ){
        Integer userId = userDetails.getUserId();
        authService.logout(userId, refreshReqDTO.getRefreshToken());
        return CommonRespDTO.success("로그아웃 완료");
    }

    @Transactional
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

    @Transactional
    @Operation(summary = "회원탈퇴")
    @PostMapping("/cancel")
    public CommonRespDTO<UserInfoDTO> cancel(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ){
        Integer userId = userDetails.getUserId();
        authService.cancel(userId);
        return CommonRespDTO.success("회원탈퇴 성공");
    }

    @Transactional
    @Operation(summary = "토큰 재발급")
    @PostMapping("/refresh")
    public ResponseEntity<CommonRespDTO<TokenRespDTO>> refresh(
            @RequestBody RefreshReqDTO refreshReqDTO
    ){
        String refreshToken = refreshReqDTO.getRefreshToken();
        TokenRespDTO reissueTokensDTO = authService.refresh(refreshToken);

        // 리프레시 토큰은 쿠키에 저장
        ResponseCookie refreshTokenCookie = setRefreshTokenInCookie(reissueTokensDTO.getRefreshToken());

        // 헤더에 엑세스 토큰과 쿠키(리프레시 토큰)를 담아 응답
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .header("AccessToken", reissueTokensDTO.getAccessToken())
                .body(CommonRespDTO.success("토큰 재발급 성공"));
    }

    ResponseCookie setRefreshTokenInCookie(String refreshToken){
        return ResponseCookie.from("RefreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(refreshValidityInMilliseconds)
                .build();
    }
}
