package com.jandi.band_backend.auth.redis;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;

@Slf4j
@Service
public class TokenBlacklistService {
    private final StringRedisTemplate redisTemplate;
    private final Key secretKey;

    public TokenBlacklistService(
            StringRedisTemplate redisTemplate,
            @Value("${jwt.secret}") String jwtSecret
    ) {
        this.redisTemplate = redisTemplate;
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public void saveToken(String refreshToken) {
        log.info("=== Token Blacklist Debug Info ===");

        // key: blacklist:refreshToken:{hash} 형태 -> 원문 노출을 막고 조회 규칙을 명확히 하기 위함
        String key = tokenToKey(refreshToken);
        log.info("key: {}", key);

        long remainSecond = getRemainSeconds(refreshToken);
        log.info("remainSecond: {}", remainSecond);

        redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(remainSecond));
        log.info("=== Token Blacklist Success ===");
    }

    public boolean isTokenBlacklist(String refreshToken) {
        String key = tokenToKey(refreshToken);
        return redisTemplate.hasKey(key);
    }

    private String tokenToKey(String refreshToken) {
        return "bl:rt:" + sha256Hex(refreshToken);
    }

    private long getRemainSeconds(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Date expiration = claims.getExpiration();
            long remainSeconds = expiration.toInstant().getEpochSecond() - Instant.now().getEpochSecond();
            return Math.max(1, remainSeconds); // 음수가 나오는 것을 방지하기 위해 최소 1초로 설정
        } catch (Exception e) {
            log.error("Fail to parsing: " + e.getMessage());
            return 1; // 파싱 실패 시 1초로 설정
        }
    }
}
