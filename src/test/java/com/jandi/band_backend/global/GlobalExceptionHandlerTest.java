package com.jandi.band_backend.global;

import com.jandi.band_backend.global.dto.CommonRespDTO;
import com.jandi.band_backend.global.exception.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler 테스트")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("UserNotFoundException -> 404 + USER_NOT_FOUND")
    void handleUserNotFoundException() {
        // Given
        UserNotFoundException exception = new UserNotFoundException("사용자를 찾을 수 없습니다");

        // When
        ResponseEntity<CommonRespDTO<?>> response = handler.handleUserNotFound(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("USER_NOT_FOUND");
        assertThat(response.getBody().getMessage()).isEqualTo("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("ClubNotFoundException -> 404 + CLUB_NOT_FOUND")
    void handleClubNotFoundException() {
        // Given
        ClubNotFoundException exception = new ClubNotFoundException("동아리를 찾을 수 없습니다");

        // When
        ResponseEntity<CommonRespDTO<?>> response = handler.handleClubNotFound(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("CLUB_NOT_FOUND");
        assertThat(response.getBody().getMessage()).isEqualTo("동아리를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("TeamNotFoundException -> 404 + TEAM_NOT_FOUND")
    void handleTeamNotFoundException() {
        // Given
        TeamNotFoundException exception = new TeamNotFoundException("팀을 찾을 수 없습니다");

        // When
        ResponseEntity<CommonRespDTO<?>> response = handler.handleTeamNotFound(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("TEAM_NOT_FOUND");
        assertThat(response.getBody().getMessage()).isEqualTo("팀을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("PollNotFoundException -> 404 + POLL_NOT_FOUND")
    void handlePollNotFoundException() {
        // Given
        PollNotFoundException exception = new PollNotFoundException("투표를 찾을 수 없습니다");

        // When
        ResponseEntity<CommonRespDTO<?>> response = handler.handlePollNotFound(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("POLL_NOT_FOUND");
        assertThat(response.getBody().getMessage()).isEqualTo("투표를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("PollSongNotFoundException -> 404 + POLL_SONG_NOT_FOUND")
    void handlePollSongNotFoundException() {
        // Given
        PollSongNotFoundException exception = new PollSongNotFoundException("투표 곡을 찾을 수 없습니다");

        // When
        ResponseEntity<CommonRespDTO<?>> response = handler.handlePollSongNotFound(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("POLL_SONG_NOT_FOUND");
        assertThat(response.getBody().getMessage()).isEqualTo("투표 곡을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("UniversityNotFoundException -> 404 + UNIVERSITY_NOT_FOUND")
    void handleUniversityNotFoundException() {
        // Given
        UniversityNotFoundException exception = new UniversityNotFoundException("대학을 찾을 수 없습니다");

        // When
        ResponseEntity<CommonRespDTO<?>> response = handler.handleUniversityNotFound(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("UNIVERSITY_NOT_FOUND");
        assertThat(response.getBody().getMessage()).isEqualTo("대학을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("TimetableNotFoundException -> 404 + TIMETABLE_NOT_FOUND")
    void handleTimetableNotFoundException() {
        // Given
        TimetableNotFoundException exception = new TimetableNotFoundException("시간표를 찾을 수 없습니다");

        // When
        ResponseEntity<CommonRespDTO<?>> response = handler.handleTimetableNotFound(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("TIMETABLE_NOT_FOUND");
        assertThat(response.getBody().getMessage()).isEqualTo("시간표를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("VoteNotFoundException -> 404 + VOTE_NOT_FOUND")
    void handleVoteNotFoundException() {
        // Given
        VoteNotFoundException exception = new VoteNotFoundException("투표 기록을 찾을 수 없습니다");

        // When
        ResponseEntity<CommonRespDTO<?>> response = handler.handleVoteNotFound(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("VOTE_NOT_FOUND");
        assertThat(response.getBody().getMessage()).isEqualTo("투표 기록을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("ResourceNotFoundException -> 404 + RESOURCE_NOT_FOUND")
    void handleResourceNotFoundException() {
        // Given
        ResourceNotFoundException exception = new ResourceNotFoundException("리소스를 찾을 수 없습니다");

        // When
        ResponseEntity<CommonRespDTO<?>> response = handler.handleResourceNotFound(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().getMessage()).isEqualTo("리소스를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("VoteAlreadyExistsException -> 409 + VOTE_ALREADY_EXISTS")
    void handleVoteAlreadyExistsException() {
        // Given
        VoteAlreadyExistsException exception = new VoteAlreadyExistsException("이미 투표하셨습니다");

        // When
        ResponseEntity<CommonRespDTO<?>> response = handler.handleVoteAlreadyExists(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("VOTE_ALREADY_EXISTS");
        assertThat(response.getBody().getMessage()).isEqualTo("이미 투표하셨습니다");
    }

    @Test
    @DisplayName("UnauthorizedClubAccessException -> 403 + UNAUTHORIZED_CLUB_ACCESS")
    void handleUnauthorizedClubAccessException() {
        // Given
        UnauthorizedClubAccessException exception = new UnauthorizedClubAccessException("동아리 접근 권한이 없습니다");

        // When
        ResponseEntity<CommonRespDTO<?>> response = handler.handleUnauthorizedClubAccess(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("UNAUTHORIZED_CLUB_ACCESS");
        assertThat(response.getBody().getMessage()).isEqualTo("동아리 접근 권한이 없습니다");
    }

    @Test
    @DisplayName("InvalidTokenException -> 401 + INVALID_TOKEN")
    void handleInvalidTokenException() {
        // Given
        InvalidTokenException exception = new InvalidTokenException();

        // When
        ResponseEntity<CommonRespDTO<?>> response = handler.handleInvalidToken(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("INVALID_TOKEN");
        assertThat(response.getBody().getMessage()).contains("유효하지 않은 토큰");
    }

    @Test
    @DisplayName("InvalidAccessException -> 403 + INVALID_ACCESS")
    void handleInvalidAccessException() {
        // Given
        InvalidAccessException exception = new InvalidAccessException("잘못된 접근입니다");

        // When
        ResponseEntity<CommonRespDTO<?>> response = handler.handleInvalidAccess(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("INVALID_ACCESS");
        assertThat(response.getBody().getMessage()).isEqualTo("잘못된 접근입니다");
    }

    @Test
    @DisplayName("IllegalArgumentException -> 403 + ILLEGAL_ARGUMENT")
    void handleIllegalArgumentException() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("잘못된 인자입니다");

        // When
        ResponseEntity<CommonRespDTO<?>> response = handler.handleIllegalArgument(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("ILLEGAL_ARGUMENT");
        assertThat(response.getBody().getMessage()).isEqualTo("잘못된 인자입니다");
    }

    @Test
    @DisplayName("BadRequestException -> 400 + BAD_REQUEST")
    void handleBadRequestException() {
        // Given
        BadRequestException exception = new BadRequestException("잘못된 요청입니다");

        // When
        ResponseEntity<CommonRespDTO<?>> response = handler.handleBadRequest(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("BAD_REQUEST");
        assertThat(response.getBody().getMessage()).isEqualTo("잘못된 요청입니다");
    }

    @Test
    @DisplayName("TeamLeaveNotAllowedException -> 400 + TEAM_LEAVE_NOT_ALLOWED")
    void handleTeamLeaveNotAllowedException() {
        // Given
        TeamLeaveNotAllowedException exception = new TeamLeaveNotAllowedException("팀 탈퇴가 허용되지 않습니다");

        // When
        ResponseEntity<CommonRespDTO<?>> response = handler.handleTeamLeaveNotAllowed(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("TEAM_LEAVE_NOT_ALLOWED");
        assertThat(response.getBody().getMessage()).isEqualTo("팀 탈퇴가 허용되지 않습니다");
    }

    @Test
    @DisplayName("FailKakaoLoginException -> 401 + FAIL_KAKAO_LOGIN")
    void handleFailKakaoLoginException() {
        // Given
        FailKakaoLoginException exception = new FailKakaoLoginException("카카오 로그인에 실패했습니다");

        // When
        ResponseEntity<CommonRespDTO<?>> response = handler.handleFailKakaoLogin(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("FAIL_KAKAO_LOGIN");
        assertThat(response.getBody().getMessage()).isEqualTo("카카오 로그인에 실패했습니다");
    }

    @Test
    @DisplayName("FailKakaoReadUserException -> 401 + FAIL_KAKAO_USER")
    void handleFailKakaoReadUserException() {
        // Given
        FailKakaoReadUserException exception = new FailKakaoReadUserException("카카오 사용자 정보 조회에 실패했습니다");

        // When
        ResponseEntity<CommonRespDTO<?>> response = handler.handleFailKakaoReadUser(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("FAIL_KAKAO_USER");
        assertThat(response.getBody().getMessage()).isEqualTo("카카오 사용자 정보 조회에 실패했습니다");
    }

    @Test
    @DisplayName("MissingServletRequestParameterException -> 400 + MISSING_PARAMETER")
    void handleMissingServletRequestParameterException() {
        // Given
        MissingServletRequestParameterException exception = 
            new MissingServletRequestParameterException("testParam", "String");

        // When
        ResponseEntity<CommonRespDTO<?>> response = handler.handleMissingServletRequestParameter(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("MISSING_PARAMETER");
        assertThat(response.getBody().getMessage()).contains("testParam");
    }

    @Test
    @DisplayName("RuntimeException -> 400 + RUNTIME_EXCEPTION")
    void handleRuntimeException() {
        // Given
        RuntimeException exception = new RuntimeException("런타임 에러");

        // When
        ResponseEntity<CommonRespDTO<?>> response = handler.handleRuntimeException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("RUNTIME_EXCEPTION");
        assertThat(response.getBody().getMessage()).isEqualTo("런타임 에러");
    }

    @Test
    @DisplayName("Exception -> 500 + INTERNAL_ERROR")
    void handleException() {
        // Given
        Exception exception = new Exception("일반 예외");

        // When
        ResponseEntity<CommonRespDTO<?>> response = handler.handleException(exception);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getErrorCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().getMessage()).contains("서버 내부 오류");
    }
}
