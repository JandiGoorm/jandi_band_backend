package com.jandi.band_backend.auth.dto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.jackson.use-jackson2-defaults=true")
@ActiveProfiles("test")
class RefreshRequestJsonTest {
    @Autowired
    private ObjectMapper mapper;

    @Test
    void readsRefreshTokenObjectWithDeploymentJsonDefaults() throws Exception {
        RefreshReqDTO request = mapper.readValue(
                "{\"refreshToken\":\"test-refresh-token\"}", RefreshReqDTO.class);

        assertThat(request.getRefreshToken()).isEqualTo("test-refresh-token");
    }
}
