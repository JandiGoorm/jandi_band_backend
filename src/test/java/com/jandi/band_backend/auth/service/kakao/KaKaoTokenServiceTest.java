package com.jandi.band_backend.auth.service.kakao;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class KaKaoTokenServiceTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "local-test +&/"})
    void includesClientSecretOnlyWhenConfigured(String secret) throws Exception {
        try (var server = new MockWebServer()) {
            server.start();
            server.enqueue(new MockResponse().setHeader("Content-Type", "application/json")
                    .setBody("{\"access_token\":\"local-access\",\"refresh_token\":\"local-refresh\",\"expires_in\":3600}"));
            var service = new KaKaoTokenService();
            ReflectionTestUtils.setField(service, "kakaoRestApiKey", "local-client");
            ReflectionTestUtils.setField(service, "kakaoRedirectUri", "http://localhost:19090/callback");
            ReflectionTestUtils.setField(service, "kakaoTokenUri", server.url("/oauth/token").toString());
            ReflectionTestUtils.setField(service, "kakaoClientSecret", secret);

            var token = service.getKakaoToken("local-code");
            var request = server.takeRequest(5, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            Map<String, String> form = Arrays.stream(request.getBody().readUtf8().split("&"))
                    .map(pair -> pair.split("=", 2))
                    .collect(Collectors.toMap(pair -> pair[0], pair -> URLDecoder.decode(pair[1], StandardCharsets.UTF_8)));
            assertThat(form).containsEntry("grant_type", "authorization_code")
                    .containsEntry("client_id", "local-client")
                    .containsEntry("redirect_uri", "http://localhost:19090/callback")
                    .containsEntry("code", "local-code");
            if (secret == null || secret.isBlank()) {
                assertThat(form).doesNotContainKey("client_secret");
            } else {
                assertThat(form).containsEntry("client_secret", secret);
            }
            assertThat(token.getAccessToken()).isEqualTo("local-access");
        }
    }
}
