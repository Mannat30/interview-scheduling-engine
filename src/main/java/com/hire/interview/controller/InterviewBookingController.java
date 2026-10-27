package com.hire.interview.controller;

import com.hire.interview.dto.InterviewBookingRequest;
import com.hire.interview.dto.InterviewBookingResponse;
import com.hire.interview.dto.RescheduleBookingRequest;
import com.hire.interview.entity.InterviewBooking;
import com.hire.interview.entity.User;
import com.hire.interview.entity.UserRole;
import com.hire.interview.exception.ResourceNotFoundException;
import com.hire.interview.repository.InterviewBookingRepository;
import com.hire.interview.repository.UserRepository;
import com.hire.interview.service.InterviewBookingService;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/bookings")
public class InterviewBookingController {

    private final InterviewBookingService bookingService;
    private final UserRepository userRepository;
    private final InterviewBookingRepository bookingRepository;

    public InterviewBookingController(
            InterviewBookingService bookingService,
            UserRepository userRepository,
            InterviewBookingRepository bookingRepository
    ) {
        this.bookingService = bookingService;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
    }

    // =========================================================
    // 1. CREATE A BOOKING
    // Only the authenticated candidate can create their booking.
    // =========================================================

    @PostMapping
    public ResponseEntity<InterviewBookingResponse> scheduleInterview(
            @Valid @RequestBody InterviewBookingRequest request,
            Authentication authentication
    ) {
        User currentUser = getAuthenticatedUser(authentication);

        requireRole(currentUser, UserRole.CANDIDATE);

        if (!Objects.equals(
                currentUser.getId(),
                request.candidateId()
        )) {
            throw new AccessDeniedException(
                    "You can only create bookings for your own account."
            );
        }

        InterviewBooking booking = bookingService.scheduleInterview(
                currentUser.getId(),
                request.interviewerId(),
                request.startAt(),
                request.endAt()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(InterviewBookingResponse.from(booking));
    }

    // =========================================================
    // 2. CANCEL A BOOKING
    // Only the candidate who owns the booking can cancel it.
    // =========================================================

    @PatchMapping("/{bookingId}/cancel")
    public ResponseEntity<InterviewBookingResponse> cancelInterview(
            @PathVariable Long bookingId,
            Authentication authentication
    ) {
        User currentUser = getAuthenticatedUser(authentication);

        requireRole(currentUser, UserRole.CANDIDATE);

        InterviewBooking existingBooking = getBooking(bookingId);

        if (!existingBooking.getCandidate().getId()
                .equals(currentUser.getId())) {
            throw new AccessDeniedException(
                    "You can only cancel your own bookings."
            );
        }

        InterviewBooking booking =
                bookingService.cancelInterview(bookingId);

        return ResponseEntity.ok(
                InterviewBookingResponse.from(booking)
        );
    }

    // =========================================================
    // 3. RESCHEDULE A BOOKING
    // Only the candidate who owns the booking can reschedule it.
    // =========================================================

    @PatchMapping("/{bookingId}/reschedule")
    public ResponseEntity<InterviewBookingResponse> rescheduleInterview(
            @PathVariable Long bookingId,
            @Valid @RequestBody RescheduleBookingRequest request,
            Authentication authentication
    ) {
        User currentUser = getAuthenticatedUser(authentication);

        requireRole(currentUser, UserRole.CANDIDATE);

        InterviewBooking existingBooking = getBooking(bookingId);

        if (!existingBooking.getCandidate().getId()
                .equals(currentUser.getId())) {
            throw new AccessDeniedException(
                    "You can only reschedule your own bookings."
            );
        }

        InterviewBooking booking =
                bookingService.rescheduleInterview(
                        bookingId,
                        request.startAt(),
                        request.endAt()
                );

        return ResponseEntity.ok(
                InterviewBookingResponse.from(booking)
        );
    }

    // =========================================================
    // 4. GET BOOKING HISTORY
    // Candidates and interviewers can only view their own records.
    // =========================================================

    @GetMapping("/participant/{participantId}")
    public Page<InterviewBookingResponse> getBookingsForParticipant(
            @PathVariable Long participantId,
            @PageableDefault(size = 10) Pageable pageable,
            Authentication authentication
    ) {
        User currentUser = getAuthenticatedUser(authentication);

        requireParticipant(currentUser, participantId);

        return bookingService.getBookingsForParticipant(
                currentUser.getId(),
                pageable
        );
    }

    // =========================================================
    // 5. GET UPCOMING BOOKINGS
    // =========================================================

    @GetMapping("/participant/{participantId}/upcoming")
    public Page<InterviewBookingResponse> getUpcomingBookings(
            @PathVariable Long participantId,
            @PageableDefault(size = 10) Pageable pageable,
            Authentication authentication
    ) {
        User currentUser = getAuthenticatedUser(authentication);

        requireParticipant(currentUser, participantId);

        return bookingService.getUpcomingBookings(
                currentUser.getId(),
                pageable
        );
    }

    // =========================================================
    // 6. GET COMPLETED BOOKINGS
    // =========================================================

    @GetMapping("/participant/{participantId}/completed")
    public Page<InterviewBookingResponse> getCompletedBookings(
            @PathVariable Long participantId,
            @PageableDefault(size = 10) Pageable pageable,
            Authentication authentication
    ) {
        User currentUser = getAuthenticatedUser(authentication);

        requireParticipant(currentUser, participantId);

        return bookingService.getCompletedBookings(
                currentUser.getId(),
                pageable
        );
    }

    // =========================================================
    // 7. COMPLETE A BOOKING
    // Only the assigned interviewer can complete the interview.
    // =========================================================

    @PatchMapping("/{bookingId}/complete")
    public ResponseEntity<InterviewBookingResponse> completeInterview(
            @PathVariable Long bookingId,
            Authentication authentication
    ) {
        User currentUser = getAuthenticatedUser(authentication);

        requireRole(currentUser, UserRole.INTERVIEWER);

        InterviewBooking existingBooking = getBooking(bookingId);

        if (!existingBooking.getInterviewer().getId()
                .equals(currentUser.getId())) {
            throw new AccessDeniedException(
                    "You can only complete interviews assigned to you."
            );
        }

        InterviewBooking booking =
                bookingService.completeInterview(bookingId);

        return ResponseEntity.ok(
                InterviewBookingResponse.from(booking)
        );
    }

    // =========================================================
    // PRIVATE SECURITY HELPERS
    // =========================================================

    private User getAuthenticatedUser(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            throw new AccessDeniedException(
                    "Authentication is required."
            );
        }

        /*
         * Assumption: the JWT authentication principal's name
         * is the user's email address, as used by login.
         */
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException(
                        "Authenticated user was not found."
                ));
    }

    private void requireRole(User user, UserRole expectedRole) {
        if (user.getRole() != expectedRole) {
            throw new AccessDeniedException(
                    "Your account does not have permission for this action."
            );
        }
    }

    private void requireParticipant(
            User currentUser,
            Long participantId
    ) {
        if (participantId == null
                || !currentUser.getId().equals(participantId)) {
            throw new AccessDeniedException(
                    "You can only view your own booking history."
            );
        }
    }

    private InterviewBooking getBooking(Long bookingId) {
        if (bookingId == null || bookingId <= 0) {
            throw new IllegalArgumentException(
                    "A valid booking ID is required."
            );
        }

        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found: " + bookingId
                ));
    }
}