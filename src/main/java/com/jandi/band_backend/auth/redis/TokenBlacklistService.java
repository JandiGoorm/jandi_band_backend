package com.jandi.band_backend.auth.redis;

import com.jandi.band_backend.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {
    private final StringRedisTemplate redisTemplate;
    private final JwtTokenProvider jwtTokenProvider;

    public void saveToken(String refreshToken) {
        // key: blacklist:refreshToken:{hash} 형태 -> 원문 노출을 막고 조회 규칙을 명확히 하기 위함
        String key = "bl:rt:" + sha256Hex(refreshToken);

        // 만료 임박/시계차로 음수가 나오는 것을 방지하기 위해 최소 1초로 설정
        long remainSecond = Math.max(1, jwtTokenProvider.getRemainMillSecond(refreshToken));
        redisTemplate.opsForValue().set(key, "1", Duration.ofSeconds(remainSecond));
    }

    public boolean isTokenBlacklist(String refreshToken) {
        String key = "bl:rt:" + sha256Hex(refreshToken);
        return redisTemplate.hasKey(key);
    }
}
