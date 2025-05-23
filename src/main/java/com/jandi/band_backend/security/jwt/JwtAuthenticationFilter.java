package com.jandi.band_backend.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jandi.band_backend.global.ApiResponse;
import com.jandi.band_backend.global.exception.InvalidTokenException;
import com.jandi.band_backend.security.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper; // Spring이 내부적으로 등록한 ObjectMapper를 주입하여 재사용성을 높임

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        try {
            // Authorization 헤더가 없으면 JWT 인증 생략하고 다음 필터로 진행
            if (header == null || !header.startsWith("Bearer ")){
                filterChain.doFilter(request, response);
                return;
            }

            // 토큰이 유효하지 않다면 InvalidTokenException 예외 던짐
            String token = header.replace("Bearer ", "");
            if (!jwtTokenProvider.validateToken(token)) {
                throw new InvalidTokenException();
            }

            // 유효한 토큰에 대해서만 다음 필터로 요청을 넘김
            Authentication auth = jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
            
            // userId를 요청 속성에 설정
            if (auth.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
                request.setAttribute("userId", userDetails.getUserId());
                log.debug("Set userId in request attributes: {}", userDetails.getUserId());
            }
            
            filterChain.doFilter(request, response);
        } catch (InvalidTokenException e) {
            // 형식이 올바르지 않거나 미인가된 토큰일 시 SecurityContext 삭제
            SecurityContextHolder.clearContext();

            // ApiResponse 형식대로 오류 응답 생성
            ApiResponse<?> errorResponse = ApiResponse.error(e.getMessage(), "INVALID_TOKEN");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        }
    }
}
