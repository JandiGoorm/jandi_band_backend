package com.jandi.band_backend.persistence;

import com.jandi.band_backend.invite.store.InviteCodeService;
import com.jandi.band_backend.invite.store.InviteType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpiringValueRepositoryTest {
    private JdbcTemplate jdbc;
    private ExpiringValueRepository values;

    @BeforeEach
    void setUp() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:expiring-values;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("DROP TABLE IF EXISTS app_expiring_value");
        jdbc.execute("CREATE TABLE app_expiring_value (entry_key VARCHAR(191) PRIMARY KEY, entry_value VARCHAR(255), expires_at TIMESTAMP(6))");
        values = new ExpiringValueRepository(jdbc);
    }

    @Test
    void replacingAndDeletingValuesPreservesRedisSetSemantics() {
        values.put("AbC123", "CLUB:7", Duration.ofDays(7));
        values.put("AbC123", "TEAM:8", Duration.ofDays(7));

        assertThat(values.get("AbC123")).contains("TEAM:8");
        assertThat(values.get("abc123")).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM app_expiring_value", Integer.class)).isEqualTo(1);

        values.delete("AbC123");
        assertThat(values.contains("AbC123")).isFalse();
    }

    @Test
    void expiredKeysAreRejectedBeforeScheduledCleanupAndActiveKeysSurviveCleanup() {
        values.put("expired", "1", Duration.ofHours(1));
        values.put("active", "1", Duration.ofHours(1));
        jdbc.update("UPDATE app_expiring_value SET expires_at = ? WHERE entry_key = ?",
                LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1), "expired");

        assertThat(values.contains("expired")).isFalse();
        assertThat(values.contains("active")).isTrue();
        values.deleteExpired();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM app_expiring_value", Integer.class)).isEqualTo(1);
    }

    @Test
    void inviteCodeTargetFormatAndMissingCodeErrorRemainUnchanged() {
        var service = new InviteCodeService(values);
        ReflectionTestUtils.setField(service, "expireDays", 7);
        service.saveCode(InviteType.CLUB, 42, "Code123ABC");

        assertThat(service.getKeyId("Code123ABC")).isEqualTo("CLUB:42");
        service.deleteRecord("Code123ABC");
        assertThatThrownBy(() -> service.getKeyId("Code123ABC"))
                .hasMessage("권한 오류: code=Code123ABC를 찾을 수 없습니다.");
    }
}
