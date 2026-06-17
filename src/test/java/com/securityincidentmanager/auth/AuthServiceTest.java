package com.securityincidentmanager.auth;

import com.securityincidentmanager.domain.entity.RefreshToken;
import com.securityincidentmanager.domain.entity.User;
import com.securityincidentmanager.domain.repository.UserRepository;
import com.securityincidentmanager.dto.request.LoginRequest;
import com.securityincidentmanager.dto.request.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenService jwtTokenService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_shouldReturnTokenPair_whenCredentialsAreNew() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("password123");
        request.setRole(User.Role.ANALYST);

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(jwtTokenService.generateToken(any(), any())).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn("raw-refresh");

        TokenPair result = authService.register(request);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("raw-refresh");
    }

    @Test
    void register_shouldThrow_whenEmailAlreadyTaken() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("taken@example.com");
        request.setUsername("alice");

        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("taken@example.com");
    }

    @Test
    void register_shouldThrow_whenUsernameAlreadyTaken() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setUsername("taken");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("taken");
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_shouldReturnTokenPair_whenCredentialsAreValid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("alice@example.com");
        request.setPassword("password123");

        User user = new User();
        user.setEmail("alice@example.com");
        user.setRole(User.Role.ANALYST);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(jwtTokenService.generateToken(any(), any())).thenReturn("access-token");
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn("raw-refresh");

        TokenPair result = authService.login(request);

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("raw-refresh");
    }

    // ── refresh ───────────────────────────────────────────────────────────────

    @Test
    void refresh_shouldReturnNewTokenPair_whenTokenIsValid() {
        User user = new User();
        user.setEmail("alice@example.com");
        user.setRole(User.Role.ANALYST);

        RefreshToken token = new RefreshToken();
        token.setRevoked(false);
        token.setExpiresAt(LocalDateTime.now().plusDays(1));
        token.setUser(user);

        when(refreshTokenService.hash("raw")).thenReturn("hashed");
        when(refreshTokenService.findByHash("hashed")).thenReturn(Optional.of(token));
        when(refreshTokenService.rotate(token)).thenReturn("new-raw-refresh");
        when(jwtTokenService.generateToken(any(), any())).thenReturn("new-access-token");

        TokenPair result = authService.refresh("raw");

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-raw-refresh");
    }

    @Test
    void refresh_shouldRevokeAllSessions_andThrow_whenTokenIsRevoked() {
        User user = new User();

        RefreshToken token = new RefreshToken();
        token.setRevoked(true);
        token.setExpiresAt(LocalDateTime.now().plusDays(1));
        token.setUser(user);

        when(refreshTokenService.hash("stolen")).thenReturn("stolen-hash");
        when(refreshTokenService.findByHash("stolen-hash")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.refresh("stolen"))
                .isInstanceOf(IllegalStateException.class);

        verify(refreshTokenService).revokeAllForUser(user);
    }

    @Test
    void refresh_shouldThrow_whenTokenIsExpired() {
        User user = new User();

        RefreshToken token = new RefreshToken();
        token.setRevoked(false);
        token.setExpiresAt(LocalDateTime.now().minusDays(1));
        token.setUser(user);

        when(refreshTokenService.hash("old")).thenReturn("old-hash");
        when(refreshTokenService.findByHash("old-hash")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> authService.refresh("old"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refresh_shouldThrow_whenTokenNotFound() {
        when(refreshTokenService.hash("missing")).thenReturn("missing-hash");
        when(refreshTokenService.findByHash("missing-hash")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("missing"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── logout ────────────────────────────────────────────────────────────────

    @Test
    void logout_shouldDelegateRevoke() {
        authService.logout("raw-token");

        verify(refreshTokenService).revoke("raw-token");
    }
}
