package com.jandi.band_backend.auth.service.kakao;

import tools.jackson.databind.ObjectMapper;
import com.jandi.band_backend.auth.dto.kakao.KakaoUserInfoDTO;
import com.jandi.band_backend.global.exception.FailKakaoLoginException;
import com.jandi.band_backend.global.exception.FailKakaoReadUserException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import tools.jackson.core.JacksonException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class KakaoUserService {
    @Value("${kakao.user-info-url}")
    private String kakaoUserInfoUri;
    @Value("${kakao.user-unlink-url}")
    private String kakaoUserUnlinkUri;
    @Value("${kakao.admin-key}")
    private String kakaoAdminKey;

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

    // 카카오 계정 연결 끊기
    public void unlink(String kakaoOauthId) {
        // 액세스 토큰으로 유저 정보 요청 보내기
        Map body = requestKakaoUserUnlink(kakaoOauthId);
        log.info("body: {}", body);
        // 응답 검토
        String returnedId = String.valueOf(body.get("id"));
        if(returnedId == null || !returnedId.equals(kakaoOauthId)) {
            throw new FailKakaoReadUserException("카카오 연결 끊기 실패: 아이디가 일치하지 않습니다");
        }
    }

    /// 내부 메소드
    // 카카오 토큰으로 카카오 계정 정보 요청 및 예외 핸들링
    // 정상일 경우 Map 반환, 오류 발생 시 FailKakaoReadUserException 예외를 던짐
    private Map requestKakaoUserInfo(String accessToken){
        try {
            // 헤더에 Authorization 추가
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            // 요청 전송
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.exchange(
                    kakaoUserInfoUri,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new FailKakaoReadUserException("카카오 연결 끊기 실패: " + response.getStatusCode());
            }

            return Optional.ofNullable(response.getBody())
                    .orElseThrow(() -> new FailKakaoReadUserException("카카오 응답 없음"));

        }catch (HttpClientErrorException | HttpServerErrorException e){
            try {
                // 카카오 오류 응답을 적절히 파싱하여 에러 처리
                Map errorBody = tools.jackson.databind.json.JsonMapper.builder().build().readValue(e.getResponseBodyAsString(), Map.class);
                throw new FailKakaoLoginException(errorBody);

            } catch (JacksonException ex) {
                throw new FailKakaoLoginException("카카오 응답 파싱 실패");
            }
        }
    }

    // 회원탈퇴
    private Map requestKakaoUserUnlink(String kakaoOauthId) {
        try {
            // 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoAdminKey);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            // 바디 파라미터 설정 (user_id 고정)
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("target_id_type", "user_id");
            body.add("target_id", kakaoOauthId);

            // 요청 생성
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            // RestTemplate로 POST 요청
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    kakaoUserUnlinkUri,
                    request,
                    Map.class
            );

            log.info("response: {}", response);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new FailKakaoReadUserException("카카오 사용자 연결 끊기 실패: " + response.getStatusCode());
            }

            return Optional.ofNullable(response.getBody())
                    .orElseThrow(() -> new FailKakaoReadUserException("카카오 응답 없음"));

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            try {
                Map errorBody = tools.jackson.databind.json.JsonMapper.builder().build().readValue(e.getResponseBodyAsString(), Map.class);
                throw new FailKakaoLoginException(errorBody);
            } catch (JacksonException ex) {
                throw new FailKakaoLoginException("카카오 응답 파싱 실패");
            }
        }
    }

}

