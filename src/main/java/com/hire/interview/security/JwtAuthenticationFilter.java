package com.hire.interview.security;

import com.hire.interview.entity.User;
import com.hire.interview.repository.UserRepository;
import com.hire.interview.service.JwtService;

import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Step 1: Check whether Authorization header exists.
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("JWT DEBUG: Missing or invalid Bearer header.");

            filterChain.doFilter(request, response);
            return;
        }

        // Step 2: Extract the token.
        String token = authHeader.substring(7).trim();

        if (token.isEmpty()) {
            System.out.println("JWT DEBUG: Token is empty.");

            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Step 3: Verify token signature and extract email.
            String email = jwtService.extractEmail(token);

            System.out.println("JWT DEBUG: Token parsed successfully.");

            // Step 4: Find the user in the database.
            User user = userRepository.findByEmail(email)
                    .orElse(null);

            if (user == null) {
                System.out.println("JWT DEBUG: User not found.");

                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            // Step 5: Validate the token against the user.
            if (!jwtService.isTokenValid(token, user)) {
                System.out.println("JWT DEBUG: Token validation failed.");

                SecurityContextHolder.clearContext();
                filterChain.doFilter(request, response);
                return;
            }

            // Step 6: Create Spring Security authentication.
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user.getEmail(),
                            null,
                            List.of(
                                    new SimpleGrantedAuthority(
                                            "ROLE_" + user.getRole().name()
                                    )
                            )
                    );

            // Step 7: Mark this request as authenticated.
            SecurityContextHolder.getContext()
                    .setAuthentication(authentication);

            System.out.println(
                    "JWT DEBUG: Authentication successful. Role = "
                            + user.getRole().name()
            );

        } catch (JwtException | IllegalArgumentException ex) {

            // Never print the actual token or password.
            System.out.println(
                    "JWT DEBUG: Token rejected. Reason = "
                            + ex.getClass().getSimpleName()
            );

            SecurityContextHolder.clearContext();
        }

        // Continue to the controller/security authorization rules.
        filterChain.doFilter(request, response);
    }
}