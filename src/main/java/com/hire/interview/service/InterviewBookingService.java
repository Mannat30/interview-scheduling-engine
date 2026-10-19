package com.hire.interview.service;

import com.hire.interview.dto.InterviewBookingResponse;
import com.hire.interview.entity.*;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

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
            InterviewerAvailabilityRepository availabilityRepository) {

        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.availabilityRepository = availabilityRepository;
    }

    @Transactional
    public InterviewBooking scheduleInterview(
            Long candidateId,
            Long interviewerId,
            Instant startAt,
            Instant endAt
    ) {
        // 1. Validate IDs.
        if (candidateId == null || interviewerId == null) {
            throw new IllegalArgumentException(
                    "Candidate ID and interviewer ID are required"
            );
        }

        if (candidateId.equals(interviewerId)) {
            throw new IllegalArgumentException(
                    "Candidate and interviewer must be different users"
            );
        }

        // 2. Validate interview times.
        if (startAt == null || endAt == null) {
            throw new IllegalArgumentException(
                    "Start time and end time are required"
            );
        }

        if (!startAt.isBefore(endAt)) {
            throw new IllegalArgumentException(
                    "Start time must be before end time"
            );
        }

        if (!startAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException(
                    "Interview must be scheduled in the future"
            );
        }

        // 3. Lock users in ascending ID order.
        // This reduces deadlock risk when requests overlap.
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

        // Restore the candidate and interviewer roles
        // because users were loaded in ID order.
        User candidate;
        User interviewer;

        if (candidateId.equals(firstLockId)) {
            candidate = firstUser;
            interviewer = secondUser;
        } else {
            candidate = secondUser;
            interviewer = firstUser;
        }

        // 4. Verify user roles.
        if (candidate.getRole() != UserRole.CANDIDATE) {
            throw new IllegalArgumentException(
                    "Selected candidate does not have the CANDIDATE role"
            );
        }

        if (interviewer.getRole() != UserRole.INTERVIEWER) {
            throw new IllegalArgumentException(
                    "Selected interviewer does not have the INTERVIEWER role"
            );
        }

        // 5. Check whether the candidate is already booked.
        boolean candidateBusy =
                bookingRepository.existsConflictingBooking(
                        candidateId,
                        startAt,
                        endAt,
                        InterviewStatus.SCHEDULED
                );

        // 6. Check whether the interviewer is already booked.
        boolean interviewerBusy =
                bookingRepository.existsConflictingBooking(
                        interviewerId,
                        startAt,
                        endAt,
                        InterviewStatus.SCHEDULED
                );

        if (candidateBusy || interviewerBusy) {
            throw new BookingConflictException(
                    "Candidate or interviewer already has an overlapping interview"
            );
        }

        // 7. Create the booking.
        InterviewBooking booking = new InterviewBooking(
                candidate,
                interviewer,
                startAt,
                endAt
        );

        // 8. Save the booking to PostgreSQL.
        return bookingRepository.save(booking);
    }

    @Transactional
    public InterviewBooking cancelInterview(Long bookingId) {

        if (bookingId == null || bookingId <= 0) {
            throw new IllegalArgumentException(
                    "A valid booking ID is required"
            );
        }

        // First, find the booking so we know which users to lock.
        InterviewBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found: " + bookingId
                ));

        Long candidateId = booking.getCandidate().getId();
        Long interviewerId = booking.getInterviewer().getId();

        // Lock participants in ascending ID order, matching the
        // order used by the booking service.
        Long firstId = Math.min(candidateId, interviewerId);
        Long secondId = Math.max(candidateId, interviewerId);

        userRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + firstId
                ));

        userRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + secondId
                ));

        // Refresh the booking under a database row lock so we check
        // its latest committed status.
        entityManager.refresh(booking, LockModeType.PESSIMISTIC_WRITE);

        if (booking.getStatus() != InterviewStatus.SCHEDULED) {
            throw new BookingConflictException(
                    "Only scheduled interviews can be cancelled"
            );
        }

        booking.setStatus(InterviewStatus.CANCELLED);

        return bookingRepository.save(booking);
    }

    @Transactional
    public InterviewBooking rescheduleInterview(
            Long bookingId,
            Instant newStartAt,
            Instant newEndAt
    ) {
        // 1. Validate the booking ID and new times.
        if (bookingId == null || bookingId <= 0) {
            throw new IllegalArgumentException(
                    "A valid booking ID is required"
            );
        }

        if (newStartAt == null || newEndAt == null) {
            throw new IllegalArgumentException(
                    "New start and end times are required"
            );
        }

        if (!newStartAt.isBefore(newEndAt)) {
            throw new IllegalArgumentException(
                    "Start time must be before end time"
            );
        }

        if (!newStartAt.isAfter(Instant.now())) {
            throw new IllegalArgumentException(
                    "Interview must be rescheduled to a future time"
            );
        }

        // 2. Find the existing booking.
        InterviewBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Booking not found: " + bookingId
                ));

        Long candidateId = booking.getCandidate().getId();
        Long interviewerId = booking.getInterviewer().getId();

        // 3. Lock participants in ascending ID order.
        Long firstId = Math.min(candidateId, interviewerId);
        Long secondId = Math.max(candidateId, interviewerId);

        userRepository.findByIdForUpdate(firstId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + firstId
                ));

        userRepository.findByIdForUpdate(secondId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + secondId
                ));

        // 4. Refresh and lock the booking itself.
        entityManager.refresh(booking, LockModeType.PESSIMISTIC_WRITE);

        if (booking.getStatus() != InterviewStatus.SCHEDULED) {
            throw new BookingConflictException(
                    "Only scheduled interviews can be rescheduled"
            );
        }

        // 5. Check the candidate for conflicts, excluding this booking.
        boolean candidateBusy =
                bookingRepository.existsConflictingBookingExcludingBooking(
                        bookingId,
                        candidateId,
                        newStartAt,
                        newEndAt,
                        InterviewStatus.SCHEDULED
                );

        // 6. Check the interviewer for conflicts.
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
                    "Candidate or interviewer already has an overlapping interview"
            );
        }

        // 7. Update the existing record, preserving its ID.
        booking.reschedule(newStartAt, newEndAt);

        // 8. Save the updated booking.
        return bookingRepository.save(booking);
    }

    private void validateInterviewerAvailability(
            User interviewer,
            Instant startAt,
            Instant endAt) {

        ZoneId zone = ZoneId.of(interviewer.getTimeZone());

        ZonedDateTime localStart = startAt.atZone(zone);
        ZonedDateTime localEnd = endAt.atZone(zone);

        // An interview must stay within one local calendar day.
        if (!localStart.toLocalDate().equals(localEnd.toLocalDate())) {
            throw new IllegalArgumentException(
                    "Interview must start and end on the same local date."
            );
        }

        LocalTime requestedStart = localStart.toLocalTime();
        LocalTime requestedEnd = localEnd.toLocalTime();

        List<InterviewerAvailability> windows =
                availabilityRepository
                        .findByInterviewer_IdAndDayOfWeekAndEnabledTrue(
                                interviewer.getId(),
                                localStart.getDayOfWeek()
                        );

        boolean fitsAvailableWindow = windows.stream().anyMatch(window ->
                !requestedStart.isBefore(window.getStartTime())
                        && !requestedEnd.isAfter(window.getEndTime())
        );

        if (!fitsAvailableWindow) {
            throw new BookingConflictException(
                    "Requested time is outside the interviewer's available hours."
            );
        }
    }@Transactional(readOnly = true)
    public Page<InterviewBookingResponse> getBookingsForParticipant(
            Long participantId,
            Pageable pageable) {

        userRepository.findById(participantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + participantId
                        )
                );

        Page<InterviewBooking> bookings =
                bookingRepository
                        .findByCandidate_IdOrInterviewer_IdOrderByStartAtDesc(
                                participantId,
                                participantId,
                                pageable
                        );

        return bookings.map(InterviewBookingResponse::from);
    }
    @Transactional(readOnly = true)
    public Page<InterviewBookingResponse> getUpcomingBookings(
            Long participantId,
            Pageable pageable) {

        userRepository.findById(participantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + participantId
                        )
                );

        Page<InterviewBooking> bookings =
                bookingRepository.findUpcomingBookingsForParticipant(
                        participantId,
                        InterviewStatus.SCHEDULED,
                        Instant.now(),
                        pageable
                );

        return bookings.map(InterviewBookingResponse::from);
    }
    @Transactional(readOnly = true)
    public Page<InterviewBookingResponse> getCompletedBookings(
            Long participantId,
            Pageable pageable) {

        userRepository.findById(participantId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + participantId
                        )
                );

        Page<InterviewBooking> bookings =
                bookingRepository.findCompletedBookingsForParticipant(
                        participantId,
                        InterviewStatus.COMPLETED,
                        Instant.now(),
                        pageable
                );

        return bookings.map(InterviewBookingResponse::from);
    }@Transactional
    public InterviewBooking completeInterview(Long bookingId) {

        InterviewBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Booking not found with id: " + bookingId
                        )
                );

        Long candidateId = booking.getCandidate().getId();
        Long interviewerId = booking.getInterviewer().getId();

        // Lock users in a consistent order to reduce deadlock risk.
        Long firstUserId = Math.min(candidateId, interviewerId);
        Long secondUserId = Math.max(candidateId, interviewerId);

        userRepository.findByIdForUpdate(firstUserId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found with id: " + firstUserId
                        )
                );

        if (!firstUserId.equals(secondUserId)) {
            userRepository.findByIdForUpdate(secondUserId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "User not found with id: " + secondUserId
                            )
                    );
        }

        // Refresh the booking and lock its database row.
        entityManager.refresh(booking, LockModeType.PESSIMISTIC_WRITE);

        if (booking.getStatus() != InterviewStatus.SCHEDULED) {
            throw new BookingConflictException(
                    "Only scheduled interviews can be marked completed."
            );
        }

        if (!booking.getEndAt().isBefore(Instant.now())) {
            throw new BookingConflictException(
                    "An interview can only be completed after its end time."
            );
        }

        booking.setStatus(InterviewStatus.COMPLETED);

        return bookingRepository.save(booking);
    }

}