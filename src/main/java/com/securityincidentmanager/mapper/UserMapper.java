package com.securityincidentmanager.mapper;

import com.securityincidentmanager.domain.entity.User;
import com.securityincidentmanager.dto.response.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toUserResponse(User user);
}

