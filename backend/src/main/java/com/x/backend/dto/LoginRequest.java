package com.x.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

        @NotBlank(message = "Username cannot be blank")
        String username,

        @NotBlank(message = "Password cannot be blank")
        @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
        String password
) {
}
