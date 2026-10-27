package com.hire.interview.config;

import com.hire.interview.security.JwtAuthenticationFilter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // =========================================================
    // 1. SECURITY FILTER CHAIN
    // =========================================================

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        return http
                .cors(cors ->
                        cors.configurationSource(corsConfigurationSource())
                )

                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // CORS preflight requests
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // Public authentication endpoints
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login"
                        ).permitAll()

                        // Public health information
                        .requestMatchers(
                                HttpMethod.GET,
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()

                        // Interviewer directory:
                        // candidates can choose an interviewer.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/interviewers"
                        ).hasAnyRole(
                                "CANDIDATE",
                                "INTERVIEWER"
                        )

                        // Candidate creates bookings.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/bookings"
                        ).hasRole("CANDIDATE")

                        // Candidate cancels their own bookings.
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/bookings/*/cancel"
                        ).hasRole("CANDIDATE")

                        // Candidate reschedules their own bookings.
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/bookings/*/reschedule"
                        ).hasRole("CANDIDATE")

                        // Only interviewers may complete interviews.
                        // The controller verifies assignment ownership.
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/bookings/*/complete"
                        ).hasRole("INTERVIEWER")

                        // Booking history endpoints.
                        // The controller checks participant ownership.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/bookings/participant/**"
                        ).hasAnyRole(
                                "CANDIDATE",
                                "INTERVIEWER"
                        )

                        // Only interviewers manage their availability.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/interviewers/*/availability"
                        ).hasRole("INTERVIEWER")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/interviewers/*/availability"
                        ).hasRole("INTERVIEWER")

                        // Candidates search for available interview slots.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/interviewers/*/available-slots"
                        ).hasRole("CANDIDATE")

                        // Other interviewer endpoints require authentication
                        // and one of the supported application roles.
                        .requestMatchers("/api/interviewers/**")
                        .hasAnyRole(
                                "CANDIDATE",
                                "INTERVIEWER"
                        )

                        // Any unmatched booking endpoint still requires
                        // authentication and a supported role.
                        .requestMatchers("/api/bookings/**")
                        .hasAnyRole(
                                "CANDIDATE",
                                "INTERVIEWER"
                        )

                        // All other routes require authentication.
                        .anyRequest().authenticated()
                )

                // Execute the JWT filter before username/password auth.
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .build();
    }

    // =========================================================
    // 2. CORS CONFIGURATION
    // =========================================================

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:5174",
                "http://localhost:5175",
                "http://localhost:5176"
        ));

        configuration.setAllowedMethods(List.of(
                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin"
        ));

        configuration.setExposedHeaders(List.of(
                "Authorization"
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    // =========================================================
    // 3. PREVENT DOUBLE REGISTRATION OF THE JWT FILTER
    // =========================================================

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter>
    jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter filter
    ) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);

        // The filter is managed by Spring Security above.
        // Disable its separate servlet-container registration.
        registration.setEnabled(false);

        return registration;
    }
}