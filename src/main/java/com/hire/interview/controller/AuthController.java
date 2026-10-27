package com.hire.interview.controller;

import com.hire.interview.dto.CandidateRegistrationRequest;
import com.hire.interview.dto.LoginRequest;
import com.hire.interview.dto.LoginResponse;
import com.hire.interview.entity.User;
import com.hire.interview.service.AuthenticationService;
import com.hire.interview.service.UserService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationService authenticationService;

    public AuthController(
            UserService userService,
            AuthenticationService authenticationService
    ) {
        this.userService = userService;
        this.authenticationService = authenticationService;
    }

    // =========================
    // REGISTER CANDIDATE / INTERVIEWER
    // POST /api/auth/register
    // =========================

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody CandidateRegistrationRequest request
    ) {

        // Role and invitation code are verified in UserService.
        User user = userService.register(request);

        // Return safe user details; never expose the password hash.
        Map<String, Object> response = new LinkedHashMap<>();

        response.put("id", user.getId());
        response.put("fullName", user.getFullName());
        response.put("email", user.getEmail());
        response.put("role", user.getRole().name());
        response.put("timeZone", user.getTimeZone());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =========================
    // LOGIN
    // POST /api/auth/login
    // =========================

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {

        LoginResponse response =
                authenticationService.login(request);

        return ResponseEntity.ok(response);
    }
}