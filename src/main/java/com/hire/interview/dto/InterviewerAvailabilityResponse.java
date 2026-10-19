
package com.hire.interview.dto;

import com.hire.interview.entity.InterviewerAvailability;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;

public record InterviewerAvailabilityResponse(
        Long id,
        Long interviewerId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        boolean enabled,
        Instant createdAt
) {
    public static InterviewerAvailabilityResponse from(
            InterviewerAvailability availability
    ) {
        return new InterviewerAvailabilityResponse(
                availability.getId(),
                availability.getInterviewer().getId(),
                availability.getDayOfWeek(),
                availability.getStartTime(),
                availability.getEndTime(),
                availability.isEnabled(),
                availability.getCreatedAt()
        );
    }
}
