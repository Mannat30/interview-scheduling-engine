package com.hire.interview.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInMs,
        String email,
        String role,
        Long userId
) {
}