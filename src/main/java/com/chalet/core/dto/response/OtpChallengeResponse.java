package com.chalet.core.dto.response;

public record OtpChallengeResponse(
        String maskedDestination,
        long expiresInSeconds,
        String devOtp
) {}
