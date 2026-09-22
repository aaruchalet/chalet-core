package com.chalet.core.dto.response;

public record AuthResponse(
        Long accountId,
        Long customerId,
        String name,
        String email,
        String phone,
        String location,
        String authProvider
) {}
