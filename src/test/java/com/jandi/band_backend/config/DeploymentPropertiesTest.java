package com.jandi.band_backend.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DeploymentPropertiesTest {
    @ParameterizedTest
    @CsvSource({"prod,jandi_band_backend,0", "dev,jandi_band_backend_dev,1"})
    void loadsProfileAndResolvesExternalCredentials(String profile, String name, String redisDb) {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("deployment", Map.of(
                "spring.config.location", Path.of("deployment-config/application.properties").toUri().toString(),
                "spring.profiles.active", profile,
                "DB_URL", "jdbc:mysql://test/" + profile,
                "DB_PASSWORD", "literal$dollar#hash"
        )));

        ConfigDataEnvironmentPostProcessor.applyTo(environment);

        assertThat(environment.getProperty("spring.application.name")).isEqualTo(name);
        assertThat(environment.getProperty("spring.data.redis.database")).isEqualTo(redisDb);
        assertThat(environment.getProperty("spring.datasource.url")).isEqualTo("jdbc:mysql://test/" + profile);
        assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("literal$dollar#hash");
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
    }
}
