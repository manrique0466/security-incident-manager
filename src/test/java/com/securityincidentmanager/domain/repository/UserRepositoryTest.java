package com.securityincidentmanager.domain.repository;

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

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

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
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("hashedpassword");
        testUser.setRole(User.Role.ANALYST);
    }

    @Test
    void shouldSaveAndFindUserById() {
        User saved = userRepository.save(testUser);

        assertThat(saved.getId()).isNotNull();
        assertThat(userRepository.findById(saved.getId())).isPresent();
    }

    @Test
    void shouldFindUserByEmail() {
        userRepository.save(testUser);

        assertThat(userRepository.findByEmail("test@example.com")).isPresent();
    }

    @Test
    void shouldFindUserByUsername() {
        userRepository.save(testUser);

        assertThat(userRepository.findByUsername("testuser")).isPresent();
    }

    @Test
    void shouldReturnTrueWhenEmailExists() {
        userRepository.save(testUser);

        assertThat(userRepository.existsByEmail("test@example.com")).isTrue();
    }

    @Test
    void shouldReturnTrueWhenUsernameExists() {
        userRepository.save(testUser);

        assertThat(userRepository.existsByUsername("testuser")).isTrue();
    }

    @Test
    void shouldReturnFalseWhenEmailDoesNotExist() {
        assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
    }
}
