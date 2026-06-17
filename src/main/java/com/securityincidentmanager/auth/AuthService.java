package com.securityincidentmanager.auth;

import com.securityincidentmanager.domain.entity.RefreshToken;
import com.securityincidentmanager.domain.entity.User;
import com.securityincidentmanager.domain.repository.UserRepository;
import com.securityincidentmanager.dto.request.LoginRequest;
import com.securityincidentmanager.dto.request.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String EMAIL_TAKEN = "Email already registered: ";
    private static final String USERNAME_TAKEN = "Username already taken: ";
    private static final String USER_NOT_FOUND = "User not found: ";
    private static final String TOKEN_NOT_FOUND = "Refresh token not found";
    private static final String TOKEN_EXPIRED = "Refresh token has expired";
    private static final String THEFT_DETECTED = "Refresh token reuse detected — all sessions revoked";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public TokenPair register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(EMAIL_TAKEN + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException(USERNAME_TAKEN + request.getUsername());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole() : User.Role.ANALYST);
        userRepository.save(user);

        String accessToken = jwtTokenService.generateToken(user.getEmail(), user.getRole().name());
        String rawRefreshToken = refreshTokenService.createRefreshToken(user);
        return new TokenPair(accessToken, rawRefreshToken);
    }

    @Transactional
    public TokenPair login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND + request.getEmail()));

        String accessToken = jwtTokenService.generateToken(user.getEmail(), user.getRole().name());
        String rawRefreshToken = refreshTokenService.createRefreshToken(user);
        return new TokenPair(accessToken, rawRefreshToken);
    }

    public TokenPair refresh(String rawToken) {
        String tokenHash = refreshTokenService.hash(rawToken);

        Optional<RefreshToken> found = refreshTokenService.findByHash(tokenHash);
        if (found.isEmpty()) {
            throw new IllegalArgumentException(TOKEN_NOT_FOUND);
        }

        RefreshToken token = found.get();

        if (token.isRevoked()) {
            refreshTokenService.revokeAllForUser(token.getUser());
            throw new IllegalStateException(THEFT_DETECTED);
        }

        if (token.isExpired()) {
            throw new IllegalStateException(TOKEN_EXPIRED);
        }

        String newRawRefreshToken = refreshTokenService.rotate(token);
        String newAccessToken = jwtTokenService.generateToken(
            token.getUser().getEmail(),
            token.getUser().getRole().name()
        );
        return new TokenPair(newAccessToken, newRawRefreshToken);
    }

    public void logout(String rawToken) {
        refreshTokenService.revoke(rawToken);
    }
}
