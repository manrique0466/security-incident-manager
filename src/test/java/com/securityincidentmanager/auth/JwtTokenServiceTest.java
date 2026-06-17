package com.securityincidentmanager.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenServiceTest {

    private JwtTokenService jwtTokenService;

    @BeforeEach
    void setUp() {
        jwtTokenService = new JwtTokenService(
                "incident-manager-secret-key-minimum-32-chars!!",
                84600000L
        );
    }

    // ── generateToken ─────────────────────────────────────────────────────────

    @Test
    void generateToken_shouldReturnNonEmptyToken() {
        String token = jwtTokenService.generateToken("user@example.com", "ANALYST");

        assertThat(token).isNotNull().isNotEmpty();
    }

    // ── isTokenValid ──────────────────────────────────────────────────────────

    @Test
    void isTokenValid_shouldReturnTrue_forValidToken() {
        String token = jwtTokenService.generateToken("user@example.com", "ANALYST");

        assertThat(jwtTokenService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_shouldReturnFalse_forGarbageToken() {
        assertThat(jwtTokenService.isTokenValid("not.a.valid.token")).isFalse();
    }

    // ── extractEmail ──────────────────────────────────────────────────────────

    @Test
    void extractEmail_shouldReturnCorrectEmail() {
        String token = jwtTokenService.generateToken("user@example.com", "ANALYST");

        assertThat(jwtTokenService.extractEmail(token)).isEqualTo("user@example.com");
    }

    // ── extractRole ───────────────────────────────────────────────────────────

    @Test
    void extractRole_shouldReturnCorrectRole() {
        String token = jwtTokenService.generateToken("user@example.com", "ADMIN");

        assertThat(jwtTokenService.extractRole(token)).isEqualTo("ADMIN");
    }

    // ── getExpirationMs ───────────────────────────────────────────────────────

    @Test
    void getExpirationMs_shouldReturnConfiguredValue() {
        assertThat(jwtTokenService.getExpirationMs()).isEqualTo(84600000L);
    }
}
