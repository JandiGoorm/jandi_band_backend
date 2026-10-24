package com.jandi.band_backend.invite.service;

import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.entity.ClubMember;
import com.jandi.band_backend.club.repository.ClubMemberRepository;
import com.jandi.band_backend.global.exception.BannedMemberJoinAttemptException;
import com.jandi.band_backend.global.exception.InvalidAccessException;
import com.jandi.band_backend.global.exception.UserNotFoundException;
import com.jandi.band_backend.invite.dto.JoinRespDTO;
import com.jandi.band_backend.invite.store.InviteCodeService;
import com.jandi.band_backend.team.repository.TeamMemberRepository;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JoinService 동아리 가입 테스트")
class JoinClubServiceTest {

    @InjectMocks
    private JoinService joinService;

    @Mock private InviteCodeService inviteCodeService;
    @Mock private InviteUtilService inviteUtilService;
    @Mock private UserRepository userRepository;
    @Mock private ClubMemberRepository clubMemberRepository;
    @Mock private TeamMemberRepository teamMemberRepository;

    private Users testUser;
    private Club testClub;
    private final Integer TEST_USER_ID = 1;
    private final Integer TEST_CLUB_ID = 1;
    private final String TEST_INVITE_CODE = "ABC123DEF";
    private final String TEST_KEY_ID = "club_1";

    @BeforeEach
    void setUp() {
        testUser = new Users();
        testUser.setId(TEST_USER_ID);
        testUser.setKakaoOauthId("test_kakao_123");
        testUser.setNickname("테스트사용자");
        testUser.setIsRegistered(true);

        testClub = new Club();
        testClub.setId(TEST_CLUB_ID);
        testClub.setName("테스트동아리");
    }

    @Test
    @DisplayName("1-1. 신규 사용자의 동아리 정상 가입")
    void joinClub_Success_NewUser() {
        // Given
        when(inviteCodeService.getKeyId(TEST_INVITE_CODE)).thenReturn(TEST_KEY_ID);
        when(inviteUtilService.getClub(TEST_KEY_ID)).thenReturn(testClub);
        when(inviteUtilService.isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID)).thenReturn(false);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(clubMemberRepository.findByClubIdAndUserId(TEST_CLUB_ID, TEST_USER_ID))
                .thenReturn(Optional.empty());

        // When
        JoinRespDTO result = joinService.joinClub(TEST_USER_ID, TEST_INVITE_CODE);

        // Then
        assertNotNull(result);
        assertEquals(TEST_CLUB_ID, result.getClubId());
        assertNull(result.getTeamId());

        verify(inviteCodeService).getKeyId(TEST_INVITE_CODE);
        verify(inviteUtilService).getClub(TEST_KEY_ID);
        verify(inviteUtilService).isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID);
        verify(userRepository).findById(TEST_USER_ID);
        verify(clubMemberRepository).findByClubIdAndUserId(TEST_CLUB_ID, TEST_USER_ID);
        verify(clubMemberRepository).save(any(ClubMember.class));
    }

    @Test
    @DisplayName("1-2. 소프트 삭제된 사용자의 동아리 재가입 (재활성화)")
    void joinClub_Success_ReactivateSoftDeletedUser() {
        // Given
        ClubMember deletedMember = new ClubMember();
        deletedMember.setClub(testClub);
        deletedMember.setUser(testUser);
        deletedMember.setRole(ClubMember.MemberRole.MEMBER);
        deletedMember.setDeletedAt(LocalDateTime.now().minusDays(1));

        LocalDateTime testStartTime = LocalDateTime.now();

        when(inviteCodeService.getKeyId(TEST_INVITE_CODE)).thenReturn(TEST_KEY_ID);
        when(inviteUtilService.getClub(TEST_KEY_ID)).thenReturn(testClub);
        when(inviteUtilService.isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID)).thenReturn(false);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(clubMemberRepository.findByClubIdAndUserId(TEST_CLUB_ID, TEST_USER_ID))
                .thenReturn(Optional.of(deletedMember));

        // When
        JoinRespDTO result = joinService.joinClub(TEST_USER_ID, TEST_INVITE_CODE);

        // Then
        assertNotNull(result);
        assertEquals(TEST_CLUB_ID, result.getClubId());
        assertNull(result.getTeamId());

        // 재활성화 검증
        assertNull(deletedMember.getDeletedAt());
        assertEquals(ClubMember.MemberRole.MEMBER, deletedMember.getRole());
        assertNotNull(deletedMember.getUpdatedAt());
        assertTrue(deletedMember.getUpdatedAt().isAfter(testStartTime) ||
                   deletedMember.getUpdatedAt().isEqual(testStartTime));

        verify(clubMemberRepository).save(deletedMember);
        verify(clubMemberRepository, never()).save(argThat(member -> member != deletedMember));
    }

    @Test
    @DisplayName("2-1. 존재하지 않는 사용자 ID로 가입 시도")
    void joinClub_ThrowsException_UserNotFound() {
        // Given
        when(inviteCodeService.getKeyId(TEST_INVITE_CODE)).thenReturn(TEST_KEY_ID);
        when(inviteUtilService.getClub(TEST_KEY_ID)).thenReturn(testClub);
        when(inviteUtilService.isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID)).thenReturn(false);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class,
                () -> joinService.joinClub(TEST_USER_ID, TEST_INVITE_CODE));

        verify(inviteCodeService).getKeyId(TEST_INVITE_CODE);
        verify(inviteUtilService).getClub(TEST_KEY_ID);
        verify(inviteUtilService).isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID);
        verify(userRepository).findById(TEST_USER_ID);
        verify(clubMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("2-2. 유효하지 않은 초대 코드로 가입 시도")
    void joinClub_ThrowsException_InvalidInviteCode() {
        // Given
        when(inviteCodeService.getKeyId(TEST_INVITE_CODE))
                .thenThrow(new RuntimeException("유효하지 않은 초대 코드"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> joinService.joinClub(TEST_USER_ID, TEST_INVITE_CODE));

        assertEquals("유효하지 않은 초대 코드", exception.getMessage());

        verify(inviteCodeService).getKeyId(TEST_INVITE_CODE);
        verify(inviteUtilService, never()).getClub(anyString());
        verify(clubMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("2-3. 이미 활성화된 멤버가 재가입 시도")
    void joinClub_ThrowsException_AlreadyActiveMember() {
        // Given
        when(inviteCodeService.getKeyId(TEST_INVITE_CODE)).thenReturn(TEST_KEY_ID);
        when(inviteUtilService.getClub(TEST_KEY_ID)).thenReturn(testClub);
        when(inviteUtilService.isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID)).thenReturn(true);

        // When & Then
        InvalidAccessException exception = assertThrows(InvalidAccessException.class,
                () -> joinService.joinClub(TEST_USER_ID, TEST_INVITE_CODE));

        assertEquals("이미 가입한 동아리입니다", exception.getMessage());

        verify(inviteCodeService).getKeyId(TEST_INVITE_CODE);
        verify(inviteUtilService).getClub(TEST_KEY_ID);
        verify(inviteUtilService).isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID);
        verify(userRepository, never()).findById(anyInt());
        verify(clubMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("2-4. 강퇴된 사용자가 재가입 시도")
    void joinClub_ThrowsException_BannedUserRejoin() {
        // Given
        ClubMember bannedMember = new ClubMember();
        bannedMember.setClub(testClub);
        bannedMember.setUser(testUser);
        bannedMember.setRole(ClubMember.MemberRole.BANNED);

        when(inviteCodeService.getKeyId(TEST_INVITE_CODE)).thenReturn(TEST_KEY_ID);
        when(inviteUtilService.getClub(TEST_KEY_ID)).thenReturn(testClub);
        when(inviteUtilService.isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID)).thenReturn(false);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(clubMemberRepository.findByClubIdAndUserId(TEST_CLUB_ID, TEST_USER_ID))
                .thenReturn(Optional.of(bannedMember));

        // When & Then
        BannedMemberJoinAttemptException exception = assertThrows(BannedMemberJoinAttemptException.class,
                () -> joinService.joinClub(TEST_USER_ID, TEST_INVITE_CODE));

        assertEquals("강퇴된 사용자는 해당 동아리에 재가입할 수 없습니다.", exception.getMessage());

        verify(inviteCodeService).getKeyId(TEST_INVITE_CODE);
        verify(inviteUtilService).getClub(TEST_KEY_ID);
        verify(inviteUtilService).isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID);
        verify(userRepository).findById(TEST_USER_ID);
        verify(clubMemberRepository).findByClubIdAndUserId(TEST_CLUB_ID, TEST_USER_ID);
        verify(clubMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("2-5. 강퇴된 상태에서 소프트 삭제도 된 사용자가 재가입 시도")
    void joinClub_ThrowsException_BannedAndSoftDeletedUserRejoin() {
        // Given
        ClubMember bannedAndDeletedMember = new ClubMember();
        bannedAndDeletedMember.setClub(testClub);
        bannedAndDeletedMember.setUser(testUser);
        bannedAndDeletedMember.setRole(ClubMember.MemberRole.BANNED);
        bannedAndDeletedMember.setDeletedAt(LocalDateTime.now().minusDays(1));

        when(inviteCodeService.getKeyId(TEST_INVITE_CODE)).thenReturn(TEST_KEY_ID);
        when(inviteUtilService.getClub(TEST_KEY_ID)).thenReturn(testClub);
        when(inviteUtilService.isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID)).thenReturn(false);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(clubMemberRepository.findByClubIdAndUserId(TEST_CLUB_ID, TEST_USER_ID))
                .thenReturn(Optional.of(bannedAndDeletedMember));

        // When & Then
        BannedMemberJoinAttemptException exception = assertThrows(BannedMemberJoinAttemptException.class,
                () -> joinService.joinClub(TEST_USER_ID, TEST_INVITE_CODE));

        assertEquals("강퇴된 사용자는 해당 동아리에 재가입할 수 없습니다.", exception.getMessage());

        verify(clubMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("2-6. Null 초대 코드로 가입 시도")
    void joinClub_ThrowsException_NullInviteCode() {
        // Given
        String nullInviteCode = null;
        when(inviteCodeService.getKeyId(nullInviteCode))
                .thenThrow(new IllegalArgumentException("초대 코드는 null일 수 없습니다"));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> joinService.joinClub(TEST_USER_ID, nullInviteCode));

        assertEquals("초대 코드는 null일 수 없습니다", exception.getMessage());

        verify(inviteCodeService).getKeyId(nullInviteCode);
        verify(inviteUtilService, never()).getClub(anyString());
        verify(clubMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("2-7. 빈 문자열 초대 코드로 가입 시도")
    void joinClub_ThrowsException_EmptyInviteCode() {
        // Given
        String emptyInviteCode = "";
        when(inviteCodeService.getKeyId(emptyInviteCode))
                .thenThrow(new IllegalArgumentException("초대 코드는 빈 문자열일 수 없습니다"));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> joinService.joinClub(TEST_USER_ID, emptyInviteCode));

        assertEquals("초대 코드는 빈 문자열일 수 없습니다", exception.getMessage());

        verify(inviteCodeService).getKeyId(emptyInviteCode);
        verify(inviteUtilService, never()).getClub(anyString());
        verify(clubMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("2-8. 공백문자만 포함된 초대 코드로 가입 시도")
    void joinClub_ThrowsException_WhitespaceOnlyInviteCode() {
        // Given
        String whitespaceInviteCode = "   ";
        when(inviteCodeService.getKeyId(whitespaceInviteCode))
                .thenThrow(new IllegalArgumentException("초대 코드는 공백문자만 포함할 수 없습니다"));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> joinService.joinClub(TEST_USER_ID, whitespaceInviteCode));

        assertEquals("초대 코드는 공백문자만 포함할 수 없습니다", exception.getMessage());

        verify(inviteCodeService).getKeyId(whitespaceInviteCode);
        verify(inviteUtilService, never()).getClub(anyString());
        verify(clubMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("2-9. 유효하지 않은 사용자 ID로 가입 시도 (0)")
    void joinClub_ThrowsException_InvalidUserId() {
        // Given
        Integer invalidUserId = 0;
        when(inviteCodeService.getKeyId(TEST_INVITE_CODE)).thenReturn(TEST_KEY_ID);
        when(inviteUtilService.getClub(TEST_KEY_ID)).thenReturn(testClub);
        when(inviteUtilService.isMemberOfClub(TEST_CLUB_ID, invalidUserId)).thenReturn(false);
        when(userRepository.findById(invalidUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class,
                () -> joinService.joinClub(invalidUserId, TEST_INVITE_CODE));

        verify(inviteCodeService).getKeyId(TEST_INVITE_CODE);
        verify(inviteUtilService).getClub(TEST_KEY_ID);
        verify(inviteUtilService).isMemberOfClub(TEST_CLUB_ID, invalidUserId);
        verify(userRepository).findById(invalidUserId);
        verify(clubMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("2-10. 음수 사용자 ID로 가입 시도")
    void joinClub_ThrowsException_NegativeUserId() {
        // Given
        Integer negativeUserId = -1;
        when(inviteCodeService.getKeyId(TEST_INVITE_CODE)).thenReturn(TEST_KEY_ID);
        when(inviteUtilService.getClub(TEST_KEY_ID)).thenReturn(testClub);
        when(inviteUtilService.isMemberOfClub(TEST_CLUB_ID, negativeUserId)).thenReturn(false);
        when(userRepository.findById(negativeUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class,
                () -> joinService.joinClub(negativeUserId, TEST_INVITE_CODE));

        verify(inviteCodeService).getKeyId(TEST_INVITE_CODE);
        verify(inviteUtilService).getClub(TEST_KEY_ID);
        verify(inviteUtilService).isMemberOfClub(TEST_CLUB_ID, negativeUserId);
        verify(userRepository).findById(negativeUserId);
        verify(clubMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("3-1. 데이터베이스 오류 발생 시")
    void joinClub_ThrowsException_DatabaseError() {
        // Given
        when(inviteCodeService.getKeyId(TEST_INVITE_CODE)).thenReturn(TEST_KEY_ID);
        when(inviteUtilService.getClub(TEST_KEY_ID)).thenReturn(testClub);
        when(inviteUtilService.isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID)).thenReturn(false);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(clubMemberRepository.findByClubIdAndUserId(TEST_CLUB_ID, TEST_USER_ID))
                .thenThrow(new DataAccessException("DB 오류") {});

        // When & Then
        assertThrows(DataAccessException.class,
                () -> joinService.joinClub(TEST_USER_ID, TEST_INVITE_CODE));

        verify(inviteCodeService).getKeyId(TEST_INVITE_CODE);
        verify(inviteUtilService).getClub(TEST_KEY_ID);
        verify(inviteUtilService).isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID);
        verify(userRepository).findById(TEST_USER_ID);
        verify(clubMemberRepository).findByClubIdAndUserId(TEST_CLUB_ID, TEST_USER_ID);
        verify(clubMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("3-2. InviteUtilService에서 클럽 조회 실패")
    void joinClub_ThrowsException_ClubNotFoundInUtilService() {
        // Given
        when(inviteCodeService.getKeyId(TEST_INVITE_CODE)).thenReturn(TEST_KEY_ID);
        when(inviteUtilService.getClub(TEST_KEY_ID))
                .thenThrow(new RuntimeException("동아리를 찾을 수 없습니다"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> joinService.joinClub(TEST_USER_ID, TEST_INVITE_CODE));

        assertEquals("동아리를 찾을 수 없습니다", exception.getMessage());

        verify(inviteCodeService).getKeyId(TEST_INVITE_CODE);
        verify(inviteUtilService).getClub(TEST_KEY_ID);
        verify(inviteUtilService, never()).isMemberOfClub(anyInt(), anyInt());
        verify(clubMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("4-1. 활성화된 멤버에서 재활성화 시도 (deletedAt이 null인 경우)")
    void joinClub_Success_ActiveMemberReactivation() {
        // Given
        ClubMember activeMember = new ClubMember();
        activeMember.setClub(testClub);
        activeMember.setUser(testUser);
        activeMember.setRole(ClubMember.MemberRole.MEMBER);
        activeMember.setDeletedAt(null); // 이미 활성화된 상태

        when(inviteCodeService.getKeyId(TEST_INVITE_CODE)).thenReturn(TEST_KEY_ID);
        when(inviteUtilService.getClub(TEST_KEY_ID)).thenReturn(testClub);
        when(inviteUtilService.isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID)).thenReturn(false);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(clubMemberRepository.findByClubIdAndUserId(TEST_CLUB_ID, TEST_USER_ID))
                .thenReturn(Optional.of(activeMember));

        // When
        JoinRespDTO result = joinService.joinClub(TEST_USER_ID, TEST_INVITE_CODE);

        // Then
        assertNotNull(result);
        assertEquals(TEST_CLUB_ID, result.getClubId());
        assertNull(result.getTeamId());

        // 활성화된 멤버는 재활성화 로직을 타지 않음 (deletedAt이 null이므로)
        verify(clubMemberRepository, never()).save(any());
    }
}
