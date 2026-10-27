
package com.hire.interview.service;

import com.hire.interview.dto.CandidateRegistrationRequest;
import com.hire.interview.entity.User;
import com.hire.interview.entity.UserRole;
import com.hire.interview.exception.DuplicateEmailException;
import com.hire.interview.repository.UserRepository;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.registration.interviewer-invite-code:}")
    private String interviewerInviteCode;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    @PostConstruct
    public void checkInviteCodeConfiguration() {
        boolean configured =
                interviewerInviteCode != null
                        && !interviewerInviteCode.isBlank();

        System.out.println(
                "Interviewer invite code configured: " + configured
        );
    }
    /**
     * Registration method used by the registration API.
     * Role is determined and checked on the backend.
     */
    @Transactional
    public User register(CandidateRegistrationRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Registration details are required."
            );
        }

        UserRole requestedRole = resolveRequestedRole(
                request.role(),
                request.interviewerInviteCode()
        );

        return createUser(
                request.fullName(),
                request.email(),
                request.password(),
                request.timeZone(),
                requestedRole
        );
    }

    /**
     * Backward-compatible candidate-only registration method.
     * Existing code calling this method still creates candidates.
     */
    @Transactional
    public User registerCandidate(
            String fullName,
            String email,
            String rawPassword,
            String timeZone
    ) {
        return createUser(
                fullName,
                email,
                rawPassword,
                timeZone,
                UserRole.CANDIDATE
        );
    }

    private User createUser(
            String fullName,
            String email,
            String rawPassword,
            String timeZone,
            UserRole role
    ) {

        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException(
                    "Name is required."
            );
        }

        if (email == null
                || email.isBlank()
                || !email.contains("@")) {
            throw new IllegalArgumentException(
                    "Valid email is required."
            );
        }

        if (rawPassword == null
                || rawPassword.length() < 8
                || rawPassword.length() > 72) {
            throw new IllegalArgumentException(
                    "Password must contain between 8 and 72 characters."
            );
        }

        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException(
                    "Email is already registered."
            );
        }

        String normalizedTimeZone =
                (timeZone == null || timeZone.isBlank())
                        ? "Asia/Kolkata"
                        : timeZone.trim();

        try {
            ZoneId.of(normalizedTimeZone);
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException(
                    "Invalid time-zone identifier."
            );
        }

        String passwordHash =
                passwordEncoder.encode(rawPassword);

        User user = new User(
                fullName.trim(),
                normalizedEmail,
                passwordHash,
                role,
                normalizedTimeZone
        );

        return userRepository.save(user);
    }

    /**
     * Candidates can register normally.
     * Interviewer registration requires the configured invite code.
     * Recruiters cannot register through the public form.
     */
    private UserRole resolveRequestedRole(
            UserRole requestedRole,
            String suppliedCode
    ) {

        if (requestedRole == null) {
            requestedRole = UserRole.CANDIDATE;
        }

        if (requestedRole == UserRole.RECRUITER) {
            throw new IllegalArgumentException(
                    "Recruiter accounts must be created by an administrator."
            );
        }

        if (requestedRole == UserRole.INTERVIEWER) {

            if (interviewerInviteCode == null
                    || interviewerInviteCode.isBlank()
                    || suppliedCode == null
                    || suppliedCode.isBlank()
                    || !MessageDigest.isEqual(
                    interviewerInviteCode.getBytes(
                            StandardCharsets.UTF_8
                    ),
                    suppliedCode.getBytes(
                            StandardCharsets.UTF_8
                    )
            )) {

                throw new IllegalArgumentException(
                        "A valid interviewer invitation code is required."
                );
            }
        }

        return requestedRole;
    }
}
