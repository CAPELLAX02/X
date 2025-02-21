package com.x.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CompleteEmailVerificationRequest(

        @NotBlank(message = "Username cannot be blank")
        String username,

        @NotBlank(message = "Verification code cannot be blank")
        String verificationCode

) {
}
