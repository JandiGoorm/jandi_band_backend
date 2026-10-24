package com.jandi.band_backend.invite.service;

import com.jandi.band_backend.club.entity.Club;
import com.jandi.band_backend.club.entity.ClubMember;
import com.jandi.band_backend.club.repository.ClubMemberRepository;
import com.jandi.band_backend.global.exception.BannedMemberJoinAttemptException;
import com.jandi.band_backend.global.exception.InvalidAccessException;
import com.jandi.band_backend.global.exception.UserNotFoundException;
import com.jandi.band_backend.invite.dto.JoinRespDTO;
import com.jandi.band_backend.invite.store.InviteCodeService;
import com.jandi.band_backend.team.entity.Team;
import com.jandi.band_backend.team.entity.TeamMember;
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
@DisplayName("JoinService 팀 가입 테스트")
class JoinTeamServiceTest {

    @InjectMocks
    private JoinService joinService;

    @Mock private InviteCodeService inviteCodeService;
    @Mock private InviteUtilService inviteUtilService;
    @Mock private UserRepository userRepository;
    @Mock private ClubMemberRepository clubMemberRepository;
    @Mock private TeamMemberRepository teamMemberRepository;

    private Users testUser;
    private Club testClub;
    private Team testTeam;
    private final Integer TEST_USER_ID = 1;
    private final Integer TEST_CLUB_ID = 1;
    private final Integer TEST_TEAM_ID = 1;
    private final String TEST_INVITE_CODE = "XYZ789GHI";
    private final String TEST_KEY_ID = "team_1";

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

        testTeam = new Team();
        testTeam.setId(TEST_TEAM_ID);
        testTeam.setName("테스트팀");
        testTeam.setClub(testClub);
    }

    @Test
    @DisplayName("1-1. 동아리 멤버가 팀에 정상 가입")
    void joinTeam_Success_ExistingClubMember() {
        // Given
        when(inviteCodeService.getKeyId(TEST_INVITE_CODE)).thenReturn(TEST_KEY_ID);
        when(inviteUtilService.getTeam(TEST_KEY_ID)).thenReturn(testTeam);
        when(inviteUtilService.isMemberOfTeam(TEST_TEAM_ID, TEST_USER_ID)).thenReturn(false);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(inviteUtilService.isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID)).thenReturn(true);
        when(teamMemberRepository.findByTeamIdAndUserId(TEST_TEAM_ID, TEST_USER_ID))
                .thenReturn(Optional.empty());

        // When
        JoinRespDTO result = joinService.joinTeam(TEST_USER_ID, TEST_INVITE_CODE);

        // Then
        assertNotNull(result);
        assertEquals(TEST_CLUB_ID, result.getClubId());
        assertEquals(TEST_TEAM_ID, result.getTeamId());

        verify(inviteCodeService).getKeyId(TEST_INVITE_CODE);
        verify(inviteUtilService).getTeam(TEST_KEY_ID);
        verify(inviteUtilService).isMemberOfTeam(TEST_TEAM_ID, TEST_USER_ID);
        verify(userRepository).findById(TEST_USER_ID);
        verify(inviteUtilService).isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID);
        verify(teamMemberRepository).findByTeamIdAndUserId(TEST_TEAM_ID, TEST_USER_ID);
        verify(teamMemberRepository).save(any(TeamMember.class));
        verify(clubMemberRepository, never()).save(any()); // 이미 동아리 멤버이므로 동아리 가입 처리 안함
    }

    @Test
    @DisplayName("1-2. 동아리 미가입자가 팀 가입 시 동아리 + 팀 동시 가입")
    void joinTeam_Success_NewUserJoinsBothClubAndTeam() {
        // Given
        when(inviteCodeService.getKeyId(TEST_INVITE_CODE)).thenReturn(TEST_KEY_ID);
        when(inviteUtilService.getTeam(TEST_KEY_ID)).thenReturn(testTeam);
        when(inviteUtilService.isMemberOfTeam(TEST_TEAM_ID, TEST_USER_ID)).thenReturn(false);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(inviteUtilService.isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID)).thenReturn(false);
        when(clubMemberRepository.findByClubIdAndUserId(TEST_CLUB_ID, TEST_USER_ID))
                .thenReturn(Optional.empty());
        when(teamMemberRepository.findByTeamIdAndUserId(TEST_TEAM_ID, TEST_USER_ID))
                .thenReturn(Optional.empty());

        // When
        JoinRespDTO result = joinService.joinTeam(TEST_USER_ID, TEST_INVITE_CODE);

        // Then
        assertNotNull(result);
        assertEquals(TEST_CLUB_ID, result.getClubId());
        assertEquals(TEST_TEAM_ID, result.getTeamId());

        verify(inviteUtilService).isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID);
        verify(clubMemberRepository).findByClubIdAndUserId(TEST_CLUB_ID, TEST_USER_ID);
        verify(clubMemberRepository).save(any(ClubMember.class)); // 동아리 가입 처리
        verify(teamMemberRepository).save(any(TeamMember.class)); // 팀 가입 처리
    }

    @Test
    @DisplayName("1-3. 소프트 삭제된 팀 멤버가 팀에 재가입 (재활성화)")
    void joinTeam_Success_ReactivateSoftDeletedTeamMember() {
        // Given
        TeamMember deletedTeamMember = new TeamMember();
        deletedTeamMember.setTeam(testTeam);
        deletedTeamMember.setUser(testUser);
        deletedTeamMember.setDeletedAt(LocalDateTime.now().minusDays(1));

        when(inviteCodeService.getKeyId(TEST_INVITE_CODE)).thenReturn(TEST_KEY_ID);
        when(inviteUtilService.getTeam(TEST_KEY_ID)).thenReturn(testTeam);
        when(inviteUtilService.isMemberOfTeam(TEST_TEAM_ID, TEST_USER_ID)).thenReturn(false);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(inviteUtilService.isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID)).thenReturn(true);
        when(teamMemberRepository.findByTeamIdAndUserId(TEST_TEAM_ID, TEST_USER_ID))
                .thenReturn(Optional.of(deletedTeamMember));

        // When
        JoinRespDTO result = joinService.joinTeam(TEST_USER_ID, TEST_INVITE_CODE);

        // Then
        assertNotNull(result);
        assertEquals(TEST_CLUB_ID, result.getClubId());
        assertEquals(TEST_TEAM_ID, result.getTeamId());

        // 재활성화 검증
        assertNull(deletedTeamMember.getDeletedAt());

        verify(teamMemberRepository).save(deletedTeamMember);
        verify(teamMemberRepository, never()).save(argThat(member -> member != deletedTeamMember));
        verify(clubMemberRepository, never()).save(any()); // 이미 동아리 멤버이므로 동아리 가입 처리 안함
    }

    @Test
    @DisplayName("1-4. 동아리 탈퇴 + 팀 탈퇴 상태에서 팀 재가입 시 동아리 + 팀 동시 재활성화")
    void joinTeam_Success_ReactivateBothClubAndTeamMember() {
        // Given
        ClubMember deletedClubMember = new ClubMember();
        deletedClubMember.setClub(testClub);
        deletedClubMember.setUser(testUser);
        deletedClubMember.setRole(ClubMember.MemberRole.MEMBER);
        deletedClubMember.setDeletedAt(LocalDateTime.now().minusDays(1));

        TeamMember deletedTeamMember = new TeamMember();
        deletedTeamMember.setTeam(testTeam);
        deletedTeamMember.setUser(testUser);
        deletedTeamMember.setDeletedAt(LocalDateTime.now().minusDays(1));

        LocalDateTime testStartTime = LocalDateTime.now();

        when(inviteCodeService.getKeyId(TEST_INVITE_CODE)).thenReturn(TEST_KEY_ID);
        when(inviteUtilService.getTeam(TEST_KEY_ID)).thenReturn(testTeam);
        when(inviteUtilService.isMemberOfTeam(TEST_TEAM_ID, TEST_USER_ID)).thenReturn(false);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(inviteUtilService.isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID)).thenReturn(false);
        when(clubMemberRepository.findByClubIdAndUserId(TEST_CLUB_ID, TEST_USER_ID))
                .thenReturn(Optional.of(deletedClubMember));
        when(teamMemberRepository.findByTeamIdAndUserId(TEST_TEAM_ID, TEST_USER_ID))
                .thenReturn(Optional.of(deletedTeamMember));

        // When
        JoinRespDTO result = joinService.joinTeam(TEST_USER_ID, TEST_INVITE_CODE);

        // Then
        assertNotNull(result);
        assertEquals(TEST_CLUB_ID, result.getClubId());
        assertEquals(TEST_TEAM_ID, result.getTeamId());

        // 동아리 재활성화 검증
        assertNull(deletedClubMember.getDeletedAt());
        assertEquals(ClubMember.MemberRole.MEMBER, deletedClubMember.getRole());
        assertNotNull(deletedClubMember.getUpdatedAt());
        assertTrue(deletedClubMember.getUpdatedAt().isAfter(testStartTime) ||
                   deletedClubMember.getUpdatedAt().isEqual(testStartTime));

        // 팀 재활성화 검증
        assertNull(deletedTeamMember.getDeletedAt());

        verify(clubMemberRepository).save(deletedClubMember);
        verify(teamMemberRepository).save(deletedTeamMember);
    }

    @Test
    @DisplayName("2-1. 존재하지 않는 사용자 ID로 가입 시도")
    void joinTeam_ThrowsException_UserNotFound() {
        // Given
        when(inviteCodeService.getKeyId(TEST_INVITE_CODE)).thenReturn(TEST_KEY_ID);
        when(inviteUtilService.getTeam(TEST_KEY_ID)).thenReturn(testTeam);
        when(inviteUtilService.isMemberOfTeam(TEST_TEAM_ID, TEST_USER_ID)).thenReturn(false);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class,
                () -> joinService.joinTeam(TEST_USER_ID, TEST_INVITE_CODE));

        verify(inviteCodeService).getKeyId(TEST_INVITE_CODE);
        verify(inviteUtilService).getTeam(TEST_KEY_ID);
        verify(inviteUtilService).isMemberOfTeam(TEST_TEAM_ID, TEST_USER_ID);
        verify(userRepository).findById(TEST_USER_ID);
        verify(clubMemberRepository, never()).save(any());
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("2-2. 유효하지 않은 초대 코드로 가입 시도")
    void joinTeam_ThrowsException_InvalidInviteCode() {
        // Given
        when(inviteCodeService.getKeyId(TEST_INVITE_CODE))
                .thenThrow(new RuntimeException("유효하지 않은 초대 코드"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> joinService.joinTeam(TEST_USER_ID, TEST_INVITE_CODE));

        assertEquals("유효하지 않은 초대 코드", exception.getMessage());

        verify(inviteCodeService).getKeyId(TEST_INVITE_CODE);
        verify(inviteUtilService, never()).getTeam(anyString());
        verify(clubMemberRepository, never()).save(any());
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("2-3. 이미 활성화된 팀 멤버가 재가입 시도")
    void joinTeam_ThrowsException_AlreadyActiveTeamMember() {
        // Given
        when(inviteCodeService.getKeyId(TEST_INVITE_CODE)).thenReturn(TEST_KEY_ID);
        when(inviteUtilService.getTeam(TEST_KEY_ID)).thenReturn(testTeam);
        when(inviteUtilService.isMemberOfTeam(TEST_TEAM_ID, TEST_USER_ID)).thenReturn(true);

        // When & Then
        InvalidAccessException exception = assertThrows(InvalidAccessException.class,
                () -> joinService.joinTeam(TEST_USER_ID, TEST_INVITE_CODE));

        assertEquals("이미 가입한 팀입니다", exception.getMessage());

        verify(inviteCodeService).getKeyId(TEST_INVITE_CODE);
        verify(inviteUtilService).getTeam(TEST_KEY_ID);
        verify(inviteUtilService).isMemberOfTeam(TEST_TEAM_ID, TEST_USER_ID);
        verify(userRepository, never()).findById(anyInt());
        verify(clubMemberRepository, never()).save(any());
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("2-4. 동아리에서 강퇴된 사용자가 팀 가입 시도")
    void joinTeam_ThrowsException_BannedUserFromClubTriesToJoinTeam() {
        // Given
        ClubMember bannedClubMember = new ClubMember();
        bannedClubMember.setClub(testClub);
        bannedClubMember.setUser(testUser);
        bannedClubMember.setRole(ClubMember.MemberRole.BANNED);

        when(inviteCodeService.getKeyId(TEST_INVITE_CODE)).thenReturn(TEST_KEY_ID);
        when(inviteUtilService.getTeam(TEST_KEY_ID)).thenReturn(testTeam);
        when(inviteUtilService.isMemberOfTeam(TEST_TEAM_ID, TEST_USER_ID)).thenReturn(false);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(inviteUtilService.isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID)).thenReturn(false);
        when(clubMemberRepository.findByClubIdAndUserId(TEST_CLUB_ID, TEST_USER_ID))
                .thenReturn(Optional.of(bannedClubMember));

        // When & Then
        BannedMemberJoinAttemptException exception = assertThrows(BannedMemberJoinAttemptException.class,
                () -> joinService.joinTeam(TEST_USER_ID, TEST_INVITE_CODE));

        assertEquals("강퇴된 사용자는 해당 동아리에 재가입할 수 없습니다.", exception.getMessage());

        verify(inviteCodeService).getKeyId(TEST_INVITE_CODE);
        verify(inviteUtilService).getTeam(TEST_KEY_ID);
        verify(inviteUtilService).isMemberOfTeam(TEST_TEAM_ID, TEST_USER_ID);
        verify(userRepository).findById(TEST_USER_ID);
        verify(inviteUtilService).isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID);
        verify(clubMemberRepository).findByClubIdAndUserId(TEST_CLUB_ID, TEST_USER_ID);
        verify(clubMemberRepository, never()).save(any());
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("2-5. 동아리 강퇴 + 팀 탈퇴 상태에서 팀 가입 시도")
    void joinTeam_ThrowsException_BannedFromClubAndDeletedFromTeam() {
        // Given
        ClubMember bannedClubMember = new ClubMember();
        bannedClubMember.setClub(testClub);
        bannedClubMember.setUser(testUser);
        bannedClubMember.setRole(ClubMember.MemberRole.BANNED);
        bannedClubMember.setDeletedAt(LocalDateTime.now().minusDays(1));

        when(inviteCodeService.getKeyId(TEST_INVITE_CODE)).thenReturn(TEST_KEY_ID);
        when(inviteUtilService.getTeam(TEST_KEY_ID)).thenReturn(testTeam);
        when(inviteUtilService.isMemberOfTeam(TEST_TEAM_ID, TEST_USER_ID)).thenReturn(false);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(inviteUtilService.isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID)).thenReturn(false);
        when(clubMemberRepository.findByClubIdAndUserId(TEST_CLUB_ID, TEST_USER_ID))
                .thenReturn(Optional.of(bannedClubMember));

        // When & Then
        BannedMemberJoinAttemptException exception = assertThrows(BannedMemberJoinAttemptException.class,
                () -> joinService.joinTeam(TEST_USER_ID, TEST_INVITE_CODE));

        assertEquals("강퇴된 사용자는 해당 동아리에 재가입할 수 없습니다.", exception.getMessage());

        verify(clubMemberRepository, never()).save(any());
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("2-6. Null 초대 코드로 팀 가입 시도")
    void joinTeam_ThrowsException_NullInviteCode() {
        // Given
        String nullInviteCode = null;
        when(inviteCodeService.getKeyId(nullInviteCode))
                .thenThrow(new IllegalArgumentException("초대 코드는 null일 수 없습니다"));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> joinService.joinTeam(TEST_USER_ID, nullInviteCode));

        assertEquals("초대 코드는 null일 수 없습니다", exception.getMessage());

        verify(inviteCodeService).getKeyId(nullInviteCode);
        verify(inviteUtilService, never()).getTeam(anyString());
        verify(clubMemberRepository, never()).save(any());
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("2-7. 빈 문자열 초대 코드로 팀 가입 시도")
    void joinTeam_ThrowsException_EmptyInviteCode() {
        // Given
        String emptyInviteCode = "";
        when(inviteCodeService.getKeyId(emptyInviteCode))
                .thenThrow(new IllegalArgumentException("초대 코드는 빈 문자열일 수 없습니다"));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> joinService.joinTeam(TEST_USER_ID, emptyInviteCode));

        assertEquals("초대 코드는 빈 문자열일 수 없습니다", exception.getMessage());

        verify(inviteCodeService).getKeyId(emptyInviteCode);
        verify(inviteUtilService, never()).getTeam(anyString());
        verify(clubMemberRepository, never()).save(any());
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("2-8. 유효하지 않은 사용자 ID로 팀 가입 시도 (0 이하)")
    void joinTeam_ThrowsException_InvalidUserId() {
        // Given
        Integer invalidUserId = 0;
        when(inviteCodeService.getKeyId(TEST_INVITE_CODE)).thenReturn(TEST_KEY_ID);
        when(inviteUtilService.getTeam(TEST_KEY_ID)).thenReturn(testTeam);
        when(inviteUtilService.isMemberOfTeam(TEST_TEAM_ID, invalidUserId)).thenReturn(false);
        when(userRepository.findById(invalidUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class,
                () -> joinService.joinTeam(invalidUserId, TEST_INVITE_CODE));

        verify(inviteCodeService).getKeyId(TEST_INVITE_CODE);
        verify(inviteUtilService).getTeam(TEST_KEY_ID);
        verify(inviteUtilService).isMemberOfTeam(TEST_TEAM_ID, invalidUserId);
        verify(userRepository).findById(invalidUserId);
        verify(clubMemberRepository, never()).save(any());
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("2-9. 음수 사용자 ID로 팀 가입 시도")
    void joinTeam_ThrowsException_NegativeUserId() {
        // Given
        Integer negativeUserId = -1;
        when(inviteCodeService.getKeyId(TEST_INVITE_CODE)).thenReturn(TEST_KEY_ID);
        when(inviteUtilService.getTeam(TEST_KEY_ID)).thenReturn(testTeam);
        when(inviteUtilService.isMemberOfTeam(TEST_TEAM_ID, negativeUserId)).thenReturn(false);
        when(userRepository.findById(negativeUserId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class,
                () -> joinService.joinTeam(negativeUserId, TEST_INVITE_CODE));

        verify(inviteCodeService).getKeyId(TEST_INVITE_CODE);
        verify(inviteUtilService).getTeam(TEST_KEY_ID);
        verify(inviteUtilService).isMemberOfTeam(TEST_TEAM_ID, negativeUserId);
        verify(userRepository).findById(negativeUserId);
        verify(clubMemberRepository, never()).save(any());
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("2-10. 상태 일관성 검증 - 동아리와 팀 멤버십 상태 확인")
    void joinTeam_Success_ConsistentStateValidation() {
        // Given
        when(inviteCodeService.getKeyId(TEST_INVITE_CODE)).thenReturn(TEST_KEY_ID);
        when(inviteUtilService.getTeam(TEST_KEY_ID)).thenReturn(testTeam);
        when(inviteUtilService.isMemberOfTeam(TEST_TEAM_ID, TEST_USER_ID)).thenReturn(false);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(inviteUtilService.isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID)).thenReturn(true);
        when(teamMemberRepository.findByTeamIdAndUserId(TEST_TEAM_ID, TEST_USER_ID))
                .thenReturn(Optional.empty());

        // When
        JoinRespDTO result = joinService.joinTeam(TEST_USER_ID, TEST_INVITE_CODE);

        // Then
        assertNotNull(result);
        assertEquals(TEST_CLUB_ID, result.getClubId());
        assertEquals(TEST_TEAM_ID, result.getTeamId());

        // 상태 일관성 검증: 동아리 멤버이므로 팀만 가입되어야 함
        verify(inviteUtilService).isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID);
        verify(clubMemberRepository, never()).save(any()); // 동아리는 이미 가입되어 있음
        verify(teamMemberRepository).save(any(TeamMember.class)); // 팀만 새로 가입

        // 팀 가입 후 결과가 동아리와 팀 정보를 모두 포함하는지 확인
        assertTrue(result.getClubId().equals(testTeam.getClub().getId()));
        assertTrue(result.getTeamId().equals(testTeam.getId()));
    }

    @Test
    @DisplayName("2-11. 활성화된 팀 멤버에서 재활성화 시도 (deletedAt이 null인 경우)")
    void joinTeam_Success_ActiveTeamMemberReactivation() {
        // Given
        TeamMember activeTeamMember = new TeamMember();
        activeTeamMember.setTeam(testTeam);
        activeTeamMember.setUser(testUser);
        activeTeamMember.setDeletedAt(null); // 이미 활성화된 상태

        when(inviteCodeService.getKeyId(TEST_INVITE_CODE)).thenReturn(TEST_KEY_ID);
        when(inviteUtilService.getTeam(TEST_KEY_ID)).thenReturn(testTeam);
        when(inviteUtilService.isMemberOfTeam(TEST_TEAM_ID, TEST_USER_ID)).thenReturn(false);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(inviteUtilService.isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID)).thenReturn(true);
        when(teamMemberRepository.findByTeamIdAndUserId(TEST_TEAM_ID, TEST_USER_ID))
                .thenReturn(Optional.of(activeTeamMember));

        // When
        JoinRespDTO result = joinService.joinTeam(TEST_USER_ID, TEST_INVITE_CODE);

        // Then
        assertNotNull(result);
        assertEquals(TEST_CLUB_ID, result.getClubId());
        assertEquals(TEST_TEAM_ID, result.getTeamId());

        // 활성화된 멤버는 재활성화 로직을 타지 않음 (deletedAt이 null이므로)
        verify(teamMemberRepository, never()).save(any());
        verify(clubMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("2-12. 동아리 활성 멤버 + 팀 신규 가입")
    void joinTeam_Success_ActiveClubMemberJoinsNewTeam() {
        // Given
        when(inviteCodeService.getKeyId(TEST_INVITE_CODE)).thenReturn(TEST_KEY_ID);
        when(inviteUtilService.getTeam(TEST_KEY_ID)).thenReturn(testTeam);
        when(inviteUtilService.isMemberOfTeam(TEST_TEAM_ID, TEST_USER_ID)).thenReturn(false);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(inviteUtilService.isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID)).thenReturn(true);
        when(teamMemberRepository.findByTeamIdAndUserId(TEST_TEAM_ID, TEST_USER_ID))
                .thenReturn(Optional.empty());

        // When
        JoinRespDTO result = joinService.joinTeam(TEST_USER_ID, TEST_INVITE_CODE);

        // Then
        assertNotNull(result);
        assertEquals(TEST_CLUB_ID, result.getClubId());
        assertEquals(TEST_TEAM_ID, result.getTeamId());

        // 동아리는 이미 가입되어 있으므로 처리하지 않고, 팀만 새로 가입
        verify(clubMemberRepository, never()).save(any());
        verify(teamMemberRepository).save(any(TeamMember.class));
    }

    @Test
    @DisplayName("3-1. 데이터베이스 오류 발생 시 (팀 멤버 조회 실패)")
    void joinTeam_ThrowsException_DatabaseErrorOnTeamMemberQuery() {
        // Given
        when(inviteCodeService.getKeyId(TEST_INVITE_CODE)).thenReturn(TEST_KEY_ID);
        when(inviteUtilService.getTeam(TEST_KEY_ID)).thenReturn(testTeam);
        when(inviteUtilService.isMemberOfTeam(TEST_TEAM_ID, TEST_USER_ID)).thenReturn(false);
        when(userRepository.findById(TEST_USER_ID)).thenReturn(Optional.of(testUser));
        when(inviteUtilService.isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID)).thenReturn(true);
        when(teamMemberRepository.findByTeamIdAndUserId(TEST_TEAM_ID, TEST_USER_ID))
                .thenThrow(new DataAccessException("DB 오류") {});

        // When & Then
        assertThrows(DataAccessException.class,
                () -> joinService.joinTeam(TEST_USER_ID, TEST_INVITE_CODE));

        verify(inviteCodeService).getKeyId(TEST_INVITE_CODE);
        verify(inviteUtilService).getTeam(TEST_KEY_ID);
        verify(inviteUtilService).isMemberOfTeam(TEST_TEAM_ID, TEST_USER_ID);
        verify(userRepository).findById(TEST_USER_ID);
        verify(inviteUtilService).isMemberOfClub(TEST_CLUB_ID, TEST_USER_ID);
        verify(teamMemberRepository).findByTeamIdAndUserId(TEST_TEAM_ID, TEST_USER_ID);
        verify(clubMemberRepository, never()).save(any());
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("3-2. InviteUtilService에서 팀 조회 실패")
    void joinTeam_ThrowsException_TeamNotFoundInUtilService() {
        // Given
        when(inviteCodeService.getKeyId(TEST_INVITE_CODE)).thenReturn(TEST_KEY_ID);
        when(inviteUtilService.getTeam(TEST_KEY_ID))
                .thenThrow(new RuntimeException("팀을 찾을 수 없습니다"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> joinService.joinTeam(TEST_USER_ID, TEST_INVITE_CODE));

        assertEquals("팀을 찾을 수 없습니다", exception.getMessage());

        verify(inviteCodeService).getKeyId(TEST_INVITE_CODE);
        verify(inviteUtilService).getTeam(TEST_KEY_ID);
        verify(inviteUtilService, never()).isMemberOfTeam(anyInt(), anyInt());
        verify(clubMemberRepository, never()).save(any());
        verify(teamMemberRepository, never()).save(any());
    }
}
