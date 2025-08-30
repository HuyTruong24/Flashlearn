package com.api.flashlearn.dtos;

import lombok.Data;

@Data
public class VerifyUserRequest {
    private String email;
    private String verificationCode;
}
