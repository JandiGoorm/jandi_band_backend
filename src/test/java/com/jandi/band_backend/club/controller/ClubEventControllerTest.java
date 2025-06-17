package com.jandi.band_backend.club.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jandi.band_backend.club.dto.CalendarEventRespDTO;
import com.jandi.band_backend.club.dto.ClubEventReqDTO;
import com.jandi.band_backend.club.dto.ClubEventRespDTO;
import com.jandi.band_backend.club.service.ClubEventService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClubEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    //  @MockBean  // mockbean 테스트 용
    @Autowired  // 실제 데이터를 넣는 테스트
    private ClubEventService clubEventService;

    @Autowired
    private ObjectMapper objectMapper;

    // 동아리 일정 추가 api 테스트 코드 추가
    @Test
    @WithMockUser(username = "testuser", roles = "USER") // 인증된 사용자 시뮬레이션
    void createClubEvent_success() throws Exception {
        // given
        Integer clubId = 1;
        ClubEventReqDTO request = ClubEventReqDTO.builder()
                .name("정기 일정 예시250610")
                .startDatetime(LocalDateTime.of(2025, 6, 20, 18, 0))
                .endDatetime(LocalDateTime.of(2025, 6, 20, 20, 0))
                .build();

        ClubEventRespDTO response = ClubEventRespDTO.builder()
                .id(1L)
                .name(request.getName())
                .startDatetime(request.getStartDatetime())
                .endDatetime(request.getEndDatetime())
                .build();

//        mockbean 테스트 용
//        when(clubEventService.createClubEvent(eq(clubId), anyInt(), any(ClubEventReqDTO.class)))
//                .thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/clubs/{clubId}/events", clubId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()) // Spring Security에서 POST 요청은 CSRF 토큰 필요
                        .header("Authorization",
                                "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI0MjcxMDU1NjQ1Iiwicm9sZSI6IlJPTEVfQURNSU4iLCJpYXQiOjE3NDk4MTI5NzgsImV4cCI6MTc0OTgxMzg3OH0.jh66rHadxQvZn0eobxA5j674a6CAopz-gmDQAS-9XWg")) // 인증 헤더 추가
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("정기 일정 예시250610"));
    }


    // 동아리 일정 상세 조회 api 테스트 코드 추가
    // 테스트가 필요한 데이터에 대하여 원하는 값들로 값을 수정하여 확인하기
    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void getClubEventDetail_success() throws Exception {
        // given
        Integer clubId = 1;
        Integer eventId = 7;

        ClubEventRespDTO response = ClubEventRespDTO.builder()
                .id((long) eventId)
                .name("정기 일정 예시250")
                .startDatetime(LocalDateTime.of(2025, 5, 20, 18, 0))
                .endDatetime(LocalDateTime.of(2025, 5, 20, 20, 0))
                .build();

        //        mockbean 테스트 용
        // Mocking the service response
//        when(clubEventService.getClubEventDetail(eq(clubId), eq(eventId), anyInt()))
//                .thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/clubs/{clubId}/events/{eventId}", clubId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()) // CSRF 토큰 추가
                        .header("Authorization",
                                "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI0MjcxMDU1NjQ1Iiwicm9sZSI6IlJPTEVfQURNSU4iLCJpYXQiOjE3NTAxNDA1OTMsImV4cCI6MTc1MDE0MTQ5M30.QDRCzA9pv9HoyNVUyjW4ZNP8bKHMtyw6h1zBXjelp8o")) // 인증 헤더 추가
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("정기 일정 예시250"));
    }


    // 캘린더용 통합 일정 조회 api 테스트 코드 추가
    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void getCalendarEvents_success() throws Exception {
        // given
        Integer clubId = 1;
        int year = 2025;
        int month = 6;
        String jwt_bearer = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI0MjcxMDU1NjQ1Iiwicm9sZSI6IlJPTEVfQURNSU4iLCJpYXQiOjE3NTAxNDE1NDYsImV4cCI6MTc1MDE0MjQ0Nn0.h7xDcJuP2G-Le8K8FDdtpLjIHAzelMcF2K9s2INu2aA";

        List<CalendarEventRespDTO> responseList = List.of(
                CalendarEventRespDTO.builder()
                        .id(1)
                        .name("string")
                        .startDatetime(LocalDateTime.of(2025, 6, 10, 10, 0))
                        .endDatetime(LocalDateTime.of(2025, 6, 10, 12, 0))
                        .eventType(CalendarEventRespDTO.EventType.TEAM_EVENT)
                        .teamId(2)
                        .teamName("string")
                        .noPosition("NONE")
                        .build(),
                CalendarEventRespDTO.builder()
                        .id(2)
                        .name("string")
                        .startDatetime(LocalDateTime.of(2025, 6, 15, 14, 0))
                        .endDatetime(LocalDateTime.of(2025, 6, 15, 16, 0))
                        .eventType(CalendarEventRespDTO.EventType.CLUB_EVENT)
                        .teamId(2)
                        .teamName("string")
                        .noPosition("NONE")
                        .build()
        );

        //        mockbean 테스트 용
//        // Mocking the service response
//        when(clubEventService.getCalendarEventsForClub(eq(clubId), anyInt(), eq(year), eq(month)))
//                .thenReturn(responseList);

        // when & then
        mockMvc.perform(get("/api/clubs/{clubId}/calendar", clubId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("year", String.valueOf(year))
                        .param("month", String.valueOf(month))
                        .with(csrf()) // CSRF 토큰 추가
                        .header("Authorization",
                                "Bearer "+jwt_bearer)) // 인증 헤더 추가
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("string"))
                .andExpect(jsonPath("$.data[0].eventType").value("TEAM_EVENT"))
                .andExpect(jsonPath("$.data[0].teamId").value(2))
                .andExpect(jsonPath("$.data[0].teamName").value("string"))
                .andExpect(jsonPath("$.data[0].noPosition").value("NONE"))
                .andExpect(jsonPath("$.data[1].name").value("string"))
                .andExpect(jsonPath("$.data[1].eventType").value("TEAM_EVENT"))
                .andExpect(jsonPath("$.data[1].teamId").value(2))
                .andExpect(jsonPath("$.data[1].teamName").value("string"))
                .andExpect(jsonPath("$.data[1].noPosition").value("NONE"));
    }


    // 동아리 일정 삭제 api 테스트 코드 추가
    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void deleteClubEvent_success() throws Exception {
        // given
        Integer clubId = 1;
        Integer eventId = 7;
        String jwtBearer = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI0MjcxMDU1NjQ1Iiwicm9sZSI6IlJPTEVfQURNSU4iLCJpYXQiOjE3NTAxNDI1MTIsImV4cCI6MTc1MDE0MzQxMn0.v7aSk8oHtWlHtL6tHDtlzO5GeritEvOiJiaOsxigBAo";

//        //        mockbean 테스트 용
//        // Mocking the service response
//        doNothing().when(clubEventService).deleteClubEvent(eq(clubId), eq(eventId), anyInt());

        // when & then
        mockMvc.perform(delete("/api/clubs/{clubId}/events/{eventId}", clubId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .with(csrf()) // CSRF 토큰 추가
                        .header("Authorization", "Bearer " + jwtBearer)) // 인증 헤더 추가
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("동아리 일정이 삭제되었습니다."));
    }


}