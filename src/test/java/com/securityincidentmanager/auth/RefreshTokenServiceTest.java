package com.securityincidentmanager.auth;

import com.securityincidentmanager.domain.entity.RefreshToken;
import com.securityincidentmanager.domain.entity.User;
import com.securityincidentmanager.domain.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", 604800000L);
    }

    // ── createRefreshToken ────────────────────────────────────────────────────

    @Test
    void createRefreshToken_shouldSaveHashedToken_andReturnRawToken() {
        User user = new User();
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        String raw = refreshTokenService.createRefreshToken(user);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());

        assertThat(raw).isNotBlank();
        assertThat(captor.getValue().getTokenHash()).isNotEqualTo(raw);
        assertThat(captor.getValue().getTokenHash()).isEqualTo(refreshTokenService.hash(raw));
    }

    // ── findByHash ────────────────────────────────────────────────────────────

    @Test
    void findByHash_shouldReturnToken_whenExists() {
        RefreshToken token = new RefreshToken();
        when(refreshTokenRepository.findByTokenHash("somehash")).thenReturn(Optional.of(token));

        assertThat(refreshTokenService.findByHash("somehash")).isPresent();
    }

    @Test
    void findByHash_shouldReturnEmpty_whenNotExists() {
        when(refreshTokenRepository.findByTokenHash("missing")).thenReturn(Optional.empty());

        assertThat(refreshTokenService.findByHash("missing")).isEmpty();
    }

    // ── rotate ────────────────────────────────────────────────────────────────

    @Test
    void rotate_shouldRevokeOldToken_andCreateNewOne() {
        User user = new User();
        RefreshToken old = new RefreshToken();
        old.setUser(user);
        old.setRevoked(false);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        refreshTokenService.rotate(old);

        assertThat(old.isRevoked()).isTrue();
    }

    // ── revoke ────────────────────────────────────────────────────────────────

    @Test
    void revoke_shouldMarkTokenAsRevoked_whenFound() {
        RefreshToken token = new RefreshToken();
        token.setRevoked(false);
        String raw = "someRawToken";
        when(refreshTokenRepository.findByTokenHash(refreshTokenService.hash(raw)))
                .thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        refreshTokenService.revoke(raw);

        assertThat(token.isRevoked()).isTrue();
    }

    @Test
    void revoke_shouldDoNothing_whenTokenNotFound() {
        String raw = "nonexistent";
        when(refreshTokenRepository.findByTokenHash(refreshTokenService.hash(raw)))
                .thenReturn(Optional.empty());

        refreshTokenService.revoke(raw);

        verify(refreshTokenRepository).findByTokenHash(any());
    }

    // ── revokeAllForUser ──────────────────────────────────────────────────────

    @Test
    void revokeAllForUser_shouldCallDeleteAllByUser() {
        User user = new User();

        refreshTokenService.revokeAllForUser(user);

        verify(refreshTokenRepository).deleteAllByUser(user);
    }

    // ── hash ──────────────────────────────────────────────────────────────────

    @Test
    void hash_shouldProduceSixtyFourCharHexString() {
        String result = refreshTokenService.hash("any-input");

        assertThat(result).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    void hash_shouldBeDeterministic() {
        assertThat(refreshTokenService.hash("abc")).isEqualTo(refreshTokenService.hash("abc"));
    }

    // ── isExpired helper ──────────────────────────────────────────────────────

    @Test
    void refreshToken_isExpired_shouldReturnTrue_whenPastExpiry() {
        RefreshToken token = new RefreshToken();
        token.setExpiresAt(LocalDateTime.now().minusSeconds(1));

        assertThat(token.isExpired()).isTrue();
    }

    @Test
    void refreshToken_isExpired_shouldReturnFalse_whenBeforeExpiry() {
        RefreshToken token = new RefreshToken();
        token.setExpiresAt(LocalDateTime.now().plusDays(7));

        assertThat(token.isExpired()).isFalse();
    }
}
