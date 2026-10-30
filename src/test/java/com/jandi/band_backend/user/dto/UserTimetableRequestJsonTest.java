package com.jandi.band_backend.user.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.jackson.use-jackson2-defaults=true")
@ActiveProfiles("test")
class UserTimetableRequestJsonTest {
    @Autowired
    private ObjectMapper mapper;

    @Test
    void readsTimetableObjectWithDeploymentJsonDefaults() throws Exception {
        UserTimetableReqDTO request = mapper.readValue(
                "{\"name\":\"local timetable\",\"timetableData\":{\"Mon\":[\"09:00\"]}}",
                UserTimetableReqDTO.class);

        assertThat(request.getName()).isEqualTo("local timetable");
        assertThat(request.getTimetableData().path("Mon").get(0).asString()).isEqualTo("09:00");
    }
}
