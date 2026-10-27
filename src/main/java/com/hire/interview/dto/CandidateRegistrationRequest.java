package com.hire.interview.dto;

import com.hire.interview.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CandidateRegistrationRequest(

        @NotBlank(message = "Full name is required")
        @Size(min = 2, max = 100)
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72)
        String password,

        @NotBlank(message = "Time zone is required")
        String timeZone,

        // Optional for compatibility with existing registration requests.
        UserRole role,

        // Required by the service only when registering as an interviewer.
        String interviewerInviteCode

) {
}