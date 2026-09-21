package com.machadaero.travelplan.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.machadaero.travelplan.entity.User;
import com.machadaero.travelplan.service.LoginService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {
    @Value("${KAKAO_REST_API_KEY_MACHADAERO}")
    String kakaoRestApiKey;

    // Codex 수정: 로그인에 필요한 서비스는 LoginService 하나만 사용합니다.
    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @GetMapping("/login")
    public String showLoginPage() {
        System.out.println("LoginController - showLoginPage()");
        return "login";
    }

    // Codex 수정: Kakao 로그인 버튼 → state 생성 → 로그인 URL 생성 → 해당 회사 로그인 화면으로 이동.
    @GetMapping("/login/kakao")
    public String kakaoLogin(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        System.out.println("LoginController - kakaoLogin()");
        //UUID는 중복될 가능성이 매우 낮은 식별자를 만들 때 사용하는 자바 기본 클래스
        //randomUUID(): 무작위 UUID를 생성하는 메서드
        //이 프로젝트에서는 로그인 요청을 확인하는 일회용 확인표로 사용
        //이 확인이 없으면, 우리 사이트에서 해당 브라우저가 시작하지 않은 로그인 응답도 처리할 위험이 있어요.
        String state = UUID.randomUUID().toString();
        try {
            String loginUrl = loginService.createKakaoLoginUrl(state);
            saveState("kakao", state, request);
            return "redirect:" + loginUrl;
        } catch (IllegalStateException exception) {
            return loginFailure(redirectAttributes, "이 로그인은 아직 준비 중입니다. 다른 로그인 방법을 이용해 주세요.");
        }
    }

    // Codex 수정: Kakao에서 돌아오면 아래 순서대로 직접 호출합니다.
    @GetMapping("/login/kakao/callback")
    public String kakaoCallback(@RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            HttpServletRequest request, RedirectAttributes redirectAttributes) {
        System.out.println("LoginController - kakaoCallback()");
        if (!isValidState("kakao", state, request)) {
            return loginFailure(redirectAttributes, "로그인 요청이 만료되었거나 올바르지 않습니다. 다시 로그인해 주세요.");
        }
        if (error != null || code == null || code.isBlank()) {
            return loginFailure(redirectAttributes, "로그인이 취소되었거나 승인되지 않았습니다. 다시 시도해 주세요.");
        }

        try {
            // 1. 인가코드로 토큰 받기 → 2. 토큰으로 제공사의 사용자 ID 받기
            String accessToken = loginService.requestKakaoAccessToken(code);
            String providerUserId = loginService.requestKakaoUserInfo(accessToken);
            // 3. 우리 회원 찾기/생성 → 4. 우리 회원 번호를 세션에 저장
            User user = loginService.findOrCreateUser("KAKAO", providerUserId);
            saveLoginSession(user, request);
            return "redirect:/";
        } catch (RestClientException | IllegalStateException | DataAccessException exception) {
            // Codex 수정: 실패 원문의 토큰·개인정보는 출력하지 않고 사용자에게 재시도를 안내합니다.
            return loginFailure(redirectAttributes, "로그인 처리에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    // Codex 수정: Naver 로그인 버튼 → state 생성 → 로그인 URL 생성 → 해당 회사 로그인 화면으로 이동.
    @GetMapping("/login/naver")
    public String naverLogin(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        System.out.println("LoginController - naverLogin()");
        String state = UUID.randomUUID().toString();
        try {
            String loginUrl = loginService.createNaverLoginUrl(state);
            System.out.println(loginUrl);
            saveState("naver", state, request);
            return "redirect:" + loginUrl;
        } catch (IllegalStateException exception) {
            return loginFailure(redirectAttributes, "이 로그인은 아직 준비 중입니다. 다른 로그인 방법을 이용해 주세요.");
        }
    }

    // Codex 수정: Naver에서 돌아오면 아래 순서대로 직접 호출합니다.
    @GetMapping("/login/naver/callback")
    public String naverCallback(@RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            HttpServletRequest request, RedirectAttributes redirectAttributes) {
        System.out.println("LoginController - naverCallback()");
        if (!isValidState("naver", state, request)) {
            return loginFailure(redirectAttributes, "로그인 요청이 만료되었거나 올바르지 않습니다. 다시 로그인해 주세요.");
        }
        if (error != null || code == null || code.isBlank()) {
            return loginFailure(redirectAttributes, "로그인이 취소되었거나 승인되지 않았습니다. 다시 시도해 주세요.");
        }

        try {
            // 1. 인가코드로 토큰 받기 → 2. 토큰으로 제공사의 사용자 ID 받기
            String accessToken = loginService.requestNaverAccessToken(code, state);
            String providerUserId = loginService.requestNaverUserInfo(accessToken);
            // 3. 우리 회원 찾기/생성 → 4. 우리 회원 번호를 세션에 저장
            User user = loginService.findOrCreateUser("NAVER", providerUserId);
            saveLoginSession(user, request);
            return "redirect:/";
        } catch (RestClientException | IllegalStateException | DataAccessException exception) {
            // Codex 수정: 실패 원문의 토큰·개인정보는 출력하지 않고 사용자에게 재시도를 안내합니다.
            return loginFailure(redirectAttributes, "로그인 처리에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    // Codex 수정: Google 로그인 버튼 → state 생성 → 로그인 URL 생성 → 해당 회사 로그인 화면으로 이동.
    @GetMapping("/login/google")
    public String googleLogin(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        System.out.println("LoginController - googleLogin()");
        String state = UUID.randomUUID().toString();
        try {
            String loginUrl = loginService.createGoogleLoginUrl(state);
            saveState("google", state, request);
            return "redirect:" + loginUrl;
        } catch (IllegalStateException exception) {
            return loginFailure(redirectAttributes, "이 로그인은 아직 준비 중입니다. 다른 로그인 방법을 이용해 주세요.");
        }
    }

    // Codex 수정: Google에서 돌아오면 아래 순서대로 직접 호출합니다.
    @GetMapping("/login/google/callback")
    public String googleCallback(@RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            HttpServletRequest request, RedirectAttributes redirectAttributes) {
        System.out.println("LoginController - googleCallback()");
        if (!isValidState("google", state, request)) {
            return loginFailure(redirectAttributes, "로그인 요청이 만료되었거나 올바르지 않습니다. 다시 로그인해 주세요.");
        }
        if (error != null || code == null || code.isBlank()) {
            return loginFailure(redirectAttributes, "로그인이 취소되었거나 승인되지 않았습니다. 다시 시도해 주세요.");
        }

        try {
            // 1. 인가코드로 토큰 받기 → 2. 토큰으로 제공사의 사용자 ID 받기
            String accessToken = loginService.requestGoogleAccessToken(code);
            String providerUserId = loginService.requestGoogleUserInfo(accessToken);
            // 3. 우리 회원 찾기/생성 → 4. 우리 회원 번호를 세션에 저장
            User user = loginService.findOrCreateUser("GOOGLE", providerUserId);
            saveLoginSession(user, request);
            return "redirect:/";
        } catch (RestClientException | IllegalStateException | DataAccessException exception) {
            // Codex 수정: 실패 원문의 토큰·개인정보는 출력하지 않고 사용자에게 재시도를 안내합니다.
            return loginFailure(redirectAttributes, "로그인 처리에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    // Codex 수정: 아래 보조 메서드는 세 제공사에서 똑같이 사용하는 세션 처리만 담당합니다.
    // state는 로그인 버튼을 누른 브라우저가 맞는지 확인할 임의의 문자열입니다.
    private void saveState(String provider, String state, HttpServletRequest request) {
        HttpSession session = request.getSession();
        synchronized (session) { // 동시에 시작된 요청의 state와 만료 시간이 섞이지 않게 저장합니다.
            session.setAttribute("oauthState:" + provider, state);
            session.setAttribute("oauthStateExpiresAt:" + provider, System.currentTimeMillis() + 10 * 60 * 1000L);
        }
    }

    //로그인 요청의 유효성을 검사
    private boolean isValidState(String provider, String state, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        //synchronized 여러 요청이 동시에 같은 코드를 실행하지 못하도록 잠그는 자바 키워드
        //같은 세션 객체를 잠금으로 사용하는 블록에는 한 번에 한 요청만 들어갑니다.
        synchronized (session) { // 같은 state를 동시에 두 번 사용하지 못하도록 확인과 삭제를 함께 합니다.
            String savedState = (String) session.getAttribute("oauthState:" + provider);
            Long expiresAt = (Long) session.getAttribute("oauthStateExpiresAt:" + provider);
            session.removeAttribute("oauthState:" + provider);
            session.removeAttribute("oauthStateExpiresAt:" + provider);

            if (state == null || savedState == null || expiresAt == null) {
                return false;
            }
            if (System.currentTimeMillis() >= expiresAt) {
                return false;
            }
            return savedState.equals(state);
        }
    }

    private void saveLoginSession(User user, HttpServletRequest request) {
        HttpSession session = request.getSession();
        request.changeSessionId();
        //로그인 정보는 저장하고 OAuth 임시 정보만 삭제
        session.setAttribute("loginUserId", user.getId());

        // Codex 수정: 로그인이 끝났으므로 다른 탭에 남아 있던 로그인 요청도 정리합니다.
        //카카오 로그인이 성공했으니 네이버·구글을 포함해 진행 중이던 로그인 시도의 임시 값도 정리
        String[] providers = {"kakao", "naver", "google"};
        for (String provider : providers) {
            session.removeAttribute("oauthState:" + provider);
            session.removeAttribute("oauthStateExpiresAt:" + provider);
        }
    }

    private String loginFailure(RedirectAttributes redirectAttributes, String message) {
        redirectAttributes.addFlashAttribute("loginError", message);
        return "redirect:/login";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        System.out.println("LoginController - logout()");

        // 이 요청에 연결된 유효한 세션을 가져오는 메서드야.
        // 인자 false는 “세션이 없어도 새로 만들지 마”라는 뜻
        HttpSession session = request.getSession(false);

        // 세션이 있는지 확인 후 해당 세션 무효화
        if (session != null) {
            session.invalidate();

        }
        String kakaoLogoutUrl = "https://kauth.kakao.com/oauth/logout"
                + "?client_id=" +kakaoRestApiKey
                + "&logout_redirect_uri=http://localhost:8080" ;

        return "redirect:"+kakaoLogoutUrl;
    }

}
