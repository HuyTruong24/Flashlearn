package com.api.flashlearn.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.api.flashlearn.dtos.PasswordResetTokenDto;
import com.api.flashlearn.entities.PasswordResetToken;

@Mapper(componentModel = "spring")
public interface PasswordResetTokenMapper {
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "token.expiryDate", target = "expiryDate")
    PasswordResetTokenDto toDto(PasswordResetToken token);

    @Mapping(source = "userId", target = "user.id")
    @Mapping(target = "expiryDate", expression = "java(PasswordResetToken.calculateExpiryDate())")
    PasswordResetToken toEntity(String token, Long userId);
}
