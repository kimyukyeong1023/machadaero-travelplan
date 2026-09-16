package com.machadaero.travelplan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.machadaero.travelplan.service.LoginService;

@Controller
public class LoginController {

    LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }
    @GetMapping("/login")
    public String showLoginPage() {
        System.out.println("LoginController - showLoginPage()");
        return "login";
    }
    @GetMapping ("/login/kakao")
    public  String kakaoLogin(){
        System.out.println("LoginController - kakaoLogin()");
        String loginUrl=loginService.CreatLoginUrl();


        return "redirect:"+loginUrl;
    }
    @GetMapping ("/login/kakao/callback")
    public String  kakaoCallback(@RequestParam ("code") String code){
        System.out.println("LoginController - kakaoCallback()");
        System.out.println("인가코드: "+code);
        loginService.requestAccessToken(code);
        return "index";
        
    }
}
