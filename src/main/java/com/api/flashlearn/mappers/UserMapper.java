package com.api.flashlearn.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.api.flashlearn.dtos.RegisterUserRequest;
import com.api.flashlearn.dtos.UpdateUserRequest;
import com.api.flashlearn.dtos.UserDto;
import com.api.flashlearn.entities.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    UserDto toDto(User user);

    User toEntity(RegisterUserRequest userRequest);

    void updateUserRequest(UpdateUserRequest request, @MappingTarget User user);
}
