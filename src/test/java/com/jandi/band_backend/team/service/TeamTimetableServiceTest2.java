package com.jandi.band_backend.team.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.jandi.band_backend.global.exception.BadRequestException;
import com.jandi.band_backend.global.util.EntityValidationUtil;
import com.jandi.band_backend.global.util.PermissionValidationUtil;
import com.jandi.band_backend.team.dto.TimetableUpdateReqDTO;
import com.jandi.band_backend.team.dto.TimetableRespDTO;
import com.jandi.band_backend.team.entity.Team;
import com.jandi.band_backend.team.entity.TeamMember;
import com.jandi.band_backend.team.repository.TeamMemberRepository;
import com.jandi.band_backend.team.util.TeamTimetableUtil;
import com.jandi.band_backend.user.entity.Users;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TeamTimetableService 수정 및 검증 테스트")
class TeamTimetableServiceTest2 {

    @InjectMocks
    private TeamTimetableService teamTimetableService;

    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private TeamTimetableUtil teamTimetableUtil;
    @Mock private EntityValidationUtil entityValidationUtil;
    @Mock private PermissionValidationUtil permissionValidationUtil;

    private Users testUser;
    private Users otherUser;
    private Team testTeam;
    private TeamMember testTeamMember;
    private TimetableUpdateReqDTO validUpdateReqDTO;
    private JsonNode validTimetableData;

    private final Integer TEST_USER_ID = 1;
    private final Integer OTHER_USER_ID = 2;
    private final Integer TEST_TEAM_ID = 1;
    private final String VALID_TIMETABLE_JSON = """
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

        ObjectMapper realObjectMapper = tools.jackson.databind.json.JsonMapper.builder().build();
        validTimetableData = realObjectMapper.readTree(VALID_TIMETABLE_JSON);

        validUpdateReqDTO = new TimetableUpdateReqDTO();
        validUpdateReqDTO.setTimetableData(validTimetableData);
    }

    // ================ updateMyTimetable 테스트 ================

    @Test
    @DisplayName("3-1. 팀원이 자신의 팀 시간표 정상 수정")
    void updateMyTimetable_Success() throws JacksonException {
        // Given
        when(entityValidationUtil.validateTeamExists(TEST_TEAM_ID)).thenReturn(testTeam);
        when(permissionValidationUtil.validateTeamMemberAccess(TEST_TEAM_ID, TEST_USER_ID, "본인의 시간표만 입력할 수 있습니다."))
                .thenReturn(testTeamMember);
        doNothing().when(teamTimetableUtil).validateTimetableRequest(validUpdateReqDTO);
        when(objectMapper.writeValueAsString(validTimetableData)).thenReturn(VALID_TIMETABLE_JSON);
        when(teamMemberRepository.save(testTeamMember)).thenReturn(testTeamMember);

        // When
        TimetableRespDTO result = teamTimetableService.updateMyTimetable(TEST_TEAM_ID, validUpdateReqDTO, TEST_USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_USER_ID, result.getUserId());
        assertEquals(TEST_TEAM_ID, result.getTeamId());
        assertEquals(validTimetableData, result.getTimetableData());
        assertNotNull(result.getUpdatedTimetableAt());

        // 팀 멤버의 시간표 데이터가 업데이트되었는지 확인
        assertEquals(VALID_TIMETABLE_JSON, testTeamMember.getTimetableData());
        assertNotNull(testTeamMember.getUpdatedTimetableAt());

        verify(entityValidationUtil).validateTeamExists(TEST_TEAM_ID);
        verify(permissionValidationUtil).validateTeamMemberAccess(TEST_TEAM_ID, TEST_USER_ID, "본인의 시간표만 입력할 수 있습니다.");
        verify(teamTimetableUtil).validateTimetableRequest(validUpdateReqDTO);
        verify(objectMapper).writeValueAsString(validTimetableData);
        verify(teamMemberRepository).save(testTeamMember);
    }

    @Test
    @DisplayName("3-2. 팀원이 아닌 사용자의 수정 시도 시 예외 발생")
    void updateMyTimetable_ThrowsException_NotTeamMember() {
        // Given
        when(entityValidationUtil.validateTeamExists(TEST_TEAM_ID)).thenReturn(testTeam);
        when(permissionValidationUtil.validateTeamMemberAccess(TEST_TEAM_ID, OTHER_USER_ID, "본인의 시간표만 입력할 수 있습니다."))
                .thenThrow(new RuntimeException("본인의 시간표만 입력할 수 있습니다."));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> teamTimetableService.updateMyTimetable(TEST_TEAM_ID, validUpdateReqDTO, OTHER_USER_ID));

        assertEquals("본인의 시간표만 입력할 수 있습니다.", exception.getMessage());

        verify(entityValidationUtil).validateTeamExists(TEST_TEAM_ID);
        verify(permissionValidationUtil).validateTeamMemberAccess(TEST_TEAM_ID, OTHER_USER_ID, "본인의 시간표만 입력할 수 있습니다.");
        verify(teamTimetableUtil, never()).validateTimetableRequest(any());
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("3-3. JSON 변환 오류 발생 시 예외 발생")
    void updateMyTimetable_ThrowsException_JsonProcessingError() throws JacksonException {
        // Given
        when(entityValidationUtil.validateTeamExists(TEST_TEAM_ID)).thenReturn(testTeam);
        when(permissionValidationUtil.validateTeamMemberAccess(TEST_TEAM_ID, TEST_USER_ID, "본인의 시간표만 입력할 수 있습니다."))
                .thenReturn(testTeamMember);
        doNothing().when(teamTimetableUtil).validateTimetableRequest(validUpdateReqDTO);
        when(objectMapper.writeValueAsString(validTimetableData))
                .thenThrow(new JacksonException("JSON 변환 실패") {});

        // When & Then
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> teamTimetableService.updateMyTimetable(TEST_TEAM_ID, validUpdateReqDTO, TEST_USER_ID));

        assertEquals("시간표 데이터 처리 중 오류가 발생했습니다.", exception.getMessage());

        verify(entityValidationUtil).validateTeamExists(TEST_TEAM_ID);
        verify(permissionValidationUtil).validateTeamMemberAccess(TEST_TEAM_ID, TEST_USER_ID, "본인의 시간표만 입력할 수 있습니다.");
        verify(teamTimetableUtil).validateTimetableRequest(validUpdateReqDTO);
        verify(objectMapper).writeValueAsString(validTimetableData);
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    @DisplayName("3-4. 시간표 업데이트 시간이 올바르게 설정되는지 확인")
    void updateMyTimetable_VerifyTimeUpdate() throws JacksonException {
        // Given
        LocalDateTime beforeUpdate = LocalDateTime.now();

        when(entityValidationUtil.validateTeamExists(TEST_TEAM_ID)).thenReturn(testTeam);
        when(permissionValidationUtil.validateTeamMemberAccess(TEST_TEAM_ID, TEST_USER_ID, "본인의 시간표만 입력할 수 있습니다."))
                .thenReturn(testTeamMember);
        doNothing().when(teamTimetableUtil).validateTimetableRequest(validUpdateReqDTO);
        when(objectMapper.writeValueAsString(validTimetableData)).thenReturn(VALID_TIMETABLE_JSON);
        when(teamMemberRepository.save(testTeamMember)).thenReturn(testTeamMember);

        // When
        TimetableRespDTO result = teamTimetableService.updateMyTimetable(TEST_TEAM_ID, validUpdateReqDTO, TEST_USER_ID);

        // Then
        LocalDateTime afterUpdate = LocalDateTime.now();

        assertNotNull(result.getUpdatedTimetableAt());
        assertTrue(result.getUpdatedTimetableAt().isAfter(beforeUpdate.minusSeconds(1)));
        assertTrue(result.getUpdatedTimetableAt().isBefore(afterUpdate.plusSeconds(1)));

        assertNotNull(testTeamMember.getUpdatedTimetableAt());
        assertTrue(testTeamMember.getUpdatedTimetableAt().isAfter(beforeUpdate.minusSeconds(1)));
        assertTrue(testTeamMember.getUpdatedTimetableAt().isBefore(afterUpdate.plusSeconds(1)));
    }
}
