package com.jandi.band_backend.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.nio.file.Path;
import java.util.Map;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

class DeploymentPropertiesTest {
    @org.junit.jupiter.api.Test
    void requiresR2ConfigurationWithoutAwsFallback() {
        var environment = new StandardEnvironment();
        var values = new HashMap<String, Object>();
        values.put("spring.config.location", Path.of("deployment-config/application.properties").toUri().toString());
        values.put("IMAGE_STORAGE_ACCESS_KEY", "r2-test-access");
        values.put("IMAGE_STORAGE_SECRET_KEY", "r2-test-secret");
        values.put("IMAGE_STORAGE_REGION", "auto");
        values.put("IMAGE_STORAGE_ENDPOINT", "https://account.r2.cloudflarestorage.com");
        values.put("IMAGE_STORAGE_BUCKET", "r2-test-bucket");
        values.put("IMAGE_STORAGE_PUBLIC_URL", "https://images.example.com");
        values.put("IMAGE_STORAGE_PATH_STYLE", "true");
        environment.getPropertySources().addFirst(new MapPropertySource("storage-test", values));
        ConfigDataEnvironmentPostProcessor.applyTo(environment);
        var properties = Binder.get(environment).bind("image.storage", ImageStorageProperties.class)
                .orElseThrow(() -> new IllegalStateException("Storage properties were not bound"));
        assertThat(properties.getRegion()).isEqualTo("auto");
        assertThat(properties.getBucket()).isEqualTo("r2-test-bucket");
        assertThat(properties.getAccessKey()).isEqualTo("r2-test-access");
        assertThat(properties.getSecretKey()).isEqualTo("r2-test-secret");
        assertThat(properties.isPathStyle()).isTrue();
        assertThat(new R2Config().imageUrls(properties).defaultClubPhotoUrl())
                .isEqualTo("https://images.example.com/club-photo/rhythmeet.webp");
    }
    @ParameterizedTest
    @CsvSource({"prod,jandi_band_backend,production", "dev,jandi_band_backend_dev,development"})
    void loadsProfileAndResolvesExternalCredentials(String profile, String name, String deployment) {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("deployment", Map.of(
                "spring.config.location", Path.of("deployment-config/application.properties").toUri().toString(),
                "spring.profiles.active", profile,
                "DB_URL", "jdbc:mysql://test/" + profile,
                "DB_PASSWORD", "literal$dollar#hash",
                "SPRING_DATA_REDIS_HOST", "jandi-band-redis-" + deployment
        )));

        ConfigDataEnvironmentPostProcessor.applyTo(environment);

        assertThat(environment.getProperty("spring.application.name")).isEqualTo(name);
        assertThat(environment.getProperty("spring.data.redis.host")).isEqualTo("jandi-band-redis-" + deployment);
        assertThat(environment.getProperty("spring.data.redis.port")).isEqualTo("6379");
        assertThat(environment.getProperty("spring.datasource.url")).isEqualTo("jdbc:mysql://test/" + profile);
        assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("literal$dollar#hash");
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
    }
}
