package com.api.flashlearn.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;


@AllArgsConstructor
@Data
public class RegisterUserRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 1, max = 20, message = "Name must be between 1 and 20 characters")
    private String username;
    @NotBlank(message = "Retyping username is required")
    private String retypeUsername;
}
