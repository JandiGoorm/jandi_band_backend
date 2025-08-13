package com.jandi.band_backend.config;

import com.jandi.band_backend.security.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // SecurityFilterChain을 사용하여 보안 설정 정의
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(request -> {
                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowedOrigins(List.of(
                            "http://localhost:5173",
                            "https://rhythmeet.netlify.app",
                            "https://rhythmeetdevelop.netlify.app",
                            "https://rhythmeet.site/",
                            "https://rhythmeet-be.yeonjae.kr"  // Swagger UI 접근을 위한 백엔드 도메인
                    ));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                    config.setAllowedHeaders(List.of("*"));
                    config.setExposedHeaders(List.of("Authorization"));
                    config.setAllowCredentials(true);
                    config.setMaxAge(3600L);
                    return config;
                }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/health",
                                "/api/clubs/**",
                                "/api/images/**",
                                "/api/promos",           // 공연 홍보 목록 조회
                                "/api/promos/{promoId}", // 공연 홍보 상세 조회
                                "/api/promos/search",    // 공연 홍보 검색 (JPA)
                                "/api/promos/filter",    // 공연 홍보 필터링 (JPA)
                                "/api/promos/map",       // 공연 홍보 지도 검색 (JPA)
                                "/api/promos/status",    // 공연 홍보 상태별 필터링 (JPA)
                                "/api/promos/*/comments", // 공연 홍보 댓글 목록 조회
                                "/api/promos/reports", // 공연 홍보 신고
                                "/api/promos/comments/reports", // 공연 홍보 댓글 신고
                                // 검색 API (인증 없이  접근 가능)
                                "/api/search/**",
                                // 관리자 API (개발/테스트용)
                                "/api/admin/**",
                                // Swagger UI 관련 경로
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                // Actuator 모니터링 엔드포인트 (Prometheus & Grafana)
                                "/actuator/**"
                        ).permitAll()
                        // CORS preflight 요청 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class) // JWT 필터 등록
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    // 비밀번호 인코더 설정
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}



