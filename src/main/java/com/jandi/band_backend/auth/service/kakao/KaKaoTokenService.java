package com.jandi.band_backend.auth.service.kakao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jandi.band_backend.auth.dto.kakao.KakaoTokenRespDTO;
import com.jandi.band_backend.global.exception.FailKakaoLoginException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;

@Service
public class KaKaoTokenService {
    @Value("${kakao.rest-api-key}")
    private String kakaoRestApiKey;

    @Value("${kakao.redirect-url}")
    private String kakaoRedirectUri;

    @Value("${kakao.token-url}")
    private String kakaoTokenUri;

    // 인가 코드를 이용해 카카오 액세스/리프레시 토큰을 요청하고 응답 데이터를 DTO로 반환
    public KakaoTokenRespDTO getKakaoToken(String code){
        // 인가 코드로 토큰 발급 요청 보내기
        Map body = requestKakaoToken(code);

        // 응답에서 토큰 추출
        String accessToken = (String) body.get("access_token");
        String refreshToken = (String) body.get("refresh_token");
        Integer expiresIn = (Integer) body.get("expires_in");

        return new KakaoTokenRespDTO(
                accessToken,
                refreshToken,
                (expiresIn != null) ? expiresIn : 0
        );
    }

    /// 내부 메소드
    // 카카오 로그인 토큰 발급을 위한 HTTP 요청 전송
    private ResponseEntity<Map> responseForm(String code){
        // 헤더
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 요청 파라미터
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoRestApiKey);
        params.add("redirect_uri", kakaoRedirectUri);
        params.add("code", code);

        // 요청 전송
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.postForEntity(
                kakaoTokenUri,
                new HttpEntity<>(params, headers),
                Map.class
        );
    }

    // 카카오 토큰 요청 처리 및 예외 핸들링
    // 정상일 경우 Map 반환, 오류 발생 시 FailKakaoLoginException 예외를 던짐
    private Map requestKakaoToken(String code){
        try{
            ResponseEntity<Map> response = responseForm(code);

            // 잘못된 응답 처리
            if(!response.getStatusCode().is2xxSuccessful() || response.getBody() == null){
                throw new FailKakaoLoginException("카카오 토큰 발급 실패: " + response.getStatusCode());
            }

            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException e){
            try {
                // 카카오 오류 응답을 적절히 파싱하여 에러 처리
                Map errorBody = new ObjectMapper().readValue(e.getResponseBodyAsString(), Map.class);
                throw new FailKakaoLoginException(errorBody);

            } catch (IOException ex) {
                throw new FailKakaoLoginException("카카오 응답 파싱 실패");
            }
        }
    }
}
