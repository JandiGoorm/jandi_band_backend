package com.jandi.band_backend.global;

import com.jandi.band_backend.global.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
public class GlobalExceptionHandler {
    /// 일반적인 예외 처리
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

    /// 부적절 예외 처리
    // 대학 미존재
    @ExceptionHandler(UniversityNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleUniversityNotFound(UniversityNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), "UNIVERSITY_NOT_FOUND"));
    }

    // 동아리 미존재
    @ExceptionHandler(ClubNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleClubNotFound(ClubNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), "CLUB_NOT_FOUND"));
    }

    // 투표 미존재
    @ExceptionHandler(PollNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handlePollNotFound(PollNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), "POLL_NOT_FOUND"));
    }

    // 투표 노래 미존재
    @ExceptionHandler(PollSongNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handlePollSongNotFound(PollSongNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), "POLL_SONG_NOT_FOUND"));
    }

    // 행사한 해당 투표 미존재
    @ExceptionHandler(VoteNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> handleVoteNotFound(VoteNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), "VOTE_NOT_FOUND"));
    }

    // 행사한 해당 투표 이미 존재
    @ExceptionHandler(VoteAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<?>> handleVoteAlreadyExists(VoteAlreadyExistsException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT) // 409 Conflict - 리소스 충돌
                .body(ApiResponse.error(ex.getMessage(), "VOTE_ALREADY_EXISTS"));
    }

    // 동아리 접근 권한 없음
    @ExceptionHandler(UnauthorizedClubAccessException.class)
    public ResponseEntity<ApiResponse<?>> handleUnauthorizedClubAccess(UnauthorizedClubAccessException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage(), "UNAUTHORIZED_CLUB_ACCESS"));
    }

    /// 토큰 예외 처리
    // 부적절한 토큰
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<?>> handleInvalidToken(InvalidTokenException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage(), "INVALID_TOKEN"));
    }

    // 잘못된 접근
    @ExceptionHandler(InvalidAccessException.class)
    public ResponseEntity<ApiResponse<?>> handleInvalidAccess(InvalidAccessException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage(), "INVALID_ACCESS"));
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
