
package com.hire.interview.service;

import com.hire.interview.entity.User;
import com.hire.interview.entity.UserRole;
import com.hire.interview.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Locale;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerCandidate(
            String fullName,
            String email,
            String rawPassword,
            String timeZone
    ) {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }

        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new IllegalArgumentException("Valid email is required");
        }

        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters"
            );
        }

        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException(
                    "Email is already registered"
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
                    "Invalid time-zone identifier"
            );
        }

        String passwordHash = passwordEncoder.encode(rawPassword);

        User candidate = new User(
                fullName.trim(),
                normalizedEmail,
                passwordHash,
                UserRole.CANDIDATE,
                normalizedTimeZone
        );

        return userRepository.save(candidate);
    }
}
