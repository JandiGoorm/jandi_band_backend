package com.jandi.band_backend.user.service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.jandi.band_backend.global.exception.InvalidAccessException;
import com.jandi.band_backend.global.exception.TimetableNotFoundException;
import com.jandi.band_backend.global.util.UserValidationUtil;
import com.jandi.band_backend.user.dto.UserTimetableDetailsRespDTO;
import com.jandi.band_backend.user.dto.UserTimetableReqDTO;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserTimetableService 생성/수정/삭제 테스트")
class UserTimetableServiceTest2 {

    @InjectMocks
    private UserTimetableService userTimetableService;

    @Mock private UserService userService;
    @Mock private UserTimetableRepository userTimetableRepository;
    @Mock private UserTimetableUtil userTimetableUtil;
    @Mock private UserValidationUtil userValidationUtil;

    private Users testUser;
    private Users adminUser;
    private Users otherUser;
    private UserTimetable testTimetable;
    private UserTimetableReqDTO validReqDTO;
    private JsonNode validTimetableData;
    private JsonNode invalidTimetableData;

    private final Integer TEST_USER_ID = 1;
    private final Integer ADMIN_USER_ID = 2;
    private final Integer OTHER_USER_ID = 3;
    private final Integer TEST_TIMETABLE_ID = 1;
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
        validTimetableData = objectMapper.readTree(VALID_TIMETABLE_JSON);

        validReqDTO = new UserTimetableReqDTO("새 시간표", validTimetableData);

        testTimetable = new UserTimetable();
        testTimetable.setId(TEST_TIMETABLE_ID);
        testTimetable.setUser(testUser);
        testTimetable.setName("기존 시간표");
        testTimetable.setTimetableData(VALID_TIMETABLE_JSON);
        testTimetable.setCreatedAt(LocalDateTime.now().minusDays(1));
    }

    // ================ createTimetable 테스트 ================

    @Test
    @DisplayName("3-1. 유효한 시간표 데이터로 정상 생성")
    void createTimetable_Success() {
        // Given
        when(userService.getMyInfo(TEST_USER_ID)).thenReturn(testUser);
        doNothing().when(userTimetableUtil).validateTimetableRequest(validReqDTO);
        
        // save() 호출 시 전달된 객체에 ID를 설정하도록 Mock 설정
        when(userTimetableRepository.save(any(UserTimetable.class))).thenAnswer(invocation -> {
            UserTimetable timetable = invocation.getArgument(0);
            timetable.setId(TEST_TIMETABLE_ID); // ID 설정
            return timetable; // 동일한 객체 반환
        });
        
        when(userTimetableUtil.stringToJson(anyString())).thenReturn(validTimetableData);

        // When
        UserTimetableDetailsRespDTO result = userTimetableService.createTimetable(TEST_USER_ID, validReqDTO);

        // Then
        assertNotNull(result);
        assertEquals(TEST_TIMETABLE_ID, result.getId());
        assertEquals("새 시간표", result.getName());
        assertEquals(validTimetableData, result.getTimetableData());

        verify(userService).getMyInfo(TEST_USER_ID);
        verify(userTimetableUtil).validateTimetableRequest(validReqDTO);
        verify(userTimetableRepository).save(any(UserTimetable.class));
        verify(userTimetableUtil).stringToJson(anyString());
    }

    @Test
    @DisplayName("3-2. 시간표 이름 누락 시 예외 발생")
    void createTimetable_ThrowsException_EmptyName() {
        // Given
        UserTimetableReqDTO reqWithEmptyName = new UserTimetableReqDTO("", validTimetableData);

        when(userService.getMyInfo(TEST_USER_ID)).thenReturn(testUser);
        doThrow(new IllegalArgumentException("제목은 공란이 될 수 없습니다."))
                .when(userTimetableUtil).validateTimetableRequest(reqWithEmptyName);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userTimetableService.createTimetable(TEST_USER_ID, reqWithEmptyName));

        assertEquals("제목은 공란이 될 수 없습니다.", exception.getMessage());

        verify(userService).getMyInfo(TEST_USER_ID);
        verify(userTimetableUtil).validateTimetableRequest(reqWithEmptyName);
        verify(userTimetableRepository, never()).save(any(UserTimetable.class));
    }

    @Test
    @DisplayName("3-3. 시간표 이름이 null인 경우 예외 발생")
    void createTimetable_ThrowsException_NullName() {
        // Given
        UserTimetableReqDTO reqWithNullName = new UserTimetableReqDTO(null, validTimetableData);

        when(userService.getMyInfo(TEST_USER_ID)).thenReturn(testUser);
        doThrow(new IllegalArgumentException("제목은 공란이 될 수 없습니다."))
                .when(userTimetableUtil).validateTimetableRequest(reqWithNullName);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userTimetableService.createTimetable(TEST_USER_ID, reqWithNullName));

        assertEquals("제목은 공란이 될 수 없습니다.", exception.getMessage());

        verify(userService).getMyInfo(TEST_USER_ID);
        verify(userTimetableUtil).validateTimetableRequest(reqWithNullName);
        verify(userTimetableRepository, never()).save(any(UserTimetable.class));
    }

    @Test
    @DisplayName("3-4. 시간표 형식 오류 - 요일 누락")
    void createTimetable_ThrowsException_MissingWeekday() throws Exception {
        // Given
        String invalidJsonMissingWeekday = """
            {
              "Mon": ["09:00", "10:00"],
              "Tue": ["14:00", "15:00"],
              "Wed": ["09:00", "10:00"],
              "Thu": []
            }
            """;
        ObjectMapper objectMapper = tools.jackson.databind.json.JsonMapper.builder().build();
        JsonNode invalidData = objectMapper.readTree(invalidJsonMissingWeekday);
        UserTimetableReqDTO invalidReqDTO = new UserTimetableReqDTO("시간표", invalidData);

        when(userService.getMyInfo(TEST_USER_ID)).thenReturn(testUser);
        doThrow(new IllegalArgumentException("시간표는 모든 요일을 포함해야 합니다: Fri 누락되었습니다."))
                .when(userTimetableUtil).validateTimetableRequest(invalidReqDTO);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userTimetableService.createTimetable(TEST_USER_ID, invalidReqDTO));

        assertTrue(exception.getMessage().contains("시간표는 모든 요일을 포함해야 합니다"));

        verify(userService).getMyInfo(TEST_USER_ID);
        verify(userTimetableUtil).validateTimetableRequest(invalidReqDTO);
        verify(userTimetableRepository, never()).save(any(UserTimetable.class));
    }

    @Test
    @DisplayName("3-5. 시간표 형식 오류 - 잘못된 시간 형식")
    void createTimetable_ThrowsException_InvalidTimeFormat() throws Exception {
        // Given
        String invalidJsonTimeFormat = """
            {
              "Mon": ["9:00", "10:00"],
              "Tue": ["14:00", "15:00"],
              "Wed": ["09:00", "10:00"],
              "Thu": [],
              "Fri": ["13:00", "14:00"],
              "Sat": [],
              "Sun": []
            }
            """;
        ObjectMapper objectMapper = tools.jackson.databind.json.JsonMapper.builder().build();
        JsonNode invalidData = objectMapper.readTree(invalidJsonTimeFormat);
        UserTimetableReqDTO invalidReqDTO = new UserTimetableReqDTO("시간표", invalidData);

        when(userService.getMyInfo(TEST_USER_ID)).thenReturn(testUser);
        doThrow(new IllegalArgumentException("9:00은 HH:mm (HH: 00~24, mm: 00, 30)형식의 문자열이어야 합니다."))
                .when(userTimetableUtil).validateTimetableRequest(invalidReqDTO);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userTimetableService.createTimetable(TEST_USER_ID, invalidReqDTO));

        assertTrue(exception.getMessage().contains("HH:mm"));

        verify(userTimetableRepository, never()).save(any(UserTimetable.class));
    }

    @Test
    @DisplayName("3-6. 시간표 형식 오류 - 30분 단위가 아닌 시간")
    void createTimetable_ThrowsException_InvalidMinuteUnit() throws Exception {
        // Given
        String invalidJsonMinute = """
            {
              "Mon": ["09:15", "10:00"],
              "Tue": ["14:00", "15:00"],
              "Wed": ["09:00", "10:00"],
              "Thu": [],
              "Fri": ["13:00", "14:00"],
              "Sat": [],
              "Sun": []
            }
            """;
        ObjectMapper objectMapper = tools.jackson.databind.json.JsonMapper.builder().build();
        JsonNode invalidData = objectMapper.readTree(invalidJsonMinute);
        UserTimetableReqDTO invalidReqDTO = new UserTimetableReqDTO("시간표", invalidData);

        when(userService.getMyInfo(TEST_USER_ID)).thenReturn(testUser);
        doThrow(new IllegalArgumentException("09:15은 30분 단위여야 합니다."))
                .when(userTimetableUtil).validateTimetableRequest(invalidReqDTO);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userTimetableService.createTimetable(TEST_USER_ID, invalidReqDTO));

        assertTrue(exception.getMessage().contains("30분 단위여야 합니다"));

        verify(userTimetableRepository, never()).save(any(UserTimetable.class));
    }

    // ================ updateTimetable 테스트 ================

    @Test
    @DisplayName("4-1. 본인의 시간표 정상 수정")
    void updateTimetable_Success_OwnTimetable() {
        // Given
        when(userService.getMyInfo(TEST_USER_ID)).thenReturn(testUser);
        when(userTimetableRepository.findByIdAndDeletedAtIsNull(TEST_TIMETABLE_ID))
                .thenReturn(Optional.of(testTimetable));
        when(userValidationUtil.getUserById(TEST_USER_ID)).thenReturn(testUser);
        doNothing().when(userTimetableUtil).validateTimetableRequest(validReqDTO);
        when(userTimetableRepository.save(testTimetable)).thenReturn(testTimetable);
        when(userTimetableUtil.stringToJson(anyString())).thenReturn(validTimetableData); // anyString() 사용

        // When
        UserTimetableDetailsRespDTO result = userTimetableService.updateTimetable(TEST_USER_ID, TEST_TIMETABLE_ID, validReqDTO);

        // Then
        assertNotNull(result);
        assertEquals(TEST_TIMETABLE_ID, result.getId());
        assertEquals("새 시간표", result.getName());
        assertEquals(validTimetableData, result.getTimetableData());

        verify(userService).getMyInfo(TEST_USER_ID);
        verify(userTimetableRepository).findByIdAndDeletedAtIsNull(TEST_TIMETABLE_ID);
        verify(userTimetableUtil).validateTimetableRequest(validReqDTO);
        verify(userTimetableRepository).save(testTimetable);
    }

    @Test
    @DisplayName("4-2. 권한 없는 시간표 수정 시도 시 예외 발생")
    void updateTimetable_ThrowsException_InvalidAccess() {
        // Given
        UserTimetable otherUserTimetable = new UserTimetable();
        otherUserTimetable.setId(TEST_TIMETABLE_ID);
        otherUserTimetable.setUser(otherUser);

        when(userService.getMyInfo(TEST_USER_ID)).thenReturn(testUser);
        when(userTimetableRepository.findByIdAndDeletedAtIsNull(TEST_TIMETABLE_ID))
                .thenReturn(Optional.of(otherUserTimetable));
        when(userValidationUtil.getUserById(TEST_USER_ID)).thenReturn(testUser);

        // When & Then
        InvalidAccessException exception = assertThrows(InvalidAccessException.class,
                () -> userTimetableService.updateTimetable(TEST_USER_ID, TEST_TIMETABLE_ID, validReqDTO));

        assertEquals("권한이 없습니다: 본인의 시간표가 아닙니다", exception.getMessage());

        verify(userService).getMyInfo(TEST_USER_ID);
        verify(userTimetableRepository).findByIdAndDeletedAtIsNull(TEST_TIMETABLE_ID);
        verify(userTimetableUtil, never()).validateTimetableRequest(any());
        verify(userTimetableRepository, never()).save(any());
    }

    @Test
    @DisplayName("4-3. ADMIN 권한으로 모든 시간표 수정 가능")
    void updateTimetable_Success_AdminAccess() {
        // Given
        UserTimetable otherUserTimetable = new UserTimetable();
        otherUserTimetable.setId(TEST_TIMETABLE_ID);
        otherUserTimetable.setUser(otherUser);
        otherUserTimetable.setName("다른 사용자 시간표");
        otherUserTimetable.setTimetableData(validTimetableData.toString()); // JsonNode.toString() 사용

        when(userService.getMyInfo(ADMIN_USER_ID)).thenReturn(adminUser);
        when(userTimetableRepository.findByIdAndDeletedAtIsNull(TEST_TIMETABLE_ID))
                .thenReturn(Optional.of(otherUserTimetable));
        when(userValidationUtil.getUserById(ADMIN_USER_ID)).thenReturn(adminUser);
        doNothing().when(userTimetableUtil).validateTimetableRequest(validReqDTO);
        when(userTimetableRepository.save(otherUserTimetable)).thenReturn(otherUserTimetable);
        when(userTimetableUtil.stringToJson(anyString())).thenReturn(validTimetableData); // anyString() 사용

        // When
        UserTimetableDetailsRespDTO result = userTimetableService.updateTimetable(ADMIN_USER_ID, TEST_TIMETABLE_ID, validReqDTO);

        // Then
        assertNotNull(result);
        assertEquals(TEST_TIMETABLE_ID, result.getId());
        assertEquals("새 시간표", result.getName());

        verify(userService).getMyInfo(ADMIN_USER_ID);
        verify(userTimetableRepository).findByIdAndDeletedAtIsNull(TEST_TIMETABLE_ID);
        verify(userTimetableUtil).validateTimetableRequest(validReqDTO);
        verify(userTimetableRepository).save(otherUserTimetable);
    }

    @Test
    @DisplayName("4-4. 존재하지 않는 시간표 수정 시 예외 발생")
    void updateTimetable_ThrowsException_TimetableNotFound() {
        // Given
        when(userService.getMyInfo(TEST_USER_ID)).thenReturn(testUser);
        when(userTimetableRepository.findByIdAndDeletedAtIsNull(999))
                .thenReturn(Optional.empty());
        // userValidationUtil.getUserById() stubbing 제거 - 예외 발생으로 호출되지 않음

        // When & Then
        assertThrows(TimetableNotFoundException.class,
                () -> userTimetableService.updateTimetable(TEST_USER_ID, 999, validReqDTO));

        verify(userService).getMyInfo(TEST_USER_ID);
        verify(userTimetableRepository).findByIdAndDeletedAtIsNull(999);
        verify(userTimetableUtil, never()).validateTimetableRequest(any());
        verify(userTimetableRepository, never()).save(any());
    }

    // ================ deleteMyTimetable 테스트 ================

    @Test
    @DisplayName("5-1. 본인의 시간표 정상 삭제")
    void deleteMyTimetable_Success_OwnTimetable() {
        // Given
        when(userService.getMyInfo(TEST_USER_ID)).thenReturn(testUser);
        when(userTimetableRepository.findByIdAndDeletedAtIsNull(TEST_TIMETABLE_ID))
                .thenReturn(Optional.of(testTimetable));
        when(userValidationUtil.getUserById(TEST_USER_ID)).thenReturn(testUser);
        when(userTimetableRepository.save(testTimetable)).thenReturn(testTimetable);

        // When
        assertDoesNotThrow(() -> userTimetableService.deleteMyTimetable(TEST_USER_ID, TEST_TIMETABLE_ID));

        // Then
        assertNotNull(testTimetable.getDeletedAt());

        verify(userService).getMyInfo(TEST_USER_ID);
        verify(userTimetableRepository).findByIdAndDeletedAtIsNull(TEST_TIMETABLE_ID);
        verify(userTimetableRepository).save(testTimetable);
    }

    @Test
    @DisplayName("5-2. 권한 없는 시간표 삭제 시도 시 예외 발생")
    void deleteMyTimetable_ThrowsException_InvalidAccess() {
        // Given
        UserTimetable otherUserTimetable = new UserTimetable();
        otherUserTimetable.setId(TEST_TIMETABLE_ID);
        otherUserTimetable.setUser(otherUser);

        when(userService.getMyInfo(TEST_USER_ID)).thenReturn(testUser);
        when(userTimetableRepository.findByIdAndDeletedAtIsNull(TEST_TIMETABLE_ID))
                .thenReturn(Optional.of(otherUserTimetable));
        when(userValidationUtil.getUserById(TEST_USER_ID)).thenReturn(testUser);

        // When & Then
        InvalidAccessException exception = assertThrows(InvalidAccessException.class,
                () -> userTimetableService.deleteMyTimetable(TEST_USER_ID, TEST_TIMETABLE_ID));

        assertEquals("권한이 없습니다: 본인의 시간표가 아닙니다", exception.getMessage());

        verify(userService).getMyInfo(TEST_USER_ID);
        verify(userTimetableRepository).findByIdAndDeletedAtIsNull(TEST_TIMETABLE_ID);
        verify(userTimetableRepository, never()).save(any());
    }

    @Test
    @DisplayName("5-3. ADMIN 권한으로 모든 시간표 삭제 가능")
    void deleteMyTimetable_Success_AdminAccess() {
        // Given
        UserTimetable otherUserTimetable = new UserTimetable();
        otherUserTimetable.setId(TEST_TIMETABLE_ID);
        otherUserTimetable.setUser(otherUser);

        when(userService.getMyInfo(ADMIN_USER_ID)).thenReturn(adminUser);
        when(userTimetableRepository.findByIdAndDeletedAtIsNull(TEST_TIMETABLE_ID))
                .thenReturn(Optional.of(otherUserTimetable));
        when(userValidationUtil.getUserById(ADMIN_USER_ID)).thenReturn(adminUser);
        when(userTimetableRepository.save(otherUserTimetable)).thenReturn(otherUserTimetable);

        // When
        assertDoesNotThrow(() -> userTimetableService.deleteMyTimetable(ADMIN_USER_ID, TEST_TIMETABLE_ID));

        // Then
        assertNotNull(otherUserTimetable.getDeletedAt());

        verify(userService).getMyInfo(ADMIN_USER_ID);
        verify(userTimetableRepository).findByIdAndDeletedAtIsNull(TEST_TIMETABLE_ID);
        verify(userTimetableRepository).save(otherUserTimetable);
    }

    @Test
    @DisplayName("5-4. 이미 삭제된 시간표 삭제 시도 시 예외 발생")
    void deleteMyTimetable_ThrowsException_AlreadyDeleted() {
        // Given
        when(userService.getMyInfo(TEST_USER_ID)).thenReturn(testUser);
        when(userTimetableRepository.findByIdAndDeletedAtIsNull(TEST_TIMETABLE_ID))
                .thenReturn(Optional.empty()); // 이미 삭제된 시간표는 조회되지 않음
        // userValidationUtil.getUserById() stubbing 제거 - 예외 발생으로 호출되지 않음

        // When & Then
        assertThrows(TimetableNotFoundException.class,
                () -> userTimetableService.deleteMyTimetable(TEST_USER_ID, TEST_TIMETABLE_ID));

        verify(userService).getMyInfo(TEST_USER_ID);
        verify(userTimetableRepository).findByIdAndDeletedAtIsNull(TEST_TIMETABLE_ID);
        verify(userTimetableRepository, never()).save(any());
    }
}
