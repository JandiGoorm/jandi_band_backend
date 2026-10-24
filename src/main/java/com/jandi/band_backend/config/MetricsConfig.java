package com.jandi.band_backend.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.micrometer.metrics.autoconfigure.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Prometheus 메트릭 수집을 위한 설정 클래스
 * 애플리케이션의 다양한 메트릭을 정의하고 설정합니다.
 */
@Configuration
public class MetricsConfig {

    @Autowired
    private Environment environment;

    // 활성 사용자 수를 추적하는 게이지
    private final AtomicInteger activeUsers = new AtomicInteger(0);

    // API 호출 횟수를 추적하는 카운터
    private Counter apiCallCounter;

    // 비즈니스 로직 처리 시간을 추적하는 타이머
    private Timer businessLogicTimer;

    /**
     * MeterRegistry 커스터마이저
     * 공통 태그를 설정하여 모든 메트릭에 애플리케이션 정보를 포함
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            registry.config().commonTags(
                Tags.of(
                    "application", "jandi_band_backend",
                    "version", "0.0.1-SNAPSHOT",
                    "environment", getEnvironment()
                )
            );

            // 커스텀 메트릭 등록
            registerCustomMetrics(registry);
        };
    }

    /**
     * 커스텀 메트릭들을 등록하는 메서드
     */
    private void registerCustomMetrics(MeterRegistry registry) {
        // 1. 활성 사용자 수 게이지
        Gauge.builder("jandi.active.users", activeUsers, AtomicInteger::get)
            .description("Number of active users")
            .register(registry);

        // 2. API 호출 횟수 카운터
        apiCallCounter = Counter.builder("jandi.api.calls.total")
            .description("Total number of API calls")
            .register(registry);

        // 3. 비즈니스 로직 처리 시간 타이머
        businessLogicTimer = Timer.builder("jandi.business.logic.duration")
            .description("Time taken for business logic processing")
            .register(registry);

        // 4. 에러 카운터 (HTTP 상태별)
        Counter.builder("jandi.errors.total")
            .description("Total number of errors")
            .tag("status", "4xx")
            .register(registry);

        Counter.builder("jandi.errors.total")
            .description("Total number of errors")
            .tag("status", "5xx")
            .register(registry);

        // 5. 데이터베이스 연결 상태 게이지
        Gauge.builder("jandi.database.connections.active", this, obj -> obj.getDatabaseConnections().doubleValue())
            .description("Number of active database connections")
            .register(registry);
    }

    /**
     * 현재 환경을 반환
     */
    private String getEnvironment() {
        String[] profiles = environment.getActiveProfiles();
        return profiles.length > 0 ? profiles[0] : "development";
    }

    /**
     * 데이터베이스 연결 수를 반환 (예시)
     * 실제로는 HikariCP 메트릭을 사용하는 것이 좋습니다.
     */
    private Number getDatabaseConnections() {
        // 실제 구현에서는 HikariCP DataSource에서 정보를 가져올 수 있습니다.
        return Math.random() * 10; // 예시용 랜덤 값
    }

    // Getter 메서드들 (다른 클래스에서 메트릭을 업데이트할 때 사용)
    public AtomicInteger getActiveUsers() {
        return activeUsers;
    }

    public Counter getApiCallCounter() {
        return apiCallCounter;
    }

    public Timer getBusinessLogicTimer() {
        return businessLogicTimer;
    }
}
