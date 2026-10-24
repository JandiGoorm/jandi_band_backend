package com.jandi.band_backend.team.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.jandi.band_backend.global.exception.BadRequestException;
import com.jandi.band_backend.global.exception.TimetableNotFoundException;
import com.jandi.band_backend.global.util.EntityValidationUtil;
import com.jandi.band_backend.global.util.PermissionValidationUtil;
import com.jandi.band_backend.global.util.TimetableValidationUtil;
import com.jandi.band_backend.team.dto.ScheduleSuggestionRespDTO;
import com.jandi.band_backend.team.dto.TimetableReqDTO;
import com.jandi.band_backend.team.dto.TimetableRespDTO;
import com.jandi.band_backend.team.entity.Team;
import com.jandi.band_backend.team.entity.TeamMember;
import com.jandi.band_backend.team.repository.TeamMemberRepository;
import com.jandi.band_backend.team.repository.TeamRepository;
import com.jandi.band_backend.user.entity.UserTimetable;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserTimetableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamTimetableService 스케줄 조율 및 등록 테스트")
class TeamTimetableServiceTest1 {

    @InjectMocks
    private TeamTimetableService teamTimetableService;

    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private UserTimetableRepository userTimetableRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private EntityValidationUtil entityValidationUtil;
    @Mock private PermissionValidationUtil permissionValidationUtil;
    @Mock private TimetableValidationUtil timetableValidationUtil;

    private Users testUser;
    private Users otherUser;
    private Team testTeam;
    private TeamMember testTeamMember;
    private UserTimetable testUserTimetable;
    private TimetableReqDTO validReqDTO;
    private JsonNode testTimetableData;

    private final Integer TEST_USER_ID = 1;
    private final Integer OTHER_USER_ID = 2;
    private final Integer TEST_TEAM_ID = 1;
    private final Integer TEST_TIMETABLE_ID = 1;
    private final String TIMETABLE_JSON = """
        {
          "Mon": ["09:00", "10:00"],
          "Tue": ["14:00", "15:00"],
          "Wed": ["09:00", "10:00"],
          "Thu": [],
          "Fri": ["13:00", "14:00"],
          "Sat": [],
          "Sun": []
        }
        """;

    @BeforeEach
    void setUp() throws Exception {
        testUser = new Users();
        testUser.setId(TEST_USER_ID);
        testUser.setNickname("테스트사용자");

        otherUser = new Users();
        otherUser.setId(OTHER_USER_ID);
        otherUser.setNickname("다른사용자");

        testTeam = new Team();
        testTeam.setId(TEST_TEAM_ID);
        testTeam.setName("테스트팀");

        testTeamMember = new TeamMember();
        testTeamMember.setId(1);
        testTeamMember.setTeam(testTeam);
        testTeamMember.setUser(testUser);

        testUserTimetable = new UserTimetable();
        testUserTimetable.setId(TEST_TIMETABLE_ID);
        testUserTimetable.setUser(testUser);
        testUserTimetable.setName("내 시간표");
        testUserTimetable.setTimetableData(TIMETABLE_JSON);

        ObjectMapper realObjectMapper = tools.jackson.databind.json.JsonMapper.builder().build();
        testTimetableData = realObjectMapper.readTree(TIMETABLE_JSON);

        validReqDTO = new TimetableReqDTO();
        validReqDTO.setUserTimetableId(TEST_TIMETABLE_ID);
    }

    // ================ startScheduleSuggestion 테스트 ================

    @Test
    @DisplayName("1-1. 팀원이 스케줄 조율 제안 정상 시작")
    void startScheduleSuggestion_Success() {
        // Given
        when(entityValidationUtil.validateTeamExists(TEST_TEAM_ID)).thenReturn(testTeam);
        when(permissionValidationUtil.validateTeamMemberAccess(TEST_TEAM_ID, TEST_USER_ID, "팀원만 접근할 수 있습니다."))
                .thenReturn(testTeamMember);
        when(teamRepository.save(testTeam)).thenReturn(testTeam);

        // When
        ScheduleSuggestionRespDTO result = teamTimetableService.startScheduleSuggestion(TEST_TEAM_ID, TEST_USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_TEAM_ID, result.getTeamId());
        assertEquals(TEST_USER_ID, result.getSuggesterUserId());
        assertEquals("테스트사용자", result.getSuggesterName());
        assertNotNull(result.getSuggestedScheduleAt());
        assertNotNull(testTeam.getSuggestedScheduleAt());

        verify(entityValidationUtil).validateTeamExists(TEST_TEAM_ID);
        verify(permissionValidationUtil).validateTeamMemberAccess(TEST_TEAM_ID, TEST_USER_ID, "팀원만 접근할 수 있습니다.");
        verify(teamRepository).save(testTeam);
    }

    @Test
    @DisplayName("1-2. 존재하지 않는 팀에 대한 스케줄 조율 제안 시 예외 발생")
    void startScheduleSuggestion_ThrowsException_TeamNotFound() {
        // Given
        when(entityValidationUtil.validateTeamExists(999))
                .thenThrow(new RuntimeException("존재하지 않는 팀입니다."));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> teamTimetableService.startScheduleSuggestion(999, TEST_USER_ID));

        assertEquals("존재하지 않는 팀입니다.", exception.getMessage());

        verify(entityValidationUtil).validateTeamExists(999);
        verify(permissionValidationUtil, never()).validateTeamMemberAccess(anyInt(), anyInt(), anyString());
        verify(teamRepository, never()).save(any());
    }

    @Test
    @DisplayName("1-3. 팀원이 아닌 사용자의 스케줄 조율 제안 시 예외 발생")
    void startScheduleSuggestion_ThrowsException_NotTeamMember() {
        // Given
        when(entityValidationUtil.validateTeamExists(TEST_TEAM_ID)).thenReturn(testTeam);
        when(permissionValidationUtil.validateTeamMemberAccess(TEST_TEAM_ID, OTHER_USER_ID, "팀원만 접근할 수 있습니다."))
                .thenThrow(new RuntimeException("팀원만 접근할 수 있습니다."));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> teamTimetableService.startScheduleSuggestion(TEST_TEAM_ID, OTHER_USER_ID));

        assertEquals("팀원만 접근할 수 있습니다.", exception.getMessage());

        verify(entityValidationUtil).validateTeamExists(TEST_TEAM_ID);
        verify(permissionValidationUtil).validateTeamMemberAccess(TEST_TEAM_ID, OTHER_USER_ID, "팀원만 접근할 수 있습니다.");
        verify(teamRepository, never()).save(any());
    }

    // ================ registerMyTimetable 테스트 ================

    @Test
    @DisplayName("2-1. 본인의 개인 시간표를 팀 시간표로 정상 등록")
    void registerMyTimetable_Success() throws JacksonException {
        // Given
        when(entityValidationUtil.validateTeamExists(TEST_TEAM_ID)).thenReturn(testTeam);
        when(permissionValidationUtil.validateTeamMemberAccess(TEST_TEAM_ID, TEST_USER_ID, "본인의 시간표만 입력할 수 있습니다."))
                .thenReturn(testTeamMember);
        when(userTimetableRepository.findByIdWithUserAndDeletedAtIsNull(TEST_TIMETABLE_ID))
                .thenReturn(Optional.of(testUserTimetable));
        doNothing().when(permissionValidationUtil).validateContentOwnership(TEST_USER_ID, TEST_USER_ID, "권한이 없습니다: 본인의 시간표가 아닙니다");
        when(timetableValidationUtil.stringToJson(TIMETABLE_JSON)).thenReturn(testTimetableData);
        when(objectMapper.writeValueAsString(testTimetableData)).thenReturn(TIMETABLE_JSON);
        when(teamMemberRepository.save(testTeamMember)).thenReturn(testTeamMember);

        // When
        TimetableRespDTO result = teamTimetableService.registerMyTimetable(TEST_TEAM_ID, validReqDTO, TEST_USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals(TEST_TEAM_ID, result.getTeamId());
        assertEquals(testTimetableData, result.getTimetableData());
        assertNotNull(result.getUpdatedTimetableAt());

        // 팀 멤버의 시간표 데이터가 업데이트되었는지 확인
        assertEquals(TIMETABLE_JSON, testTeamMember.getTimetableData());
        assertNotNull(testTeamMember.getUpdatedTimetableAt());

        verify(entityValidationUtil).validateTeamExists(TEST_TEAM_ID);
        verify(permissionValidationUtil).validateTeamMemberAccess(TEST_TEAM_ID, TEST_USER_ID, "본인의 시간표만 입력할 수 있습니다.");
        verify(userTimetableRepository).findByIdWithUserAndDeletedAtIsNull(TEST_TIMETABLE_ID);
        verify(timetableValidationUtil).stringToJson(TIMETABLE_JSON);
        verify(objectMapper).writeValueAsString(testTimetableData);
        verify(teamMemberRepository).save(testTeamMember);
    }

    @Test
    @DisplayName("2-2. 시간표 ID 누락 시 예외 발생")
    void registerMyTimetable_ThrowsException_NullTimetableId() {
        // Given
        TimetableReqDTO reqWithNullId = new TimetableReqDTO();
        reqWithNullId.setUserTimetableId(null);

        when(entityValidationUtil.validateTeamExists(TEST_TEAM_ID)).thenReturn(testTeam);
        when(permissionValidationUtil.validateTeamMemberAccess(TEST_TEAM_ID, TEST_USER_ID, "본인의 시간표만 입력할 수 있습니다."))
                .thenReturn(testTeamMember);

        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> teamTimetableService.registerMyTimetable(TEST_TEAM_ID, reqWithNullId, TEST_USER_ID));

        assertEquals("시간표 ID는 필수입니다.", exception.getMessage());

        verify(entityValidationUtil).validateTeamExists(TEST_TEAM_ID);
        verify(permissionValidationUtil).validateTeamMemberAccess(TEST_TEAM_ID, TEST_USER_ID, "본인의 시간표만 입력할 수 있습니다.");
        verify(userTimetableRepository, never()).findByIdWithUserAndDeletedAtIsNull(anyInt());
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("2-3. 존재하지 않는 시간표 등록 시 예외 발생")
    void registerMyTimetable_ThrowsException_TimetableNotFound() {
        // Given
        when(entityValidationUtil.validateTeamExists(TEST_TEAM_ID)).thenReturn(testTeam);
        when(permissionValidationUtil.validateTeamMemberAccess(TEST_TEAM_ID, TEST_USER_ID, "본인의 시간표만 입력할 수 있습니다."))
                .thenReturn(testTeamMember);
        when(userTimetableRepository.findByIdWithUserAndDeletedAtIsNull(TEST_TIMETABLE_ID))
                .thenReturn(Optional.empty());

        // When & Then
        TimetableNotFoundException exception = assertThrows(TimetableNotFoundException.class,
                () -> teamTimetableService.registerMyTimetable(TEST_TEAM_ID, validReqDTO, TEST_USER_ID));

        assertEquals("존재하지 않는 시간표입니다.", exception.getMessage());

        verify(entityValidationUtil).validateTeamExists(TEST_TEAM_ID);
        verify(permissionValidationUtil).validateTeamMemberAccess(TEST_TEAM_ID, TEST_USER_ID, "본인의 시간표만 입력할 수 있습니다.");
        verify(userTimetableRepository).findByIdWithUserAndDeletedAtIsNull(TEST_TIMETABLE_ID);
        verify(teamMemberRepository, never()).save(any());
    }
}
