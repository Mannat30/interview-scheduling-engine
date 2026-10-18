package com.hire.interview.controller;

import com.hire.interview.dto.CandidateRegistrationRequest;
import com.hire.interview.dto.UserResponse;
import com.hire.interview.entity.User;
import com.hire.interview.service.UserService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid @RequestBody CandidateRegistrationRequest request
    ) {
        User candidate = userService.registerCandidate(
                request.fullName(),
                request.email(),
                request.password(),
                request.timeZone()
        );

        UserResponse response = UserResponse.from(candidate);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}