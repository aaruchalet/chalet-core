package com.chalet.core.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OtpRequest(
        @NotBlank(message = "Email or phone is required")
        String identifier
) {}
