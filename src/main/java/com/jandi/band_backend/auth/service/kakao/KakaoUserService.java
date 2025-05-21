package com.jandi.band_backend.auth.service.kakao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jandi.band_backend.auth.dto.kakao.KakaoUserInfoDTO;
import com.jandi.band_backend.global.exception.FailKakaoLoginException;
import com.jandi.band_backend.global.exception.FailKakaoReadUserException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Service
public class KakaoUserService {
    @Value("${kakao.user-info-url}")
    private String kakaoUserInfoUri;

    // 카카오 서버로부터 유저 정보 얻기
    public KakaoUserInfoDTO getKakaoUserInfo(String accessToken){
        // 액세스 토큰으로 유저 정보 요청 보내기
        Map body = requestKakaoUserInfo(accessToken);
        Map account = (Map) body.get("kakao_account");
        Map profile = (Map) account.get("profile");

        // 응답에서 유저 정보 추출
        String kakaoOauthId = String.valueOf(body.get("id"));
        String nickname = String.valueOf(profile.get("nickname"));
        String profilePhoto = String.valueOf(profile.get("profile_image_url"));

        return new KakaoUserInfoDTO(
                kakaoOauthId, nickname, profilePhoto
        );
    }

    /// 내부 메소드
    // 카카오 계정 정보 조회를 위한 HTTP 요청 전송
    private ResponseEntity<Map> responseForm(String accessToken){
        // 헤더에 Authorization 추가
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        // 요청 전송
        RestTemplate restTemplate = new RestTemplate();
        return restTemplate.exchange(
                kakaoUserInfoUri,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );
    }

    // 카카오 토큰으로 카카오 계정 정보 요청 및 예외 핸들링
    // 정상일 경우 Map 반환, 오류 발생 시 FailKakaoReadUserException 예외를 던짐
    private Map requestKakaoUserInfo(String accessToken){
        try {
            ResponseEntity<Map> response = responseForm(accessToken);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new FailKakaoReadUserException("카카오 사용자 정보 조회 실패: " + response.getStatusCode());
            }

            return Optional.ofNullable(response.getBody())
                    .orElseThrow(() -> new FailKakaoReadUserException("카카오 응답 없음"));

        }catch (HttpClientErrorException | HttpServerErrorException e){
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

