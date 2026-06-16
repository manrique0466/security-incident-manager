package com.securityincidentmanager.service;

import com.securityincidentmanager.domain.entity.User;
import com.securityincidentmanager.domain.repository.UserRepository;
import com.securityincidentmanager.dto.response.UserResponse;
import com.securityincidentmanager.exception.ResourceNotFoundException;
import com.securityincidentmanager.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String USER_NOT_FOUND = "User not found: ";

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserResponse getById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + id));
        return userMapper.toUserResponse(user);
    }

    public List<UserResponse> getAll() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    public UserResponse getByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(USER_NOT_FOUND + email));
        return userMapper.toUserResponse(user);
    }
}
