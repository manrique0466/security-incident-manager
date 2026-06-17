package com.securityincidentmanager.domain.repository;

import com.securityincidentmanager.domain.entity.RefreshToken;
import com.securityincidentmanager.domain.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RefreshTokenRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("incident_manager_test")
            .withUsername("appuser")
            .withPassword("apppassword");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setUsername("tokenuser");
        testUser.setEmail("tokenuser@example.com");
        testUser.setPassword("hashedpassword");
        testUser.setRole(User.Role.ANALYST);
        userRepository.save(testUser);
    }

    private RefreshToken buildToken(User owner, String hash) {
        RefreshToken token = new RefreshToken();
        token.setTokenHash(hash);
        token.setUser(owner);
        token.setExpiresAt(LocalDateTime.now().plusDays(7));
        token.setRevoked(false);
        return token;
    }

    @Test
    void shouldSaveAndFindByTokenHash() {
        refreshTokenRepository.save(buildToken(testUser, "abc123hash"));

        assertThat(refreshTokenRepository.findByTokenHash("abc123hash")).isPresent();
    }

    @Test
    void shouldReturnEmpty_whenTokenHashNotFound() {
        assertThat(refreshTokenRepository.findByTokenHash("nonexistent")).isEmpty();
    }

    @Test
    void deleteAllByUser_shouldRemoveAllTokensForUser_andLeaveOthers() {
        User otherUser = new User();
        otherUser.setUsername("other");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword("hashedpassword");
        otherUser.setRole(User.Role.ANALYST);
        userRepository.save(otherUser);

        refreshTokenRepository.save(buildToken(testUser, "hash-a"));
        refreshTokenRepository.save(buildToken(testUser, "hash-b"));
        refreshTokenRepository.save(buildToken(otherUser, "hash-c"));

        refreshTokenRepository.deleteAllByUser(testUser);

        assertThat(refreshTokenRepository.findByTokenHash("hash-a")).isEmpty();
        assertThat(refreshTokenRepository.findByTokenHash("hash-b")).isEmpty();
        assertThat(refreshTokenRepository.findByTokenHash("hash-c")).isPresent();
    }
}
