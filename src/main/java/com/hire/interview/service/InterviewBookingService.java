package com.hire.interview.service;

import com.hire.interview.dto.InterviewBookingResponse;
import com.hire.interview.entity.InterviewBooking;
import com.hire.interview.entity.InterviewStatus;
import com.hire.interview.entity.InterviewerAvailability;
import com.hire.interview.entity.User;
import com.hire.interview.entity.UserRole;
import com.hire.interview.exception.BookingConflictException;
import com.hire.interview.exception.ResourceNotFoundException;
import com.hire.interview.repository.InterviewBookingRepository;
import com.hire.interview.repository.InterviewerAvailabilityRepository;
import com.hire.interview.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
public class InterviewBookingService {

    @PersistenceContext
    private EntityManager entityManager;

    private final UserRepository userRepository;
    private final InterviewBookingRepository bookingRepository;
    private final InterviewerAvailabilityRepository availabilityRepository;

    public InterviewBookingService(
            UserRepository userRepository,
            InterviewBookingRepository bookingRepository,
            InterviewerAvailabilityRepository availabilityRepository
    ) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.availabilityRepository = availabilityRepository;
    }

    // =========================================================
    // 1. SCHEDULE A NEW INTERVIEW
    // =========================================================

    @Transactional
    public InterviewBooking scheduleInterview(
            Long candidateId,
            Long interviewerId,
            Instant startAt,
            Instant endAt
    ) {
        // Validate participant IDs.
        if (candidateId == null || interviewerId == null
                || candidateId <= 0 || interviewerId <= 0) {
            throw new IllegalArgumentException(
                    "Valid candidate ID and interviewer ID are required."
            );
        }

        if (candidateId.equals(interviewerId)) {
            throw new IllegalArgumentException(
                    "Candidate and interviewer must be different users."
            );
        }

        // Validate interview times.
        validateInterviewTimes(startAt, endAt);

        // Lock participants in ascending ID order.
        // This helps serialize conflicting booking operations.
        Long firstLockId = Math.min(candidateId, interviewerId);
        Long secondLockId = Math.max(candidateId, interviewerId);

        User firstUser = userRepository
                .findByIdForUpdate(firstLockId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + firstLockId
                ));

        User secondUser = userRepository
                .findByIdForUpdate(secondLockId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + secondLockId
                ));

        // Restore candidate/interviewer after sorting IDs.
        User candidate;
        User interviewer;

        if (candidateId.equals(firstLockId)) {
            candidate = firstUser;
            interviewer = secondUser;
        } else {
            candidate = secondUser;
            interviewer = firstUser;
        }

        // Verify user roles.
        if (candidate.getRole() != UserRole.CANDIDATE) {
            throw new IllegalArgumentException(
                    "Selected candidate does not have the CANDIDATE role."
            );
        }

        if (interviewer.getRole() != UserRole.INTERVIEWER) {
            throw new IllegalArgumentException(
                    "Selected interviewer does not have the INTERVIEWER role."
            );
        }

        // IMPORTANT:
        // Ensure the requested slot falls inside configured availability.
        validateInterviewerAvailability(
                interviewer,
                startAt,
                endAt
        );

        // Check whether the candidate already has a conflicting booking.
        boolean candidateBusy =
                bookingRepository.existsConflictingBooking(
                        candidateId,
                        startAt,
                        endAt,
                        InterviewStatus.SCHEDULED
                );

        // Check whether the interviewer already has a conflicting booking.
        boolean interviewerBusy =
                bookingRepository.existsConflictingBooking(
                        interviewerId,
                        startAt,
                        endAt,
                        InterviewStatus.SCHEDULED
                );

        if (candidateBusy || interviewerBusy) {
            throw new BookingConflictException(
                    "Candidate or interviewer already has an overlapping interview."
            );
        }

        // Create and save the booking.
        InterviewBooking booking = new InterviewBooking(
                candidate,
                interviewer,
                startAt,
                endAt
        );

        return bookingRepository.save(booking);
    }

    // =========================================================
    // 2. CANCEL AN INTERVIEW
    // =========================================================

    @Transactional
    public InterviewBooking cancelInterview(Long bookingId) {

        validateBookingId(bookingId);

        // Find the booking.
        InterviewBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found: " + bookingId
                ));

        Long candidateId = booking.getCandidate().getId();
        Long interviewerId = booking.getInterviewer().getId();

        // Lock participants in a consistent order.
        lockParticipants(candidateId, interviewerId);

        // Refresh and lock the booking row.
        entityManager.refresh(
                booking,
                LockModeType.PESSIMISTIC_WRITE
        );

        // Only scheduled interviews can be cancelled.
        if (booking.getStatus() != InterviewStatus.SCHEDULED) {
            throw new BookingConflictException(
                    "Only scheduled interviews can be cancelled."
            );
        }

        booking.setStatus(InterviewStatus.CANCELLED);

        return bookingRepository.save(booking);
    }

    // =========================================================
    // 3. RESCHEDULE AN EXISTING INTERVIEW
    // =========================================================

    @Transactional
    public InterviewBooking rescheduleInterview(
            Long bookingId,
            Instant newStartAt,
            Instant newEndAt
    ) {
        validateBookingId(bookingId);

        // Validate the new interview times.
        validateInterviewTimes(newStartAt, newEndAt);

        // Find the existing booking.
        InterviewBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found: " + bookingId
                ));

        Long candidateId = booking.getCandidate().getId();
        Long interviewerId = booking.getInterviewer().getId();

        // Lock participants in a consistent order.
        lockParticipants(candidateId, interviewerId);

        // Refresh and lock the booking.
        entityManager.refresh(
                booking,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (booking.getStatus() != InterviewStatus.SCHEDULED) {
            throw new BookingConflictException(
                    "Only scheduled interviews can be rescheduled."
            );
        }

        // IMPORTANT:
        // Validate the new time against the interviewer's availability.
        validateInterviewerAvailability(
                booking.getInterviewer(),
                newStartAt,
                newEndAt
        );

        // Check candidate conflicts, excluding this booking.
        boolean candidateBusy =
                bookingRepository.existsConflictingBookingExcludingBooking(
                        bookingId,
                        candidateId,
                        newStartAt,
                        newEndAt,
                        InterviewStatus.SCHEDULED
                );

        // Check interviewer conflicts, excluding this booking.
        boolean interviewerBusy =
                bookingRepository.existsConflictingBookingExcludingBooking(
                        bookingId,
                        interviewerId,
                        newStartAt,
                        newEndAt,
                        InterviewStatus.SCHEDULED
                );

        if (candidateBusy || interviewerBusy) {
            throw new BookingConflictException(
                    "Candidate or interviewer already has an overlapping interview."
            );
        }

        // Update the same booking instead of creating a new one.
        booking.reschedule(newStartAt, newEndAt);

        return bookingRepository.save(booking);
    }

    // =========================================================
    // 4. VALIDATE INTERVIEW DATE AND TIME
    // =========================================================

    private void validateInterviewTimes(
            Instant startAt,
            Instant endAt
    ) {
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException(
                    "Start time and end time are required."
            );
        }

        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException(
                    "Start time must be before end time."
            );
        }

        if (!startAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException(
                    "Interview must be scheduled in the future."
            );
        }
    }

    private void validateBookingId(Long bookingId) {
        if (bookingId == null || bookingId <= 0) {
            throw new IllegalArgumentException(
                    "A valid booking ID is required."
            );
        }
    }

    // =========================================================
    // 5. VALIDATE INTERVIEWER AVAILABILITY
    // =========================================================

    private void validateInterviewerAvailability(
            User interviewer,
            Instant startAt,
            Instant endAt
    ) {
        if (interviewer.getTimeZone() == null
                || interviewer.getTimeZone().isBlank()) {
            throw new IllegalArgumentException(
                    "Interviewer timezone is not configured."
            );
        }

        ZoneId zone;

        try {
            zone = ZoneId.of(interviewer.getTimeZone());
        } catch (DateTimeException exception) {
            throw new IllegalArgumentException(
                    "Interviewer timezone is invalid.",
                    exception
            );
        }

        // Convert UTC instants into the interviewer's local timezone.
        ZonedDateTime localStart = startAt.atZone(zone);
        ZonedDateTime localEnd = endAt.atZone(zone);

        // Interviews must start and finish on the same local date.
        if (!localStart.toLocalDate().equals(localEnd.toLocalDate())) {
            throw new IllegalArgumentException(
                    "Interview must start and end on the same local date."
            );
        }

        LocalTime requestedStart = localStart.toLocalTime();
        LocalTime requestedEnd = localEnd.toLocalTime();

        // Fetch enabled availability windows for this weekday.
        List<InterviewerAvailability> windows =
                availabilityRepository
                        .findByInterviewer_IdAndDayOfWeekAndEnabledTrue(
                                interviewer.getId(),
                                localStart.getDayOfWeek()
                        );

        // The requested interview must fit entirely within a window.
        boolean fitsAvailableWindow = windows.stream().anyMatch(window ->
                !requestedStart.isBefore(window.getStartTime())
                        && !requestedEnd.isAfter(window.getEndTime())
        );

        if (!fitsAvailableWindow) {
            throw new BookingConflictException(
                    "Requested time is outside the interviewer's available hours."
            );
        }
    }

    // =========================================================
    // 6. LOCK BOTH PARTICIPANTS
    // =========================================================

    private void lockParticipants(
            Long candidateId,
            Long interviewerId
    ) {
        Long firstId = Math.min(candidateId, interviewerId);
        Long secondId = Math.max(candidateId, interviewerId);

        userRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + firstId
                ));

        if (!firstId.equals(secondId)) {
            userRepository.findByIdForUpdate(secondId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User not found: " + secondId
                    ));
        }
    }

    // =========================================================
    // 7. GET ALL BOOKINGS FOR A PARTICIPANT
    // =========================================================

    @Transactional(readOnly = true)
    public Page<InterviewBookingResponse> getBookingsForParticipant(
            Long participantId,
            Pageable pageable
    ) {
        userRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + participantId
                ));

        Page<InterviewBooking> bookings =
                bookingRepository
                        .findByCandidate_IdOrInterviewer_IdOrderByStartAtDesc(
                                participantId,
                                participantId,
                                pageable
                        );

        return bookings.map(InterviewBookingResponse::from);
    }

    // =========================================================
    // 8. GET UPCOMING BOOKINGS
    // =========================================================

    @Transactional(readOnly = true)
    public Page<InterviewBookingResponse> getUpcomingBookings(
            Long participantId,
            Pageable pageable
    ) {
        userRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + participantId
                ));

        Page<InterviewBooking> bookings =
                bookingRepository.findUpcomingBookingsForParticipant(
                        participantId,
                        InterviewStatus.SCHEDULED,
                        Instant.now(),
                        pageable
                );

        return bookings.map(InterviewBookingResponse::from);
    }

    // =========================================================
    // 9. GET COMPLETED BOOKINGS
    // =========================================================

    @Transactional(readOnly = true)
    public Page<InterviewBookingResponse> getCompletedBookings(
            Long participantId,
            Pageable pageable
    ) {
        userRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with id: " + participantId
                ));

        Page<InterviewBooking> bookings =
                bookingRepository.findCompletedBookingsForParticipant(
                        participantId,
                        InterviewStatus.COMPLETED,
                        Instant.now(),
                        pageable
                );

        return bookings.map(InterviewBookingResponse::from);
    }

    // =========================================================
    // 10. MARK AN INTERVIEW AS COMPLETED
    // =========================================================

    @Transactional
    public InterviewBooking completeInterview(Long bookingId) {

        validateBookingId(bookingId);

        InterviewBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found with id: " + bookingId
                ));

        Long candidateId = booking.getCandidate().getId();
        Long interviewerId = booking.getInterviewer().getId();

        // Lock participants in a consistent order.
        lockParticipants(candidateId, interviewerId);

        // Refresh and lock the booking.
        entityManager.refresh(
                booking,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (booking.getStatus() != InterviewStatus.SCHEDULED) {
            throw new BookingConflictException(
                    "Only scheduled interviews can be marked completed."
            );
        }

        // An interview can only be marked complete after its end time.
        if (booking.getEndAt() == null
                || !booking.getEndAt().isBefore(Instant.now())) {
            throw new BookingConflictException(
                    "An interview can only be completed after its end time."
            );
        }

        booking.setStatus(InterviewStatus.COMPLETED);

        return bookingRepository.save(booking);
    }
}