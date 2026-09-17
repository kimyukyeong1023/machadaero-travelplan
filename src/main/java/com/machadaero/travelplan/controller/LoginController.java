package com.machadaero.travelplan.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.machadaero.travelplan.entity.User;
import com.machadaero.travelplan.service.LoginService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


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
    public String  kakaoCallback(@RequestParam ("code") String code,
                                    HttpServletRequest request){
        System.out.println("LoginController - kakaoCallback()");
        System.out.println("인가코드: "+code);
        User user= loginService.findOrCreateUser(code);

        //기존 세션을 가져오거나 새로 생성
        HttpSession session= request.getSession();
        //로그인 전에 누군가 알고 있던 세션 ID를 로그인 후에도 이용하는 공격을 막기 위해서
        request.changeSessionId();

        //세션에 "loginUserId"라는 이름으로 우리 DB 회원 번호인 Long 값을 저장해.
        //  카카오 사용자 ID나 액세스 토큰을 넣는 게 아니야.
        session.setAttribute("loginUserId", user.getId());

        //쿠키에 세션 ID를 넣어 브라우저로 보내는 일은 톰캣이 처리
        return "redirect:/";
        
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        System.out.println("LoginController - logout()");

        // 이 요청에 연결된 유효한 세션을 가져오는 메서드야.
        //  인자 false는 “세션이 없어도 새로 만들지 마”라는 뜻
        HttpSession session=request.getSession(false);

        // 세션이 있는지 확인 후 해당 세션 무효화
        if (session!=null) {
            session.invalidate();
            
        }
        
        return "redirect:/";
    }
    
}
