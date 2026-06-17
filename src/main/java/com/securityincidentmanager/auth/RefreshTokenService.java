package com.securityincidentmanager.auth;

import com.securityincidentmanager.domain.entity.RefreshToken;
import com.securityincidentmanager.domain.entity.User;
import com.securityincidentmanager.domain.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final String SHA_ALGORITHM = "SHA-256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Transactional
    public String createRefreshToken(User user) {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenHash(hash(rawToken));
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plus(refreshExpirationMs, ChronoUnit.MILLIS));
        refreshToken.setRevoked(false);

        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByHash(String tokenHash) {
        return refreshTokenRepository.findByTokenHash(tokenHash);
    }

    @Transactional
    public String rotate(RefreshToken oldToken) {
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);
        return createRefreshToken(oldToken.getUser());
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.deleteAllByUser(user);
    }

    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_ALGORITHM);
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available on this JVM", e);
        }
    }
}
