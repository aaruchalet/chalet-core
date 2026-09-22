package com.chalet.core.dto.request;

import jakarta.validation.constraints.NotBlank;

public record SignInRequest(
        @NotBlank(message = "Email or phone is required")
        String identifier,

        @NotBlank(message = "Password is required")
        String password
) {}
