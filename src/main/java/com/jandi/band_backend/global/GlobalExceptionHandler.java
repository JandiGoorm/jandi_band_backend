package com.jandi.band_backend.global;

import com.jandi.band_backend.global.exception.FailKakaoReadUserException;
import com.jandi.band_backend.global.exception.InvalidTokenException;
import com.jandi.band_backend.global.exception.FailKakaoLoginException;
import com.jandi.band_backend.global.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
public class GlobalExceptionHandler {
    /// 일반적인 예외
    // 전역적 런타임 에러
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<?>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), "RUNTIME_EXCEPTION"));
    }
    // 기타 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("서버 내부 오류가 발생했습니다", "INTERNAL_ERROR"));
    }

    /// 미존재 예외 처리
    // 사용자 미존재
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), "USER_NOT_FOUND"));
    }

    /// 토큰 예외 처리
    // 부적절한 토큰
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<?>> handleInvalidToken(InvalidTokenException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage(), "INVALID_TOKEN"));
    }

    /// 카카오 예외 처리
    // 카카오 로그인 토큰 발급 실패
    @ExceptionHandler(FailKakaoLoginException.class)
    public ResponseEntity<ApiResponse<?>> handleFailKakaoLogin(FailKakaoLoginException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage(), "FAIL_KAKAO_LOGIN"));
    }

    // 카카오 유저 정보 조회 실패
    @ExceptionHandler(FailKakaoReadUserException.class)
    public ResponseEntity<ApiResponse<?>> handleFailKakaoReadUser(FailKakaoReadUserException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage(), "FAIL_KAKAO_USER"));
    }
}