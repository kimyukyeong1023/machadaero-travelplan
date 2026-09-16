package com.machadaero.travelplan.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClient;

@Service
public class LoginService {
    @Value("${KAKAO_REST_API_KEY}")
    String kakaoRestApiKey;

    @Value("${KAKAO_REST_API_SECRET_KEY}")
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

    public void requestAccessToken(String code) {
        System.out.println("LoginService - requestAccessToken()");
        RestClient restClient = RestClient.create();
        String redirectUri = "http://localhost:8080/login/kakao/callback";

        MultiValueMap<String, String> requstMap = new LinkedMultiValueMap<>();
        requstMap.add("grant_type", "authorization_code");
        requstMap.add("client_id", kakaoRestApiKey);
        requstMap.add("redirect_uri", redirectUri);
        requstMap.add("code", code);
        requstMap.add("client_secret", kakaoRestApiSecretKey);

        Map reaposeToken  =restClient.post()
                    .uri("https://kauth.kakao.com/oauth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(requstMap)
                    .retrieve()
                    .body(Map.class);
        String token= (String) reaposeToken.get("access_token");
        System.out.println("토큰값: "+token);

    }

}
