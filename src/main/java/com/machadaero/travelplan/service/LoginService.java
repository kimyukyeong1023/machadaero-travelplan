package com.machadaero.travelplan.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.machadaero.travelplan.entity.User;
import com.machadaero.travelplan.repository.UserRepository;

// Codex 수정: 회사별 서비스 파일을 이 파일로 합쳤습니다.
// 아래에서 카카오 → 네이버 → 구글 → 우리 DB 회원 조회/저장 순서로 읽으면 됩니다.
@Service
public class LoginService {
    // Codex 수정: 카카오 설정값은 기존 application.properties를 그대로 사용합니다.
    @Value("${login.oauth.kakao.client-id}")
    private String kakaoClientId;

    @Value("${login.oauth.kakao.client-secret}")
    private String kakaoClientSecret;

    @Value("${login.oauth.kakao.redirect-uri}")
    private String kakaoRedirectUri;

    // Codex 수정: 네이버 설정값은 기존 application.properties를 그대로 사용합니다.
    @Value("${NAVER_REST_API_KEY_MACHADAERO}")
    private String naverClientId;

    @Value("${NAVER_REST_API_SECRET_KEY_MACHADAERO}")
    private String naverClientSecret;

    @Value("${login.oauth.naver.redirect-uri}")
    private String naverRedirectUri;

    // Codex 수정: 구글 설정값은 기존 application.properties를 그대로 사용합니다.
    @Value("${login.oauth.google.client-id}")
    private String googleClientId;

    @Value("${login.oauth.google.client-secret}")
    private String googleClientSecret;

    @Value("${login.oauth.google.redirect-uri}")
    private String googleRedirectUri;

    private final UserRepository userRepository;
    private final RestClient restClient;

    public LoginService(UserRepository userRepository) {
        this.userRepository = userRepository;
        // 외부 서버가 응답하지 않을 때 기다릴 최대 시간을 설정합니다.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    // ===== Codex 수정: 카카오 로그인 =====
    // 1. 사용자가 로그인할 제공사 화면의 주소를 만듭니다.
    public String createKakaoLoginUrl(String state) {
        System.out.println("LoginService - createKakaoLoginUrl()");
        checkKakaoSettings();
        // queryParam은 URL 뒤에 붙는 요청 항목이며, encode는 주소에 포함된 특수문자를 처리합니다.
        return UriComponentsBuilder.fromUriString("https://kauth.kakao.com/oauth/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", kakaoClientId)
                .queryParam("redirect_uri", kakaoRedirectUri)
                .queryParam("state", state)
                .build().encode().toUriString();
    }

    // 2. 인가코드(code)를 보내 액세스 토큰을 받습니다. 이 메서드는 토큰만 반환합니다.
    public String requestKakaoAccessToken(String code) {
        System.out.println("LoginService - requestKakaoAccessToken()");
        checkKakaoSettings();
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("redirect_uri", kakaoRedirectUri);
        params.add("code", code);
        if (kakaoClientSecret != null && !kakaoClientSecret.isBlank()) {
            params.add("client_secret", kakaoClientSecret);
        }

        Map<?, ?> result = restClient.post()
                .uri("https://kauth.kakao.com/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(Map.class);

        // Codex 수정: 실패 응답이나 빈 토큰으로 사용자 정보 요청을 계속하지 않습니다.
        if (result == null || result.containsKey("error") || !(result.get("access_token") instanceof String)) {
            throw new IllegalStateException("토큰 발급에 실패했습니다.");
        }
        String accessToken = (String) result.get("access_token");
        if (accessToken.isBlank()) {
            throw new IllegalStateException("토큰이 비어 있습니다.");
        }
        return accessToken;
    }

    // 3. 토큰을 헤더에 담아 사용자 정보를 요청하고, 제공사의 사용자 ID를 반환합니다.
    public String requestKakaoUserInfo(String accessToken) {
        System.out.println("LoginService - requestKakaoUserInfo()");
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("사용자 조회에 필요한 토큰이 없습니다.");
        }
        Map<?, ?> userInfo = restClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);

        if (userInfo == null || userInfo.containsKey("error")) {
            throw new IllegalStateException("사용자 정보 조회에 실패했습니다.");
        }
        // 카카오는 응답의 맨 바깥에 id가 있습니다.
        Object id = userInfo.get("id");
        if (!(id instanceof Number) && !(id instanceof String)) {
            throw new IllegalStateException("카카오 사용자 ID가 없습니다.");
        }
        if (id.toString().isBlank()) {
            throw new IllegalStateException("카카오 사용자 ID가 없습니다.");
        }
        return id.toString();
    }

    // Codex 수정: 키가 아직 없어도 서버는 시작하고, 해당 로그인을 시도할 때 설정 누락을 확인합니다.
    private void checkKakaoSettings() {
        System.out.println("LoginService - checkKakaoSettings()");

        if (kakaoClientId == null || kakaoClientId.isBlank() || kakaoRedirectUri == null
                || kakaoRedirectUri.isBlank()) {
            throw new IllegalStateException("로그인 설정이 필요합니다.");
        }
    }

    // ===== Codex 수정: 네이버 로그인 =====
    // 1. 사용자가 로그인할 제공사 화면의 주소를 만듭니다.
    public String createNaverLoginUrl(String state) {
        System.out.println("LoginService - createNaverLoginUrl()");
        checkNaverSettings();
        // queryParam은 URL 뒤에 붙는 요청 항목이며, encode는 주소에 포함된 특수문자를 처리합니다.
        return UriComponentsBuilder.fromUriString("https://nid.naver.com/oauth2.0/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", naverClientId)
                .queryParam("redirect_uri", naverRedirectUri)
                .queryParam("state", state)
                .build().encode().toUriString();
    }

    // 2. 인가코드(code)를 보내 액세스 토큰을 받습니다. 이 메서드는 토큰만 반환합니다.
    public String requestNaverAccessToken(String code, String state) {
        System.out.println("LoginService - requestNaverAccessToken()");
        checkNaverSettings();
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", naverClientId);
        params.add("redirect_uri", naverRedirectUri);
        params.add("code", code);
        params.add("client_secret", naverClientSecret);
        // 네이버는 토큰 요청에도 로그인 시작 때 만든 state를 함께 보냅니다.
        params.add("state", state);

        Map<?, ?> result = restClient.post()
                .uri("https://nid.naver.com/oauth2.0/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(Map.class);

        // Codex 수정: 실패 응답이나 빈 토큰으로 사용자 정보 요청을 계속하지 않습니다.
        if (result == null || result.containsKey("error") || !(result.get("access_token") instanceof String)) {
            throw new IllegalStateException("토큰 발급에 실패했습니다.");
        }
        String accessToken = (String) result.get("access_token");
        if (accessToken.isBlank()) {
            throw new IllegalStateException("토큰이 비어 있습니다.");
        }
        return accessToken;
    }

    // 3. 토큰을 헤더에 담아 사용자 정보를 요청하고, 제공사의 사용자 ID를 반환합니다.
    public String requestNaverUserInfo(String accessToken) {
        System.out.println("LoginService - requestNaverUserInfo()");
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("사용자 조회에 필요한 토큰이 없습니다.");
        }
        Map<?, ?> userInfo = restClient.get()
                .uri("https://openapi.naver.com/v1/nid/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);

        if (userInfo == null || userInfo.containsKey("error")) {
            throw new IllegalStateException("사용자 정보 조회에 실패했습니다.");
        }
        // 네이버는 response라는 내부 Map에 id가 있으므로 한 번 더 꺼냅니다.
        if (!"00".equals(userInfo.get("resultcode")) || !(userInfo.get("response") instanceof Map)) {
            throw new IllegalStateException("네이버 사용자 정보 조회에 실패했습니다.");
        }
        Map<?, ?> profile = (Map<?, ?>) userInfo.get("response");
        Object id = profile.get("id");
        if (!(id instanceof String) || ((String) id).isBlank()) {
            throw new IllegalStateException("네이버 사용자 ID가 없습니다.");
        }
        return (String) id;
    }

    // Codex 수정: 키가 아직 없어도 서버는 시작하고, 해당 로그인을 시도할 때 설정 누락을 확인합니다.
    private void checkNaverSettings() {
        if (naverClientId == null || naverClientId.isBlank() || naverRedirectUri == null
                || naverRedirectUri.isBlank()) {
            throw new IllegalStateException("로그인 설정이 필요합니다.");
        }
        if (naverClientSecret == null || naverClientSecret.isBlank()) {
            throw new IllegalStateException("Client Secret 설정이 필요합니다.");
        }
    }

    // ===== Codex 수정: 구글 로그인 =====
    // 1. 사용자가 로그인할 제공사 화면의 주소를 만듭니다.
    public String createGoogleLoginUrl(String state) {
        System.out.println("LoginService - createGoogleLoginUrl()");
        checkGoogleSettings();
        // queryParam은 URL 뒤에 붙는 요청 항목이며, encode는 주소에 포함된 특수문자를 처리합니다.
        return UriComponentsBuilder.fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("response_type", "code")
                .queryParam("client_id", googleClientId)
                .queryParam("redirect_uri", googleRedirectUri)
                .queryParam("state", state)
                .queryParam("scope", "openid")
                .build().encode().toUriString();
    }

    // 2. 인가코드(code)를 보내 액세스 토큰을 받습니다. 이 메서드는 토큰만 반환합니다.
    public String requestGoogleAccessToken(String code) {
        System.out.println("LoginService - requestGoogleAccessToken()");
        checkGoogleSettings();
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", googleClientId);
        params.add("redirect_uri", googleRedirectUri);
        params.add("code", code);
        params.add("client_secret", googleClientSecret);

        Map<?, ?> result = restClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(Map.class);

        // Codex 수정: 실패 응답이나 빈 토큰으로 사용자 정보 요청을 계속하지 않습니다.
        if (result == null || result.containsKey("error") || !(result.get("access_token") instanceof String)) {
            throw new IllegalStateException("토큰 발급에 실패했습니다.");
        }
        String accessToken = (String) result.get("access_token");
        if (accessToken.isBlank()) {
            throw new IllegalStateException("토큰이 비어 있습니다.");
        }
        return accessToken;
    }

    // 3. 토큰을 헤더에 담아 사용자 정보를 요청하고, 제공사의 사용자 ID를 반환합니다.
    public String requestGoogleUserInfo(String accessToken) {
        System.out.println("LoginService - requestGoogleUserInfo()");
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("사용자 조회에 필요한 토큰이 없습니다.");
        }
        Map<?, ?> userInfo = restClient.get()
                .uri("https://openidconnect.googleapis.com/v1/userinfo")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);

        if (userInfo == null || userInfo.containsKey("error")) {
            throw new IllegalStateException("사용자 정보 조회에 실패했습니다.");
        }
        // 이 구글 API에서는 고유 사용자 ID의 이름이 sub입니다. 이메일을 회원 번호로 쓰지 않습니다.
        Object id = userInfo.get("sub");
        if (!(id instanceof String) || ((String) id).isBlank()) {
            throw new IllegalStateException("구글 사용자 ID가 없습니다.");
        }
        return (String) id;
    }

    // Codex 수정: 키가 아직 없어도 서버는 시작하고, 해당 로그인을 시도할 때 설정 누락을 확인합니다.
    private void checkGoogleSettings() {
        if (googleClientId == null || googleClientId.isBlank() || googleRedirectUri == null
                || googleRedirectUri.isBlank()) {
            throw new IllegalStateException("로그인 설정이 필요합니다.");
        }
        if (googleClientSecret == null || googleClientSecret.isBlank()) {
            throw new IllegalStateException("Client Secret 설정이 필요합니다.");
        }
    }

    // ===== Codex 수정: 우리 DB 회원 조회 / 저장 =====
    // Codex 수정: 원래 작성하신 Optional + if 형태로 돌렸습니다.
    // 위의 사용자 조회 메서드로 ID를 받은 뒤 회사 이름과 ID를 여기에 전달합니다.
    public User findOrCreateUser(String provider, String providerUserId) {
        System.out.println("LoginService - findOrCreateUser()");
        if (!"KAKAO".equals(provider) && !"NAVER".equals(provider) && !"GOOGLE".equals(provider)) {
            throw new IllegalStateException("지원하지 않는 로그인입니다.");
        }
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new IllegalStateException("사용자 ID가 없습니다.");
        }

        // 1. 제공사와 제공사 사용자 ID가 모두 일치하는 회원을 찾습니다.
        Optional<User> optionalUser = userRepository.findByProviderAndProviderUserId(provider, providerUserId);
        if (optionalUser.isPresent()) {
            return optionalUser.get();
        }

        // 2. 처음 로그인한 사람이면 우리 DB에 회원을 저장합니다.
        User newUser = new User(provider, providerUserId);
        try {
            return userRepository.save(newUser);
        } catch (DataIntegrityViolationException exception) {
            // Codex 수정: 동시에 같은 회원이 가입된 경우 먼저 저장된 회원을 찾아 반환합니다.
            Optional<User> savedUser = userRepository.findByProviderAndProviderUserId(provider, providerUserId);
            if (savedUser.isPresent()) {
                return savedUser.get();
            }
            throw exception;
        }
    }
}
