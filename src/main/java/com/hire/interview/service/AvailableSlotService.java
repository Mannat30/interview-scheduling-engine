
package com.hire.interview.service;

import com.hire.interview.dto.AvailableSlotResponse;
import com.hire.interview.entity.InterviewBooking;
import com.hire.interview.entity.InterviewStatus;
import com.hire.interview.entity.InterviewerAvailability;
import com.hire.interview.entity.User;
import com.hire.interview.entity.UserRole;
import com.hire.interview.exception.ResourceNotFoundException;
import com.hire.interview.repository.InterviewBookingRepository;
import com.hire.interview.repository.InterviewerAvailabilityRepository;
import com.hire.interview.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class AvailableSlotService {

    private final UserRepository userRepository;
    private final InterviewerAvailabilityRepository availabilityRepository;
    private final InterviewBookingRepository bookingRepository;

    public AvailableSlotService(
            UserRepository userRepository,
            InterviewerAvailabilityRepository availabilityRepository,
            InterviewBookingRepository bookingRepository
    ) {
        this.userRepository = userRepository;
        this.availabilityRepository = availabilityRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional(readOnly = true)
    public List<AvailableSlotResponse> getAvailableSlots(
            Long interviewerId,
            Long candidateId,
            LocalDate date,
            int durationMinutes
    ) {
        // 1. Validate IDs and request data.
        if (interviewerId == null || interviewerId <= 0
                || candidateId == null || candidateId <= 0) {
            throw new IllegalArgumentException(
                    "Valid interviewer and candidate IDs are required"
            );
        }

        if (interviewerId.equals(candidateId)) {
            throw new IllegalArgumentException(
                    "Candidate and interviewer must be different users"
            );
        }

        if (date == null) {
            throw new IllegalArgumentException("Date is required");
        }

        if (durationMinutes < 15
                || durationMinutes > 240
                || durationMinutes % 15 != 0) {
            throw new IllegalArgumentException(
                    "Duration must be 15 to 240 minutes in 15-minute increments"
            );
        }

        // 2. Find both participants.
        User interviewer = userRepository.findById(interviewerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Interviewer not found: " + interviewerId
                ));

        User candidate = userRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Candidate not found: " + candidateId
                ));

        // 3. Validate their roles.
        if (interviewer.getRole() != UserRole.INTERVIEWER) {
            throw new IllegalArgumentException(
                    "Selected user is not an interviewer"
            );
        }

        if (candidate.getRole() != UserRole.CANDIDATE) {
            throw new IllegalArgumentException(
                    "Selected user is not a candidate"
            );
        }

        // 4. Get the interviewer's time zone.
        ZoneId zone;

        try {
            zone = ZoneId.of(interviewer.getTimeZone());
        } catch (DateTimeException exception) {
            throw new IllegalStateException(
                    "Interviewer has an invalid time-zone configuration"
            );
        }

        if (date.isBefore(LocalDate.now(zone))) {
            throw new IllegalArgumentException(
                    "Cannot search for availability in the past"
            );
        }

        // 5. Get the interviewer's enabled working windows.
        List<InterviewerAvailability> windows =
                availabilityRepository
                        .findByInterviewer_IdAndDayOfWeekAndEnabledTrue(
                                interviewerId,
                                date.getDayOfWeek()
                        );

        if (windows.isEmpty()) {
            return List.of();
        }

        // 6. Convert the local day into exact instants.
        Instant dayStart = date.atStartOfDay(zone).toInstant();

        Instant nextDayStart = date.plusDays(1)
                .atStartOfDay(zone)
                .toInstant();

        // 7. Fetch the interviewer's scheduled bookings.
        List<InterviewBooking> interviewerBookings =
                bookingRepository.findOverlappingBookingsForInterviewer(
                        interviewerId,
                        dayStart,
                        nextDayStart,
                        InterviewStatus.SCHEDULED
                );

        // 8. Fetch the candidate's scheduled bookings.
        List<InterviewBooking> candidateBookings =
                bookingRepository.findOverlappingBookingsForParticipant(
                        candidateId,
                        dayStart,
                        nextDayStart,
                        InterviewStatus.SCHEDULED
                );

        // 9. Generate slots and remove conflicts.
        List<AvailableSlotResponse> availableSlots = new ArrayList<>();
        Instant now = Instant.now();

        for (InterviewerAvailability window : windows) {

            Instant windowStart = date
                    .atTime(window.getStartTime())
                    .atZone(zone)
                    .toInstant();

            Instant windowEnd = date
                    .atTime(window.getEndTime())
                    .atZone(zone)
                    .toInstant();

            Instant slotStart = windowStart;

            while (true) {
                Instant slotEnd = slotStart.plus(
                        durationMinutes,
                        ChronoUnit.MINUTES
                );

                // The complete slot must fit in the working window.
                if (slotEnd.isAfter(windowEnd)) {
                    break;
                }

                Instant currentSlotStart = slotStart;
                Instant currentSlotEnd = slotEnd;

                // Don't offer slots that have already started.
                boolean isFuture = currentSlotStart.isAfter(now);

                // Check overlap with the interviewer.
                boolean interviewerBusy =
                        interviewerBookings.stream().anyMatch(booking ->
                                currentSlotStart.isBefore(booking.getEndAt())
                                        && currentSlotEnd.isAfter(
                                        booking.getStartAt()
                                )
                        );

                // Check overlap with the candidate.
                boolean candidateBusy =
                        candidateBookings.stream().anyMatch(booking ->
                                currentSlotStart.isBefore(booking.getEndAt())
                                        && currentSlotEnd.isAfter(
                                        booking.getStartAt()
                                )
                        );

                // Return the slot only if both participants are free.
                if (isFuture && !interviewerBusy && !candidateBusy) {
                    LocalDateTime localStart =
                            LocalDateTime.ofInstant(currentSlotStart, zone);

                    LocalDateTime localEnd =
                            LocalDateTime.ofInstant(currentSlotEnd, zone);

                    availableSlots.add(
                            new AvailableSlotResponse(
                                    currentSlotStart,
                                    currentSlotEnd,
                                    localStart,
                                    localEnd,
                                    zone.getId()
                            )
                    );
                }

                slotStart = slotEnd;
            }
        }

        return List.copyOf(availableSlots);
    }
}
