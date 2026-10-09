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
    @ParameterizedTest
    @CsvSource({"false,ap-northeast-2,jandi-rhythmeet", "true,auto,r2-test-bucket"})
    void retainsAwsDefaultsAndAcceptsR2Overrides(boolean r2, String region, String bucket) {
        var environment = new StandardEnvironment();
        var values = new HashMap<String, Object>();
        values.put("spring.config.location", Path.of("deployment-config/application.properties").toUri().toString());
        values.put("AWS_ACCESS_KEY_ID", "legacy-test-access");
        values.put("AWS_SECRET_ACCESS_KEY", "legacy-test-secret");
        if (r2) {
            values.put("IMAGE_STORAGE_ACCESS_KEY", "r2-test-access");
            values.put("IMAGE_STORAGE_SECRET_KEY", "r2-test-secret");
            values.put("IMAGE_STORAGE_REGION", "auto");
            values.put("IMAGE_STORAGE_ENDPOINT", "https://account.r2.cloudflarestorage.com");
            values.put("IMAGE_STORAGE_BUCKET", "r2-test-bucket");
            values.put("IMAGE_STORAGE_PUBLIC_URL", "https://images.example.com");
            values.put("IMAGE_STORAGE_PATH_STYLE", "true");
        }
        environment.getPropertySources().addFirst(new MapPropertySource("storage-test", values));
        ConfigDataEnvironmentPostProcessor.applyTo(environment);
        var properties = Binder.get(environment).bind("image.storage", ImageStorageProperties.class)
                .orElseThrow(() -> new IllegalStateException("Storage properties were not bound"));
        assertThat(properties.getRegion()).isEqualTo(region);
        assertThat(properties.getBucket()).isEqualTo(bucket);
        assertThat(properties.getAccessKey()).isEqualTo(r2 ? "r2-test-access" : "legacy-test-access");
        assertThat(properties.getSecretKey()).isEqualTo(r2 ? "r2-test-secret" : "legacy-test-secret");
        assertThat(properties.isPathStyle()).isEqualTo(r2);
        assertThat(new S3Config().imageUrls(properties).defaultClubPhotoUrl())
                .isEqualTo((r2 ? "https://images.example.com" : "https://jandi-rhythmeet.s3.ap-northeast-2.amazonaws.com")
                        + "/club-photo/rhythmeet.webp");
    }
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
