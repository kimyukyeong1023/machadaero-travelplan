package com.machadaero.travelplan.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;

import com.machadaero.travelplan.entity.User;
import com.machadaero.travelplan.repository.UserRepository;

@Service
public class LoginService {
    @Autowired
    UserRepository userRepository;

    @Value("${KAKAO_REST_API_KEY_MACHADAERO}")
    String kakaoRestApiKey;

    @Value("${KAKAO_REST_API_SECRET_KEY_MACHADAERO}")
    String kakaoRestApiSecretKey;

    public String CreatLoginUrl() {
        System.out.println("LoginService - CreatLoginUrl()");

        String redirectUri = "http://localhost:8080/login/kakao/callback";

        String loginUrl = "https://kauth.kakao.com/oauth/authorize"
                + "?client_id=" + kakaoRestApiKey
                + "&redirect_uri=" + redirectUri
                + "&response_type=code";

        return loginUrl;
    }

    public User findOrCreateUser(String code) {
        System.out.println("LoginService - findOrCreateUser()");

        String providerUserId = requestAccessToken(code);

        Optional<User> optionalUser = userRepository.findByProviderAndProviderUserId("KAKAO", providerUserId);

        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            return user;

        } else {
            User newUser = new User("KAKAO", providerUserId);
            User saveUser = userRepository.save(newUser);
            return saveUser;
        }


    }

    public String requestAccessToken(String code) {
        System.out.println("LoginService - requestAccessToken()");
        RestClient restClient = RestClient.create();
        String redirectUri = "http://localhost:8080/login/kakao/callback";

        MultiValueMap<String, String> requstMap = new LinkedMultiValueMap<>();
        requstMap.add("grant_type", "authorization_code");
        requstMap.add("client_id", kakaoRestApiKey);
        requstMap.add("redirect_uri", redirectUri);
        requstMap.add("code", code);
        requstMap.add("client_secret", kakaoRestApiSecretKey);

        Map reaposeToken = restClient.post()
                .uri("https://kauth.kakao.com/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(requstMap)
                .retrieve()
                .body(Map.class);
        String token = (String) reaposeToken.get("access_token");
        System.out.println("토큰값: " + token);

        return requestUserInfo(token);

    }

    public String requestUserInfo(String token) {

        RestClient restClient = RestClient.create();

        Map userInfo = restClient.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(Map.class);

        System.out.println(userInfo);
        Object userInfoId = userInfo.get("id");
        String userId = userInfoId.toString();
        System.out.println("유저아이디: " + userId);
        return userId;
    }

}
