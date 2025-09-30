package com.jandi.band_backend.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jandi.band_backend.config.TestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;

/**
 * Controller 테스트를 위한 기본 클래스
 * 공통 설정과 유틸리티 메서드를 제공합니다.
 */
@WebMvcTest
@Import(TestConfig.class)
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public abstract class BaseControllerTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    void baseSetUp() {
        // 공통 설정이 필요한 경우 여기에 추가
    }

    /**
     * JSON 문자열을 예쁘게 포맷팅하는 유틸리티 메서드
     */
    protected String toJsonString(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    /**
     * 공통 테스트 데이터 생성 메서드들을 여기에 추가할 수 있습니다.
     */
}