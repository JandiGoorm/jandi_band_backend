package com.jandi.band_backend.security.jwt;

import tools.jackson.databind.ObjectMapper;
import com.jandi.band_backend.global.dto.CommonRespDTO;
import com.jandi.band_backend.global.exception.InvalidTokenException;
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
import org.springframework.lang.Nullable;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        try {
            if (header == null || !header.startsWith("Bearer ")){
                filterChain.doFilter(request, response);
                return;
            }

            String token = header.replace("Bearer ", "");
            if (!jwtTokenProvider.validateToken(token)) {
                throw new InvalidTokenException();
            }

            Authentication auth = jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
            
            filterChain.doFilter(request, response);
        } catch (InvalidTokenException e) {
            // 형식이 올바르지 않거나 미인가된 토큰일 시 SecurityContext 삭제
            SecurityContextHolder.clearContext();

            CommonRespDTO<?> errorResponse = CommonRespDTO.error(e.getMessage(), "INVALID_TOKEN");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json; charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        }
    }
}
