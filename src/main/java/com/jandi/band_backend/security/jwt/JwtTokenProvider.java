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
    private final long validityInMilliseconds; // 액세스 토큰 유효 기간
    private final long refreshValidityInMilliseconds; // 리프레시 토큰 유효 기간
    private final UserRepository userRepository;
    private final CustomUserDetailsService userDetailsService;

    /// 생성자 주입
    // jwt 시크릿 키, 토큰 만료 시간은 설정 파일에서 주입받도록 함
    public JwtTokenProvider(
            @Value("${jwt.secret}") String jwtSecret,
            @Value("${jwt.access-token-validity}") long validityInMilliseconds,
            @Value("${jwt.refresh-token-validity}") long refreshValidityInMilliseconds,
            UserRepository userRepository,
            CustomUserDetailsService userDetailsService
    ) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.validityInMilliseconds = validityInMilliseconds;
        this.refreshValidityInMilliseconds = refreshValidityInMilliseconds;
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
    }

    /// 액세스 토큰 생성
    // 유저의 kakaoOauthId, role 포함함
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

    /// 리프레시 토큰 생성
    // 유저의 kakaoOauthId 포함함
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

    /// 토큰에서 유저 정보 추출
    // 토큰에서 유저의 kakaoOauthId를 추출함
    public String getKakaoOauthId(String token) {
        // 토큰이 유효하지 않을 경우 InvalidTokenException 예외 던짐
        if(!validateToken(token))
            throw new InvalidTokenException();

        try {
            if(!validateToken(token))
                throw new InvalidTokenException();

            Claims claims = parseClaims(token);

            log.debug("토큰에서 추출한 카카오 계정: {}", claims.getSubject());
            return claims.getSubject();
        } catch (JwtException | IllegalArgumentException e) {
            log.error("토큰에서 카카오 계정 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    /// 토큰 유효성 검사
    // 토큰이 유효할 때 true를 반환함
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            log.error("JWT 토큰 유효성 검사 실패: {}", e.getMessage());
            return false;
        }
    }

    /// 토큰 유효성 검사
    // 액세스 토큰일 때 true를 반환함
    public boolean isAccessToken(String token) {
        try {
            Claims claims = parseClaims(token);

            // 액세스 토큰에만 role 정보가 포함되므로, role 정보 유무로 액세스 토큰인지 검사
            return claims.get("role") != null;
        } catch (Exception e) {
            return false;
        }
    }

    /// 인증된 사용자인지 확인
    public Authentication getAuthentication(String token) {
        // 액세스 토큰이 아닌 경우 유효하지 않은 토큰으로 간주
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

    // token에서 claims 파싱
    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}