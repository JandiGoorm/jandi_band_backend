package com.jandi.band_backend.auth.store;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import com.jandi.band_backend.persistence.ExpiringValueRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;

@Slf4j
@Service
public class TokenBlacklistService {
    private final ExpiringValueRepository values;
    private final SecretKey secretKey;

    public TokenBlacklistService(
            ExpiringValueRepository values,
            @Value("${jwt.secret}") String jwtSecret
    ) {
        this.values = values;
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public void saveToken(String refreshToken) {
        String key = tokenToKey(refreshToken);
        long remainSecond = getRemainSeconds(refreshToken);

        // 토큰 만료/파싱 과정에서 에러 발생 시 블랙리스트 과정 생략
        if (remainSecond <= 0) {
            log.error("=== Token Blacklist Failed ===");
        }else{
            values.put(key, "1", Duration.ofSeconds(remainSecond));
            log.info("=== Token Blacklist Success ===");
        }
    }

    public boolean isTokenBlacklist(String refreshToken) {
        String key = tokenToKey(refreshToken);
        return values.contains(key);
    }

    private String tokenToKey(String refreshToken) {
        return "bl:rt:" + sha256Hex(refreshToken);
    }

    // return +n: 정상, 0: error(파싱 실패), -n: error(토큰 만료)
    private long getRemainSeconds(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
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
