package com.jandi.band_backend.global;

import com.jandi.band_backend.global.dto.CommonRespDTO;
import com.jandi.band_backend.global.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice
public class GlobalExceptionHandler {
    /// 일반적인 예외 처리
    // 전역적 런타임 에러
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<CommonRespDTO<?>> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CommonRespDTO.error(ex.getMessage(), "RUNTIME_EXCEPTION"));
    }
    // 기타 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonRespDTO<?>> handleException(@SuppressWarnings("unused") Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CommonRespDTO.error("서버 내부 오류가 발생했습니다: " + ex.getMessage(), "INTERNAL_ERROR"));
    }

    /// 미존재 예외 처리
    // 리소스 미존재 (일반적인 경우)
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<CommonRespDTO<?>> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(CommonRespDTO.error(ex.getMessage(), "RESOURCE_NOT_FOUND"));
    }

    // 사용자 미존재
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<CommonRespDTO<?>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(CommonRespDTO.error(ex.getMessage(), "USER_NOT_FOUND"));
    }

    // 대학 미존재
    @ExceptionHandler(UniversityNotFoundException.class)
    public ResponseEntity<CommonRespDTO<?>> handleUniversityNotFound(UniversityNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(CommonRespDTO.error(ex.getMessage(), "UNIVERSITY_NOT_FOUND"));
    }

    // 동아리 미존재
    @ExceptionHandler(ClubNotFoundException.class)
    public ResponseEntity<CommonRespDTO<?>> handleClubNotFound(ClubNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(CommonRespDTO.error(ex.getMessage(), "CLUB_NOT_FOUND"));
    }

    // 팀 미존재
    @ExceptionHandler(TeamNotFoundException.class)
    public ResponseEntity<CommonRespDTO<?>> handleTeamNotFound(TeamNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(CommonRespDTO.error(ex.getMessage(), "TEAM_NOT_FOUND"));
    }

    // 투표 미존재
    @ExceptionHandler(PollNotFoundException.class)
    public ResponseEntity<CommonRespDTO<?>> handlePollNotFound(PollNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(CommonRespDTO.error(ex.getMessage(), "POLL_NOT_FOUND"));
    }

    // 투표 노래 미존재
    @ExceptionHandler(PollSongNotFoundException.class)
    public ResponseEntity<CommonRespDTO<?>> handlePollSongNotFound(PollSongNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(CommonRespDTO.error(ex.getMessage(), "POLL_SONG_NOT_FOUND"));
    }

    // 시간표 미존재
    @ExceptionHandler(TimetableNotFoundException.class)
    public ResponseEntity<CommonRespDTO<?>> handleTimetableNotFound(TimetableNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(CommonRespDTO.error(ex.getMessage(), "TIMETABLE_NOT_FOUND"));
    }

    /// 부적절 접근 처리
    // 행사한 해당 투표 미존재
    @ExceptionHandler(VoteNotFoundException.class)
    public ResponseEntity<CommonRespDTO<?>> handleVoteNotFound(VoteNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(CommonRespDTO.error(ex.getMessage(), "VOTE_NOT_FOUND"));
    }

    // 행사한 해당 투표 이미 존재
    @ExceptionHandler(VoteAlreadyExistsException.class)
    public ResponseEntity<CommonRespDTO<?>> handleVoteAlreadyExists(VoteAlreadyExistsException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT) // 409 Conflict - 리소스 충돌
                .body(CommonRespDTO.error(ex.getMessage(), "VOTE_ALREADY_EXISTS"));
    }

    // 동아리 접근 권한 없음
    @ExceptionHandler(UnauthorizedClubAccessException.class)
    public ResponseEntity<CommonRespDTO<?>> handleUnauthorizedClubAccess(UnauthorizedClubAccessException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(CommonRespDTO.error(ex.getMessage(), "UNAUTHORIZED_CLUB_ACCESS"));
    }

    // 부적절한 토큰
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<CommonRespDTO<?>> handleInvalidToken(InvalidTokenException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(CommonRespDTO.error(ex.getMessage(), "INVALID_TOKEN"));
    }

    // 잘못된 접근
    @ExceptionHandler(InvalidAccessException.class)
    public ResponseEntity<CommonRespDTO<?>> handleInvalidAccess(InvalidAccessException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(CommonRespDTO.error(ex.getMessage(), "INVALID_ACCESS"));
    }

    // 잘못된 인자
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CommonRespDTO<?>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(CommonRespDTO.error(ex.getMessage(), "ILLEGAL_ARGUMENT"));
    }

    // 잘못된 요청 데이터
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<CommonRespDTO<?>> handleBadRequest(BadRequestException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CommonRespDTO.error(ex.getMessage(), "BAD_REQUEST"));
    }

    // 팀 탈퇴 불가
    @ExceptionHandler(TeamLeaveNotAllowedException.class)
    public ResponseEntity<CommonRespDTO<?>> handleTeamLeaveNotAllowed(TeamLeaveNotAllowedException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CommonRespDTO.error(ex.getMessage(), "TEAM_LEAVE_NOT_ALLOWED"));
    }

    /// 카카오 예외 처리
    // 카카오 로그인 토큰 발급 실패
    @ExceptionHandler(FailKakaoLoginException.class)
    public ResponseEntity<CommonRespDTO<?>> handleFailKakaoLogin(FailKakaoLoginException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(CommonRespDTO.error(ex.getMessage(), "FAIL_KAKAO_LOGIN"));
    }

    // 카카오 유저 정보 조회 실패
    @ExceptionHandler(FailKakaoReadUserException.class)
    public ResponseEntity<CommonRespDTO<?>> handleFailKakaoReadUser(FailKakaoReadUserException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(CommonRespDTO.error(ex.getMessage(), "FAIL_KAKAO_USER"));
    }
}
