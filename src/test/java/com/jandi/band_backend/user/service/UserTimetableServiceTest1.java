package com.jandi.band_backend.user.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.jandi.band_backend.global.exception.InvalidAccessException;
import com.jandi.band_backend.global.exception.TimetableNotFoundException;
import com.jandi.band_backend.global.util.UserValidationUtil;
import com.jandi.band_backend.user.dto.UserTimetableDetailsRespDTO;
import com.jandi.band_backend.user.dto.UserTimetableRespDTO;
import com.jandi.band_backend.user.entity.UserTimetable;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserTimetableRepository;
import com.jandi.band_backend.user.util.UserTimetableUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserTimetableService 조회 테스트")
class UserTimetableServiceTest1 {

    @InjectMocks
    private UserTimetableService userTimetableService;

    @Mock private UserService userService;
    @Mock private UserTimetableRepository userTimetableRepository;
    @Mock private UserTimetableUtil userTimetableUtil;
    @Mock private UserValidationUtil userValidationUtil;

    private Users testUser;
    private Users adminUser;
    private Users otherUser;
    private UserTimetable testTimetable1;
    private UserTimetable testTimetable2;
    private UserTimetable otherUserTimetable;
    private JsonNode testTimetableData;

    private final Integer TEST_USER_ID = 1;
    private final Integer ADMIN_USER_ID = 2;
    private final Integer OTHER_USER_ID = 3;
    private final Integer TEST_TIMETABLE_ID = 1;
    private final Integer OTHER_TIMETABLE_ID = 2;
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
        testUser.setAdminRole(Users.AdminRole.USER);

        adminUser = new Users();
        adminUser.setId(ADMIN_USER_ID);
        adminUser.setNickname("관리자");
        adminUser.setAdminRole(Users.AdminRole.ADMIN);

        otherUser = new Users();
        otherUser.setId(OTHER_USER_ID);
        otherUser.setNickname("다른사용자");
        otherUser.setAdminRole(Users.AdminRole.USER);

        ObjectMapper objectMapper = tools.jackson.databind.json.JsonMapper.builder().build();
        testTimetableData = objectMapper.readTree(TIMETABLE_JSON);

        testTimetable1 = new UserTimetable();
        testTimetable1.setId(TEST_TIMETABLE_ID);
        testTimetable1.setUser(testUser);
        testTimetable1.setName("내 시간표 1");
        testTimetable1.setTimetableData(TIMETABLE_JSON);
        testTimetable1.setCreatedAt(LocalDateTime.now().minusDays(2));

        testTimetable2 = new UserTimetable();
        testTimetable2.setId(2);
        testTimetable2.setUser(testUser);
        testTimetable2.setName("내 시간표 2");
        testTimetable2.setTimetableData(TIMETABLE_JSON);
        testTimetable2.setCreatedAt(LocalDateTime.now().minusDays(1));

        otherUserTimetable = new UserTimetable();
        otherUserTimetable.setId(OTHER_TIMETABLE_ID);
        otherUserTimetable.setUser(otherUser);
        otherUserTimetable.setName("다른 사용자 시간표");
        otherUserTimetable.setTimetableData(TIMETABLE_JSON);
    }

    // ================ getMyTimetables 테스트 ================

    @Test
    @DisplayName("1-1. 내 시간표 목록 정상 조회")
    void getMyTimetables_Success() {
        // Given
        List<UserTimetable> mockTimetables = Arrays.asList(testTimetable1, testTimetable2);
        when(userService.getMyInfo(TEST_USER_ID)).thenReturn(testUser);
        when(userTimetableRepository.findByUserAndDeletedAtIsNull(testUser)).thenReturn(mockTimetables);

        // When
        List<UserTimetableRespDTO> result = userTimetableService.getMyTimetables(TEST_USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(TEST_TIMETABLE_ID, result.get(0).getId());
        assertEquals("내 시간표 1", result.get(0).getName());
        assertEquals(2, result.get(1).getId());
        assertEquals("내 시간표 2", result.get(1).getName());

        verify(userService).getMyInfo(TEST_USER_ID);
        verify(userTimetableRepository).findByUserAndDeletedAtIsNull(testUser);
    }

    @Test
    @DisplayName("1-2. 시간표가 없는 사용자의 빈 목록 조회")
    void getMyTimetables_EmptyList() {
        // Given
        when(userService.getMyInfo(TEST_USER_ID)).thenReturn(testUser);
        when(userTimetableRepository.findByUserAndDeletedAtIsNull(testUser)).thenReturn(Collections.emptyList());

        // When
        List<UserTimetableRespDTO> result = userTimetableService.getMyTimetables(TEST_USER_ID);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(userService).getMyInfo(TEST_USER_ID);
        verify(userTimetableRepository).findByUserAndDeletedAtIsNull(testUser);
    }

    @Test
    @DisplayName("1-3. 삭제된 시간표 제외하고 조회")
    void getMyTimetables_ExcludeDeleted() {
        // Given
        UserTimetable deletedTimetable = new UserTimetable();
        deletedTimetable.setId(3);
        deletedTimetable.setUser(testUser);
        deletedTimetable.setName("삭제된 시간표");
        deletedTimetable.setDeletedAt(LocalDateTime.now());

        List<UserTimetable> activeTimetables = Arrays.asList(testTimetable1, testTimetable2);
        when(userService.getMyInfo(TEST_USER_ID)).thenReturn(testUser);
        when(userTimetableRepository.findByUserAndDeletedAtIsNull(testUser)).thenReturn(activeTimetables);

        // When
        List<UserTimetableRespDTO> result = userTimetableService.getMyTimetables(TEST_USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertFalse(result.stream().anyMatch(t -> t.getName().equals("삭제된 시간표")));

        verify(userService).getMyInfo(TEST_USER_ID);
        verify(userTimetableRepository).findByUserAndDeletedAtIsNull(testUser);
    }

    // ================ getMyTimetableById 테스트 ================

    @Test
    @DisplayName("2-1. 본인의 시간표 정상 조회")
    void getMyTimetableById_Success_OwnTimetable() {
        // Given
        when(userService.getMyInfo(TEST_USER_ID)).thenReturn(testUser);
        when(userTimetableRepository.findByIdAndDeletedAtIsNull(TEST_TIMETABLE_ID))
                .thenReturn(Optional.of(testTimetable1));
        when(userValidationUtil.getUserById(TEST_USER_ID)).thenReturn(testUser);
        when(userTimetableUtil.stringToJson(TIMETABLE_JSON)).thenReturn(testTimetableData);

        // When
        UserTimetableDetailsRespDTO result = userTimetableService.getMyTimetableById(TEST_USER_ID, TEST_TIMETABLE_ID);

        // Then
        assertNotNull(result);
        assertEquals(TEST_TIMETABLE_ID, result.getId());
        assertEquals("내 시간표 1", result.getName());
        assertEquals(testTimetableData, result.getTimetableData());

        verify(userService).getMyInfo(TEST_USER_ID);
        verify(userTimetableRepository).findByIdAndDeletedAtIsNull(TEST_TIMETABLE_ID);
        verify(userTimetableUtil).stringToJson(TIMETABLE_JSON);
    }

    @Test
    @DisplayName("2-2. 존재하지 않는 시간표 조회 시 예외 발생")
    void getMyTimetableById_ThrowsException_TimetableNotFound() {
        // Given
        when(userService.getMyInfo(TEST_USER_ID)).thenReturn(testUser);
        when(userTimetableRepository.findByIdAndDeletedAtIsNull(999))
                .thenReturn(Optional.empty());
        // userValidationUtil.getUserById() stubbing 제거 - 예외 발생으로 호출되지 않음

        // When & Then
        assertThrows(TimetableNotFoundException.class,
                () -> userTimetableService.getMyTimetableById(TEST_USER_ID, 999));

        verify(userService).getMyInfo(TEST_USER_ID);
        verify(userTimetableRepository).findByIdAndDeletedAtIsNull(999);
        verify(userTimetableUtil, never()).stringToJson(anyString());
    }

    @Test
    @DisplayName("2-3. 권한 없는 시간표 접근 시 예외 발생")
    void getMyTimetableById_ThrowsException_InvalidAccess() {
        // Given
        when(userService.getMyInfo(TEST_USER_ID)).thenReturn(testUser);
        when(userTimetableRepository.findByIdAndDeletedAtIsNull(OTHER_TIMETABLE_ID))
                .thenReturn(Optional.of(otherUserTimetable));
        when(userValidationUtil.getUserById(TEST_USER_ID)).thenReturn(testUser);

        // When & Then
        InvalidAccessException exception = assertThrows(InvalidAccessException.class,
                () -> userTimetableService.getMyTimetableById(TEST_USER_ID, OTHER_TIMETABLE_ID));

        assertEquals("권한이 없습니다: 본인의 시간표가 아닙니다", exception.getMessage());

        verify(userService).getMyInfo(TEST_USER_ID);
        verify(userTimetableRepository).findByIdAndDeletedAtIsNull(OTHER_TIMETABLE_ID);
        verify(userTimetableUtil, never()).stringToJson(anyString());
    }

    @Test
    @DisplayName("2-4. ADMIN 권한으로 모든 시간표 조회 가능")
    void getMyTimetableById_Success_AdminAccess() {
        // Given
        when(userService.getMyInfo(ADMIN_USER_ID)).thenReturn(adminUser);
        when(userTimetableRepository.findByIdAndDeletedAtIsNull(OTHER_TIMETABLE_ID))
                .thenReturn(Optional.of(otherUserTimetable));
        when(userValidationUtil.getUserById(ADMIN_USER_ID)).thenReturn(adminUser);
        when(userTimetableUtil.stringToJson(TIMETABLE_JSON)).thenReturn(testTimetableData);

        // When
        UserTimetableDetailsRespDTO result = userTimetableService.getMyTimetableById(ADMIN_USER_ID, OTHER_TIMETABLE_ID);

        // Then
        assertNotNull(result);
        assertEquals(OTHER_TIMETABLE_ID, result.getId());
        assertEquals("다른 사용자 시간표", result.getName());
        assertEquals(testTimetableData, result.getTimetableData());

        verify(userService).getMyInfo(ADMIN_USER_ID);
        verify(userTimetableRepository).findByIdAndDeletedAtIsNull(OTHER_TIMETABLE_ID);
        verify(userTimetableUtil).stringToJson(TIMETABLE_JSON);
    }

    @Test
    @DisplayName("2-5. 삭제된 시간표 접근 시 예외 발생")
    void getMyTimetableById_ThrowsException_DeletedTimetable() {
        // Given
        UserTimetable deletedTimetable = new UserTimetable();
        deletedTimetable.setId(TEST_TIMETABLE_ID);
        deletedTimetable.setUser(testUser);
        deletedTimetable.setDeletedAt(LocalDateTime.now());

        when(userService.getMyInfo(TEST_USER_ID)).thenReturn(testUser);
        when(userTimetableRepository.findByIdAndDeletedAtIsNull(TEST_TIMETABLE_ID))
                .thenReturn(Optional.empty()); // 삭제된 시간표는 조회되지 않음
        // userValidationUtil.getUserById() stubbing 제거 - 예외 발생으로 호출되지 않음

        // When & Then
        assertThrows(TimetableNotFoundException.class,
                () -> userTimetableService.getMyTimetableById(TEST_USER_ID, TEST_TIMETABLE_ID));

        verify(userService).getMyInfo(TEST_USER_ID);
        verify(userTimetableRepository).findByIdAndDeletedAtIsNull(TEST_TIMETABLE_ID);
        verify(userTimetableUtil, never()).stringToJson(anyString());
    }
}
