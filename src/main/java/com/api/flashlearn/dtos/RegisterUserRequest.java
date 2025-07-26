package com.api.flashlearn.dtos;

import com.api.flashlearn.validation.Lowercase;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;


@AllArgsConstructor
@Data
public class RegisterUserRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Lowercase(message = "Email must be lowercase")
    private String email;
    
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 20, message = "Name must be between 1 and 255 characters")
    private String name;

    @NotBlank(message = "Password is required")
    @Size(min = 1, max = 25, message = "Password must be between 1 and 25 characters")
    private String password;
}
