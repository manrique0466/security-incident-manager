package com.securityincidentmanager.mapper;

import com.securityincidentmanager.domain.entity.User;
import com.securityincidentmanager.dto.response.UserResponse;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Test
    void shouldMapUserToUserResponse() {
        User user = new User();
        user.setUsername("camilo");
        user.setEmail("camilo@example.com");
        user.setPassword("secret");
        user.setRole(User.Role.ADMIN);

        UserResponse response = userMapper.toUserResponse(user);

        assertThat(response.getUsername()).isEqualTo("camilo");
        assertThat(response.getEmail()).isEqualTo("camilo@example.com");
        assertThat(response.getRole()).isEqualTo(User.Role.ADMIN);
    }

    @Test
    void shouldMapAnalystRole() {
        User user = new User();
        user.setRole(User.Role.ANALYST);

        assertThat(userMapper.toUserResponse(user).getRole()).isEqualTo(User.Role.ANALYST);
    }

    @Test
    void shouldMapNullIdAndTimestampsWhenNotSet() {
        User user = new User();
        user.setUsername("test");
        user.setEmail("test@example.com");
        user.setPassword("secret");
        user.setRole(User.Role.ANALYST);

        UserResponse response = userMapper.toUserResponse(user);

        assertThat(response.getId()).isNull();
        assertThat(response.getCreatedAt()).isNull();
    }

}
