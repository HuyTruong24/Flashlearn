package com.api.flashlearn.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.api.flashlearn.dtos.UserDto;
import com.api.flashlearn.entities.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    UserDto toDto(User user);
}
