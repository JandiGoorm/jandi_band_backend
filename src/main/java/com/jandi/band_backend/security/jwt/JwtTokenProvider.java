package com.jandi.band_backend.security.jwt;

import com.jandi.band_backend.global.exception.InvalidTokenException;
import com.jandi.band_backend.global.exception.UserNotFoundException;
import com.jandi.band_backend.security.CustomUserDetailsService;
import com.jandi.band_backend.user.entity.Users;
import com.jandi.band_backend.user.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {
    private final Key secretKey;
    private final long validityInMilliseconds;
    private final long refreshValidityInMilliseconds;
    private final long refreshTokenReissueThreshold;
    private final UserRepository userRepository;
    private final CustomUserDetailsService userDetailsService;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.access-token-validity}") long validityInMilliseconds,
            @Value("${jwt.refresh-token-validity}") long refreshValidityInMilliseconds,
            @Value("${jwt.refresh-token-reissue-threshold}") long refreshTokenReissueThreshold,
            UserRepository userRepository,
            CustomUserDetailsService userDetailsService
    ) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.validityInMilliseconds = validityInMilliseconds;
        this.refreshTokenReissueThreshold = refreshTokenReissueThreshold;
        this.refreshValidityInMilliseconds = refreshValidityInMilliseconds;
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
    }

    public String generateAccessToken(String kakaoOauthId) {
        Users user = userRepository.findByKakaoOauthId(kakaoOauthId)
                .orElseThrow(UserNotFoundException::new);

        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityInMilliseconds);
        String role = "ROLE_" + user.getAdminRole().name();

        String token = Jwts.builder()
                .setSubject(kakaoOauthId)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(secretKey)
                .compact();

        log.debug("액세스 토큰 생성 완료: 사용자 카카오 계정={}, 역할={}, 만료 시간={}", kakaoOauthId, role, expiry);
        return token;
    }

    public String generateRefreshToken(String kakaoOauthId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshValidityInMilliseconds);

        String token = Jwts.builder()
                .setSubject(kakaoOauthId)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(secretKey)
                .compact();

        log.debug("리프레시 토큰 생성 완료: 사용자 카카오 계정={}, 만료 시간={}", kakaoOauthId, expiry);
        return token;
    }

    public String ReissueRefreshToken(String originalRefreshToken) {
        Claims claims = parseClaims(originalRefreshToken);
        Date expiration = claims.getExpiration();
        long remainTime = expiration.getTime() - System.currentTimeMillis();

        return (remainTime < refreshTokenReissueThreshold) ?
                generateRefreshToken(originalRefreshToken) : originalRefreshToken;
    }

    public String getKakaoOauthId(String token) {
        try {
            Claims claims = parseClaims(token);
            log.debug("토큰에서 추출한 카카오 계정: {}", claims.getSubject());
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            log.error("토큰에서 카카오 계정 추출 실패: {}", e.getMessage());
            throw new InvalidTokenException();
        }
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            log.error("JWT 토큰 유효성 검사 실패: {}", e.getMessage());
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        try {
            Claims claims = parseClaims(token);
            // 액세스 토큰에만 role 정보가 포함되므로, role 정보 유무로 액세스 토큰인지 검사
            return claims.get("role") != null;
        } catch (Exception e) {
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        if (!isAccessToken(token)) {
            throw new InvalidTokenException();
        }

        // 예외를 그대로 전파하여 필터에서 처리되도록 함
        String kakaoOauthId = getKakaoOauthId(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(kakaoOauthId);
        return new UsernamePasswordAuthenticationToken(
                userDetails,
                "",
                userDetails.getAuthorities()
        );
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}