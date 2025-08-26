package com.api.flashlearn.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotNull(message = "Token is required")
    @NotBlank(message = "Token is required")
    private String token;
    @NotBlank(message = "Password is required")
    @Size(min = 1, max = 25, message = "Password must be between 1 and 25 characters")
    private String newPassword;
    @NotNull(message = "Confirm password is required")
    @NotBlank(message = "Password is required")
    private String confirmPassword;
}
