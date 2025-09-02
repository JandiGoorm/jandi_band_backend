package com.jandi.band_backend.auth.redis;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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

        // key: blacklist:refreshToken:{hash} 형태 -> 원문 노출을 막고 공간을 절약하기 위함
        String key = tokenToKey(refreshToken);
        log.info("key: {}", key);

        long remainSecond = getRemainSeconds(refreshToken);
        log.info("remainSecond: {}", remainSecond);

        // 토큰 만료/파싱 과정에서 에러 발생 시 블랙리스트 과정 생략
        if (remainSecond <= 0) {
            log.error("=== Token Blacklist Failed ===");
        }else{
            redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(remainSecond));
            log.info("=== Token Blacklist Success ===");
        }
    }

    public boolean isTokenBlacklist(String refreshToken) {
        String key = tokenToKey(refreshToken);
        return redisTemplate.hasKey(key);
    }

    private String tokenToKey(String refreshToken) {
        return "bl:rt:" + sha256Hex(refreshToken);
    }

    // return +n: 정상, 0: error(파싱 실패), -n: error(토큰 만료)
    private long getRemainSeconds(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Date expiration = claims.getExpiration();

            long remainSeconds = expiration.toInstant().getEpochSecond() - Instant.now().getEpochSecond();
            if(remainSeconds <= 0){
                log.debug("-- Token is expired!");
            }
            return remainSeconds;
        } catch (Exception e) {
            log.debug("-- Fail to parsing: {}", e.getMessage());
            return 0; // 저장하지 않음
        }
    }
}
