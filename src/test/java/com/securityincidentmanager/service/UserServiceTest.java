package com.securityincidentmanager.service;

import com.securityincidentmanager.domain.entity.User;
import com.securityincidentmanager.domain.repository.UserRepository;
import com.securityincidentmanager.dto.response.UserResponse;
import com.securityincidentmanager.exception.ResourceNotFoundException;
import com.securityincidentmanager.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    void getById_shouldReturnResponse_whenUserExists() {
        UUID id = UUID.randomUUID();
        User user = new User();
        UserResponse expectedResponse = new UserResponse();

        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(expectedResponse);

        UserResponse result = userService.getById(id);

        assertThat(result).isEqualTo(expectedResponse);
    }

    @Test
    void getById_shouldThrowException_whenUserNotFound() {
        UUID id = UUID.randomUUID();

        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getAll ────────────────────────────────────────────────────────────────

    @Test
    void getAll_shouldReturnMappedList() {
        User user = new User();
        UserResponse response = new UserResponse();

        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(response);

        List<UserResponse> result = userService.getAll();

        assertThat(result).hasSize(1).containsExactly(response);
    }

    // ── getByEmail ────────────────────────────────────────────────────────────

    @Test
    void getByEmail_shouldReturnResponse_whenUserExists() {
        String email = "user@example.com";
        User user = new User();
        UserResponse expectedResponse = new UserResponse();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(expectedResponse);

        UserResponse result = userService.getByEmail(email);

        assertThat(result).isEqualTo(expectedResponse);
    }

    @Test
    void getByEmail_shouldThrowException_whenUserNotFound() {
        String email = "missing@example.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByEmail(email))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}