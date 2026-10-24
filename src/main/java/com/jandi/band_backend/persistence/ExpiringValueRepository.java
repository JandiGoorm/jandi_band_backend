package com.jandi.band_backend.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ExpiringValueRepository {
    private final JdbcTemplate jdbcTemplate;

    public void put(String key, String value, Duration ttl) {
        LocalDateTime expiresAt = LocalDateTime.now(ZoneOffset.UTC).plus(ttl);
        jdbcTemplate.update("""
                INSERT INTO app_expiring_value (entry_key, entry_value, expires_at)
                VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE entry_value = ?, expires_at = ?
                """, key, value, expiresAt, value, expiresAt);
    }

    public Optional<String> get(String key) {
        return jdbcTemplate.query("""
                SELECT entry_value FROM app_expiring_value
                WHERE entry_key = ? AND expires_at > ?
                """, (row, index) -> row.getString(1), key, LocalDateTime.now(ZoneOffset.UTC))
                .stream().findFirst();
    }

    public boolean contains(String key) {
        return get(key).isPresent();
    }

    public void delete(String key) {
        jdbcTemplate.update("DELETE FROM app_expiring_value WHERE entry_key = ?", key);
    }

    @Scheduled(fixedDelayString = "${app.expiring-values.cleanup-delay-ms:3600000}")
    public void deleteExpired() {
        jdbcTemplate.update("DELETE FROM app_expiring_value WHERE expires_at <= ?",
                LocalDateTime.now(ZoneOffset.UTC));
    }
}
