package com.jandi.band_backend.config;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestExecutionListeners.MergeMode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.junit.jupiter.api.TestInstance;

import java.lang.annotation.*;

/**
 * 통합 테스트를 위한 기본 애노테이션
 * - MockMvc 자동 설정 추가
 * - TestContainers 자동 설정
 * - 테스트 프로파일 활성화
 * - 트랜잭션 롤백
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "logging.level.org.springframework.security=DEBUG",
    "spring.sql.init.mode=never"
})
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = MergeMode.MERGE_WITH_DEFAULTS)
public @interface IntegrationTest {
}
