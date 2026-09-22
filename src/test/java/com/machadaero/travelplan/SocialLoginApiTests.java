package com.machadaero.travelplan;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.machadaero.travelplan.service.LoginService;
import com.machadaero.travelplan.repository.UserRepository;

// Codex 작성: 실제 키·외부 통신 없이 제공사별 요청 body, 토큰 사용, ID 해석과 실패 응답을 검증합니다.
class SocialLoginApiTests {
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final LoginService loginService = new LoginService(mock(UserRepository.class), mock(PasswordEncoder.class));

    @BeforeEach
    void setUp() {
        // Codex 수정: 합친 서비스에 테스트용 HTTP 요청 도구와 각 회사 설정을 넣습니다.
        ReflectionTestUtils.setField(loginService, "restClient", builder.build());
        for (String provider : new String[] {"kakao", "naver", "google"}) {
            ReflectionTestUtils.setField(loginService, provider + "ClientId", "test-client");
            ReflectionTestUtils.setField(loginService, provider + "ClientSecret", "test-secret");
            ReflectionTestUtils.setField(loginService, provider + "RedirectUri", "http://localhost:8080/login/" + provider + "/callback");
        }
    }

    private String createLoginUrl(String provider, String key, String state) {
        ReflectionTestUtils.setField(loginService, provider + "ClientId", key);
        if ("kakao".equals(provider)) return loginService.createKakaoLoginUrl(state);
        if ("naver".equals(provider)) return loginService.createNaverLoginUrl(state);
        return loginService.createGoogleLoginUrl(state);
    }

    private String requestAccessToken(String provider, String code, String state) {
        if ("kakao".equals(provider)) return loginService.requestKakaoAccessToken(code);
        if ("naver".equals(provider)) return loginService.requestNaverAccessToken(code, state);
        return loginService.requestGoogleAccessToken(code);
    }

    private String requestUserInfo(String provider, String token) {
        if ("kakao".equals(provider)) return loginService.requestKakaoUserInfo(token);
        if ("naver".equals(provider)) return loginService.requestNaverUserInfo(token);
        return loginService.requestGoogleUserInfo(token);
    }

    private String tokenUrl(String provider) {
        return switch (provider) {
            case "kakao" -> "https://kauth.kakao.com/oauth/token";
            case "naver" -> "https://nid.naver.com/oauth2.0/token";
            default -> "https://oauth2.googleapis.com/token";
        };
    }

    private String profileUrl(String provider) {
        return switch (provider) {
            case "kakao" -> "https://kapi.kakao.com/v2/user/me";
            case "naver" -> "https://openapi.naver.com/v1/nid/me";
            default -> "https://openidconnect.googleapis.com/v1/userinfo";
        };
    }

    @ParameterizedTest
    @ValueSource(strings = {"kakao", "naver", "google"})
    void authorizationUrlContainsStateAndCallbackButNoSecret(String provider) {
        String url = createLoginUrl(provider, "test-client", "test-state");
        assertTrue(url.startsWith(switch (provider) {
            case "kakao" -> "https://kauth.kakao.com/oauth/authorize?";
            case "naver" -> "https://nid.naver.com/oauth2.0/authorize?";
            default -> "https://accounts.google.com/o/oauth2/v2/auth?";
        }));
        assertTrue(url.contains("response_type=code"));
        assertTrue(url.contains("client_id=test-client"));
        assertTrue(url.contains("state=test-state"));
        assertTrue(url.contains("/login/" + provider + "/callback"));
        assertFalse(url.contains("test-secret"));
        if (provider.equals("google")) assertTrue(url.contains("scope=openid"));
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"kakao", "naver", "google"})
    void exchangesCodeWithCorrectFormBodyAndReadsProviderUserId(String provider) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", "test-client");
        form.add("client_secret", "test-secret");
        form.add("redirect_uri", "http://localhost:8080/login/" + provider + "/callback");
        form.add("code", "code+with&symbols");
        if (provider.equals("naver")) form.add("state", "test-state");
        server.expect(requestTo(tokenUrl(provider))).andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().formData(form))
                .andRespond(withSuccess("{\"access_token\":\"test-token\"}", MediaType.APPLICATION_JSON));
        String profile = switch (provider) {
            case "kakao" -> "{\"id\":12345}";
            case "naver" -> "{\"resultcode\":\"00\",\"response\":{\"id\":\"12345\"}}";
            default -> "{\"sub\":\"12345\",\"email\":\"different@example.com\"}";
        };
        server.expect(requestTo(profileUrl(provider))).andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess(profile, MediaType.APPLICATION_JSON));

        String token = requestAccessToken(provider, "code+with&symbols", "test-state");
        assertEquals("test-token", token);
        assertEquals("12345", requestUserInfo(provider, token));
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"access_token\":\"\"}", "{\"error\":\"invalid_grant\"}"})
    void rejectsFailedTokenResponses(String response) {
        server.expect(requestTo(tokenUrl("naver")))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        assertThrows(IllegalStateException.class,
                () -> loginService.requestNaverAccessToken("code", "state"));
        server.verify();
    }

    static Stream<Arguments> invalidProfiles() {
        return Stream.of(
                Arguments.of("kakao", "{}"),
                Arguments.of("kakao", "{\"id\":null}"),
                Arguments.of("naver", "{\"resultcode\":\"024\",\"response\":{\"id\":\"12345\"}}"),
                Arguments.of("naver", "{\"resultcode\":\"00\",\"response\":{}}"),
                Arguments.of("google", "{\"email\":\"only-email@example.com\"}"));
    }

    @ParameterizedTest
    @MethodSource("invalidProfiles")
    void rejectsMissingIdOrProfileFailure(String provider, String response) {
        server.expect(requestTo(profileUrl(provider)))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));
        assertThrows(IllegalStateException.class,
                () -> requestUserInfo(provider, "token"));
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"kakao", "naver", "google"})
    void absentKeysFailOnlyWhenStartingLogin(String provider) {
        assertThrows(IllegalStateException.class, () -> createLoginUrl(provider, "", "state"));
        server.verify();
    }

    @Test
    void propagatesHttpErrorForControllerToHandle() {
        server.expect(requestTo(profileUrl("google"))).andRespond(withUnauthorizedRequest());
        assertThrows(RestClientException.class, () -> loginService.requestGoogleUserInfo("token"));
        server.verify();
    }
}
