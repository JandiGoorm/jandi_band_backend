package com.jandi.band_backend.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 테스트용 설정 클래스
 * Controller 테스트에서 발생하는 Bean 의존성 문제를 해결합니다.
 */
@TestConfiguration
public class TestConfig {

    /**
     * 테스트용 MeterRegistry Bean
     * MetricsInterceptor의 의존성을 해결합니다.
     */
    @Bean
    @Primary
    public MeterRegistry testMeterRegistry() {
        return new SimpleMeterRegistry();
    }

    /**
     * 테스트용 MetricsConfig Bean
     */
    @Bean
    @Primary
    public MetricsConfig testMetricsConfig() {
        return new MetricsConfig();
    }
}