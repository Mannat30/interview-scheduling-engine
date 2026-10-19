
package com.hire.interview.controller;

import com.hire.interview.dto.InterviewBookingRequest;
import com.hire.interview.dto.InterviewBookingResponse;
import com.hire.interview.dto.RescheduleBookingRequest;
import com.hire.interview.entity.InterviewBooking;
import com.hire.interview.service.InterviewBookingService;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class InterviewBookingController {

    private final InterviewBookingService bookingService;

    public InterviewBookingController(
            InterviewBookingService bookingService
    ) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<InterviewBookingResponse> scheduleInterview(
            @Valid @RequestBody InterviewBookingRequest request
    ) {
        InterviewBooking booking = bookingService.scheduleInterview(
                request.candidateId(),
                request.interviewerId(),
                request.startAt(),
                request.endAt()
        );

        InterviewBookingResponse response =
                InterviewBookingResponse.from(booking);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PatchMapping("/{bookingId}/cancel")
    public ResponseEntity<InterviewBookingResponse> cancelInterview(
            @PathVariable Long bookingId
    ) {
        InterviewBooking booking =
                bookingService.cancelInterview(bookingId);

        return ResponseEntity.ok(
                InterviewBookingResponse.from(booking)
        );
    }
    @PatchMapping("/{bookingId}/reschedule")
    public ResponseEntity<InterviewBookingResponse> rescheduleInterview(
            @PathVariable Long bookingId,
            @Valid @RequestBody RescheduleBookingRequest request
    ) {
        InterviewBooking booking = bookingService.rescheduleInterview(
                bookingId,
                request.startAt(),
                request.endAt()
        );

        return ResponseEntity.ok(
                InterviewBookingResponse.from(booking)
        );
    }@GetMapping("/participant/{participantId}")
    public Page<InterviewBookingResponse> getBookingsForParticipant(
            @PathVariable Long participantId,
            @PageableDefault(size = 10) Pageable pageable) {

        return bookingService.getBookingsForParticipant(
                participantId,
                pageable
        );
    }@GetMapping("/participant/{participantId}/upcoming")
    public Page<InterviewBookingResponse> getUpcomingBookings(
            @PathVariable Long participantId,
            @PageableDefault(size = 10) Pageable pageable) {

        return bookingService.getUpcomingBookings(
                participantId,
                pageable
        );
    }@GetMapping("/participant/{participantId}/completed")
    public Page<InterviewBookingResponse> getCompletedBookings(
            @PathVariable Long participantId,
            @PageableDefault(size = 10) Pageable pageable) {

        return bookingService.getCompletedBookings(
                participantId,
                pageable
        );
    }
    @PatchMapping("/{bookingId}/complete")
    public ResponseEntity<InterviewBookingResponse> completeInterview(
            @PathVariable Long bookingId) {

        InterviewBooking booking =
                bookingService.completeInterview(bookingId);

        return ResponseEntity.ok(
                InterviewBookingResponse.from(booking)
        );
    }

}
