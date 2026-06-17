package com.securityincidentmanager.controller;

import com.securityincidentmanager.auth.AuthService;
import com.securityincidentmanager.auth.JwtTokenService;
import com.securityincidentmanager.auth.TokenPair;
import com.securityincidentmanager.dto.request.LoginRequest;
import com.securityincidentmanager.dto.request.RegisterRequest;
import com.securityincidentmanager.dto.response.AuthResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";
    private static final String BEARER = "Bearer";
    private static final int REFRESH_COOKIE_MAX_AGE = 7 * 24 * 60 * 60;

    private final AuthService authService;
    private final JwtTokenService jwtTokenService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {

        TokenPair pair = authService.register(request);
        setRefreshCookie(response, pair.refreshToken());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new AuthResponse(pair.accessToken(), BEARER, jwtTokenService.getExpirationMs()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {

        TokenPair pair = authService.login(request);
        setRefreshCookie(response, pair.refreshToken());
        return ResponseEntity.ok(
            new AuthResponse(pair.accessToken(), BEARER, jwtTokenService.getExpirationMs()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        String rawToken = extractRefreshCookie(request);
        if (rawToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            TokenPair pair = authService.refresh(rawToken);
            setRefreshCookie(response, pair.refreshToken());
            return ResponseEntity.ok(
                new AuthResponse(pair.accessToken(), BEARER, jwtTokenService.getExpirationMs()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            clearRefreshCookie(response);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        String rawToken = extractRefreshCookie(request);
        if (rawToken != null) {
            authService.logout(rawToken);
        }
        clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    // ── cookie helpers ────────────────────────────────────────────────────────

    private void setRefreshCookie(HttpServletResponse response, String rawToken) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, rawToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(REFRESH_COOKIE_MAX_AGE);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
            .filter(c -> REFRESH_COOKIE_NAME.equals(c.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);
    }
}
