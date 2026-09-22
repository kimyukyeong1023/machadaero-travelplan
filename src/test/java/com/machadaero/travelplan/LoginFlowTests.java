package com.machadaero.travelplan;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClientException;

import com.machadaero.travelplan.controller.LoginController;
import com.machadaero.travelplan.entity.User;
import com.machadaero.travelplan.repository.UserRepository;
import com.machadaero.travelplan.service.LoginService;

// Codex 작성: 실제 DB·외부 로그인 없이 공통 로그인 경로, state 검증, 회원 구분과 세션 설정을 확인합니다.
class LoginFlowTests {
    private final LoginService loginService = mock(LoginService.class);
    // Codex 수정: LoginService 하나로 합친 후에도 각 회사의 메서드와 세션 처리가 연결되는지 확인합니다.
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        when(loginService.createKakaoLoginUrl(anyString())).thenReturn("https://example.com/kakao");
        when(loginService.createNaverLoginUrl(anyString())).thenReturn("https://example.com/naver");
        when(loginService.createGoogleLoginUrl(anyString())).thenReturn("https://example.com/google");
        mvc = MockMvcBuilders.standaloneSetup(
                new LoginController(loginService)).build();
    }

    private String start(String provider, MockHttpSession session) throws Exception {
        mvc.perform(get("/login/" + provider).session(session))
                .andExpect(status().is3xxRedirection());
        return (String) session.getAttribute("oauthState:" + provider);
    }

    @ParameterizedTest
    @ValueSource(strings = {"kakao", "naver", "google"})
    void successfulCallbackUsesLocalUserIdAndRotatesSession(String provider) throws Exception {
        MockHttpSession session = new MockHttpSession();
        String state = start(provider, session);
        assertNotNull(state);
        assertDoesNotThrow(() -> UUID.fromString(state));
        String previousSessionId = session.getId();
        User user = new User(provider.toUpperCase(Locale.ROOT), "external-id");
        user.setId(42L);
        when(loginService.requestKakaoAccessToken("code")).thenReturn("token");
        when(loginService.requestKakaoUserInfo("token")).thenReturn("external-id");
        when(loginService.requestNaverAccessToken("code", state)).thenReturn("token");
        when(loginService.requestNaverUserInfo("token")).thenReturn("external-id");
        when(loginService.requestGoogleAccessToken("code")).thenReturn("token");
        when(loginService.requestGoogleUserInfo("token")).thenReturn("external-id");
        when(loginService.findOrCreateUser(provider.toUpperCase(Locale.ROOT), "external-id")).thenReturn(user);

        mvc.perform(get("/login/" + provider + "/callback").session(session)
                        .param("code", "code").param("state", state))
                .andExpect(redirectedUrl("/"));
        assertEquals(42L, session.getAttribute("loginUserId"));
        assertNotEquals(previousSessionId, session.getId());
        assertNull(session.getAttribute("oauthState:" + provider));

        // 같은 콜백의 재사용은 차단하며 토큰을 다시 발급하지 않습니다.
        mvc.perform(get("/login/" + provider + "/callback").session(session)
                        .param("code", "code").param("state", state))
                .andExpect(redirectedUrl("/login")).andExpect(flash().attributeExists("loginError"));
        if ("kakao".equals(provider)) {
            verify(loginService, times(1)).requestKakaoAccessToken("code");
            verify(loginService, never()).requestNaverAccessToken(any(), any());
            verify(loginService, never()).requestGoogleAccessToken(any());
        } else if ("naver".equals(provider)) {
            verify(loginService, times(1)).requestNaverAccessToken("code", state);
            verify(loginService, never()).requestKakaoAccessToken(any());
            verify(loginService, never()).requestGoogleAccessToken(any());
        } else {
            verify(loginService, times(1)).requestGoogleAccessToken("code");
            verify(loginService, never()).requestKakaoAccessToken(any());
            verify(loginService, never()).requestNaverAccessToken(any(), any());
        }
        verify(loginService, times(1)).findOrCreateUser(provider.toUpperCase(Locale.ROOT), "external-id");
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "wrong", "expired", "other-provider", "no-session"})
    void rejectsInvalidStateBeforeAnyTokenRequest(String mode) throws Exception {
        MockHttpSession session = new MockHttpSession();
        String state = start(mode.equals("other-provider") ? "kakao" : "naver", session);
        if (mode.equals("expired")) session.setAttribute("oauthStateExpiresAt:naver", 0L);
        var request = get("/login/naver/callback").param("code", "code");
        if (!mode.equals("no-session")) request.session(session);
        if (!mode.equals("missing")) request.param("state", mode.equals("wrong") ? "wrong-state" : state);
        mvc.perform(request).andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("loginError"));
        verify(loginService, never()).requestNaverAccessToken(any(), any());
        verify(loginService, never()).findOrCreateUser(any(), any());
        assertNull(session.getAttribute("loginUserId"));
    }

    @Test
    void userCancellationConsumesStateWithoutLoggingIn() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String state = start("google", session);
        mvc.perform(get("/login/google/callback").session(session)
                        .param("state", state).param("error", "access_denied"))
                .andExpect(redirectedUrl("/login")).andExpect(flash().attributeExists("loginError"));
        assertNull(session.getAttribute("oauthState:google"));
        verify(loginService, never()).requestGoogleAccessToken(any());
        verify(loginService, never()).findOrCreateUser(any(), any());
    }

    @Test
    void apiFailureDoesNotCreateUserOrExposeResponseBody() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String state = start("naver", session);
        when(loginService.requestNaverAccessToken("code", state)).thenThrow(new RestClientException("sensitive-response"));
        var result = mvc.perform(get("/login/naver/callback").session(session)
                        .param("state", state).param("code", "code"))
                .andExpect(redirectedUrl("/login")).andExpect(flash().attributeExists("loginError"))
                .andReturn();
        assertFalse(result.getFlashMap().get("loginError").toString().contains("sensitive-response"));
        assertNull(session.getAttribute("loginUserId"));
        verify(loginService, never()).findOrCreateUser(any(), any());
    }

    @Test
    void missingSettingsShowLoginNoticeWithoutCreatingPendingState() throws Exception {
        when(loginService.createGoogleLoginUrl(anyString())).thenThrow(new IllegalStateException("missing-key"));
        MockHttpSession session = new MockHttpSession();
        mvc.perform(get("/login/google").session(session))
                .andExpect(redirectedUrl("/login")).andExpect(flash().attributeExists("loginError"));
        assertNull(session.getAttribute("oauthState:google"));
    }

    @Test
    void rejectsUnknownProvider() throws Exception {
        mvc.perform(get("/login/unknown")).andExpect(status().isNotFound());
        mvc.perform(get("/login/unknown/callback")).andExpect(status().isNotFound());
        verify(loginService, never()).findOrCreateUser(any(), any());
    }

    @Test
    void eachLoginStartGeneratesFreshState() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String first = start("kakao", session);
        String second = start("kakao", session);
        assertNotEquals(first, second);
        mvc.perform(get("/login/kakao/callback").session(session).param("code", "code").param("state", first))
                .andExpect(redirectedUrl("/login"));
        verify(loginService, never()).requestKakaoAccessToken(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"KAKAO", "NAVER", "GOOGLE"})
    void memberLookupReusesExistingUser(String provider) {
        UserRepository repository = mock(UserRepository.class);
        User user = new User(provider, "12345");
        user.setId(42L);
        when(repository.findByProviderAndProviderUserId(provider, "12345")).thenReturn(Optional.of(user));
        assertSame(user, new LoginService(repository, mock(PasswordEncoder.class)).findOrCreateUser(provider, "12345"));
        verify(repository, never()).save(any());
    }

    @Test
    void newMemberUsesProviderAndExternalId() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        User user = new LoginService(repository, mock(PasswordEncoder.class)).findOrCreateUser("NAVER", "12345");
        assertEquals("NAVER", user.getProvider());
        assertEquals("12345", user.getProviderUserId());
        verify(repository).findByProviderAndProviderUserId("NAVER", "12345");
    }

    @Test
    void concurrentSignupReusesMemberCreatedByOtherRequest() {
        UserRepository repository = mock(UserRepository.class);
        User existing = new User("GOOGLE", "12345");
        existing.setId(99L);
        when(repository.findByProviderAndProviderUserId("GOOGLE", "12345"))
                .thenReturn(Optional.empty()).thenReturn(Optional.of(existing));
        when(repository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate"));
        assertSame(existing, new LoginService(repository, mock(PasswordEncoder.class)).findOrCreateUser("GOOGLE", "12345"));
    }
}
