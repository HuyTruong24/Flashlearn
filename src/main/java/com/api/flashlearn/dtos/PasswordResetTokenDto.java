package com.api.flashlearn.dtos;

import lombok.Data;

@Data
public class PasswordResetTokenDto {
    private Long id;
    private String token;
    private Long userId;
    private String expiryDate;
}
