package com.hire.interview.dto;

import com.hire.interview.entity.User;
import com.hire.interview.entity.UserRole;

import java.time.Instant;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        UserRole role,
        String timeZone,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getTimeZone(),
                user.getCreatedAt()
        );
    }
}