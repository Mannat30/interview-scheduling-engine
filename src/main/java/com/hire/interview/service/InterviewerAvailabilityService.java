
package com.hire.interview.service;

import com.hire.interview.entity.InterviewerAvailability;
import com.hire.interview.entity.User;
import com.hire.interview.entity.UserRole;
import com.hire.interview.exception.BookingConflictException;
import com.hire.interview.exception.ResourceNotFoundException;
import com.hire.interview.repository.InterviewerAvailabilityRepository;
import com.hire.interview.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Service
public class InterviewerAvailabilityService {

    private final UserRepository userRepository;
    private final InterviewerAvailabilityRepository availabilityRepository;

    public InterviewerAvailabilityService(
            UserRepository userRepository,
            InterviewerAvailabilityRepository availabilityRepository
    ) {
        this.userRepository = userRepository;
        this.availabilityRepository = availabilityRepository;
    }

    @Transactional
    public InterviewerAvailability addAvailability(
            Long interviewerId,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {
        // 1. Validate required inputs.
        if (interviewerId == null || interviewerId <= 0) {
            throw new IllegalArgumentException(
                    "A valid interviewer ID is required"
            );
        }

        if (dayOfWeek == null || startTime == null || endTime == null) {
            throw new IllegalArgumentException(
                    "Day, start time and end time are required"
            );
        }

        // 2. Start time must be before end time.
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException(
                    "Start time must be before end time"
            );
        }

        // 3. Retrieve the interviewer.
        User interviewer = userRepository.findById(interviewerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Interviewer not found: " + interviewerId
                ));

        // 4. Only users with the INTERVIEWER role can set availability.
        if (interviewer.getRole() != UserRole.INTERVIEWER) {
            throw new IllegalArgumentException(
                    "Only users with the INTERVIEWER role can set availability"
            );
        }

        // 5. Retrieve enabled windows for this interviewer and weekday.
        List<InterviewerAvailability> existingWindows =
                availabilityRepository
                        .findByInterviewer_IdAndDayOfWeekAndEnabledTrue(
                                interviewerId,
                                dayOfWeek
                        );

        // 6. Reject any overlapping window.
        boolean overlaps = existingWindows.stream().anyMatch(existing ->
                startTime.isBefore(existing.getEndTime())
                        && endTime.isAfter(existing.getStartTime())
        );

        if (overlaps) {
            throw new BookingConflictException(
                    "Availability overlaps an existing enabled window"
            );
        }

        // 7. Create and save the availability window.
        InterviewerAvailability availability =
                new InterviewerAvailability(
                        interviewer,
                        dayOfWeek,
                        startTime,
                        endTime
                );

        return availabilityRepository.save(availability);
    }

    @Transactional(readOnly = true)
    public List<InterviewerAvailability> getAvailability(
            Long interviewerId,
            DayOfWeek dayOfWeek
    ) {
        if (interviewerId == null || interviewerId <= 0) {
            throw new IllegalArgumentException(
                    "A valid interviewer ID is required"
            );
        }

        if (dayOfWeek == null) {
            throw new IllegalArgumentException(
                    "Day of week is required"
            );
        }

        User interviewer = userRepository.findById(interviewerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Interviewer not found: " + interviewerId
                ));

        if (interviewer.getRole() != UserRole.INTERVIEWER) {
            throw new IllegalArgumentException(
                    "User is not an interviewer"
            );
        }

        return availabilityRepository
                .findByInterviewer_IdAndDayOfWeekAndEnabledTrue(
                        interviewerId,
                        dayOfWeek
                );
    }

}
