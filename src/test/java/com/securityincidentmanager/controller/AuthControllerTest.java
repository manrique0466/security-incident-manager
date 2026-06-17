package com.securityincidentmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securityincidentmanager.auth.AuthService;
import com.securityincidentmanager.auth.CustomUserDetailsService;
import com.securityincidentmanager.auth.JwtTokenService;
import com.securityincidentmanager.auth.TokenPair;
import com.securityincidentmanager.domain.entity.User;
import com.securityincidentmanager.dto.request.LoginRequest;
import com.securityincidentmanager.dto.request.RegisterRequest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AuthController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_shouldReturn201_withAccessToken_andSetCookie() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("password123");
        request.setRole(User.Role.ANALYST);

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(new TokenPair("access-jwt", "raw-refresh"));
        when(jwtTokenService.getExpirationMs()).thenReturn(900000L);

        var result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-jwt"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        Cookie cookie = result.getResponse().getCookie("refresh_token");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo("raw-refresh");
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Test
    void register_shouldReturn400_whenRequestBodyInvalid() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("not-a-valid-email");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_shouldReturn200_withAccessToken_andSetCookie() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("alice@example.com");
        request.setPassword("password123");

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(new TokenPair("access-jwt", "raw-refresh"));
        when(jwtTokenService.getExpirationMs()).thenReturn(900000L);

        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-jwt"))
                .andReturn();

        Cookie cookie = result.getResponse().getCookie("refresh_token");
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    void refresh_shouldReturn200_andRotateCookie_whenCookiePresent() throws Exception {
        when(authService.refresh("old-raw"))
                .thenReturn(new TokenPair("new-access-jwt", "new-raw-refresh"));
        when(jwtTokenService.getExpirationMs()).thenReturn(900000L);

        var result = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", "old-raw")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-jwt"))
                .andReturn();

        Cookie cookie = result.getResponse().getCookie("refresh_token");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo("new-raw-refresh");
    }

    @Test
    void refresh_shouldReturn401_whenNoCookiePresent() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    void logout_shouldReturn204_andClearCookie() throws Exception {
        var result = mockMvc.perform(post("/api/auth/logout")
                        .cookie(new Cookie("refresh_token", "some-raw-token")))
                .andExpect(status().isNoContent())
                .andReturn();

        verify(authService).logout("some-raw-token");

        Cookie cookie = result.getResponse().getCookie("refresh_token");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getMaxAge()).isZero();
    }
}
