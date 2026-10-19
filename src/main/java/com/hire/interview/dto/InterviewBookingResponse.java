
package com.hire.interview.dto;

import com.hire.interview.entity.InterviewBooking;
import com.hire.interview.entity.InterviewStatus;

import java.time.Instant;

public record InterviewBookingResponse(
        Long id,
        Long candidateId,
        Long interviewerId,
        Instant startAt,
        Instant endAt,
        InterviewStatus status,
        Instant createdAt
) {
    public static InterviewBookingResponse from(InterviewBooking booking) {
        return new InterviewBookingResponse(
                booking.getId(),
                booking.getCandidate().getId(),
                booking.getInterviewer().getId(),
                booking.getStartAt(),
                booking.getEndAt(),
                booking.getStatus(),
                booking.getCreatedAt()
        );
    }
}
