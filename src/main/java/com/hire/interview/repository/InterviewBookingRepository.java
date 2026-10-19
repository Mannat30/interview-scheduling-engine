
package com.hire.interview.repository;

import com.hire.interview.entity.InterviewBooking;
import com.hire.interview.entity.InterviewStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface InterviewBookingRepository
        extends JpaRepository<InterviewBooking, Long> {

    @Query("""
        SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
        FROM InterviewBooking b
        WHERE b.status = :status
          AND (b.interviewer.id = :userId OR b.candidate.id = :userId)
          AND b.startAt < :requestedEnd
          AND b.endAt > :requestedStart
        """)
    boolean existsConflictingBooking(
            @Param("userId") Long userId,
            @Param("requestedStart") Instant requestedStart,
            @Param("requestedEnd") Instant requestedEnd,
            @Param("status") InterviewStatus status
    );

    @Query("""
    SELECT b FROM InterviewBooking b
    WHERE b.interviewer.id = :interviewerId
      AND b.status = :status
      AND b.startAt < :rangeEnd
      AND b.endAt > :rangeStart
    ORDER BY b.startAt
    """)
    List<InterviewBooking> findOverlappingBookingsForInterviewer(
            @Param("interviewerId") Long interviewerId,
            @Param("rangeStart") Instant rangeStart,
            @Param("rangeEnd") Instant rangeEnd,
            @Param("status") InterviewStatus status
    );

    @Query("""
    SELECT b FROM InterviewBooking b
    WHERE (b.interviewer.id = :userId OR b.candidate.id = :userId)
      AND b.status = :status
      AND b.startAt < :rangeEnd
      AND b.endAt > :rangeStart
    ORDER BY b.startAt
    """)
    List<InterviewBooking> findOverlappingBookingsForParticipant(
            @Param("userId") Long userId,
            @Param("rangeStart") Instant rangeStart,
            @Param("rangeEnd") Instant rangeEnd,
            @Param("status") InterviewStatus status
    );
    @Query("""
    SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
    FROM InterviewBooking b
    WHERE b.id <> :excludedBookingId
      AND b.status = :status
      AND (b.interviewer.id = :userId OR b.candidate.id = :userId)
      AND b.startAt < :requestedEnd
      AND b.endAt > :requestedStart
    """)
    boolean existsConflictingBookingExcludingBooking(
            @Param("excludedBookingId") Long excludedBookingId,
            @Param("userId") Long userId,
            @Param("requestedStart") Instant requestedStart,
            @Param("requestedEnd") Instant requestedEnd,
            @Param("status") InterviewStatus status
    );
    Page<InterviewBooking> findByCandidate_IdOrInterviewer_IdOrderByStartAtDesc(
            Long candidateId,
            Long interviewerId,
            Pageable pageable
    );@Query("""
    SELECT b FROM InterviewBooking b
    WHERE (b.candidate.id = :participantId
        OR b.interviewer.id = :participantId)
      AND b.status = :status
      AND b.startAt >= :now
    ORDER BY b.startAt ASC
    """)
    Page<InterviewBooking> findUpcomingBookingsForParticipant(
                    @Param("participantId") Long participantId,
                    @Param("status") InterviewStatus status,
                    @Param("now") Instant now,
                    Pageable pageable
            );
    @Query("""
    SELECT b FROM InterviewBooking b
    WHERE (b.candidate.id = :participantId
        OR b.interviewer.id = :participantId)
      AND b.status = :status
      AND b.endAt < :now
    ORDER BY b.endAt DESC
    """)
    Page<InterviewBooking> findCompletedBookingsForParticipant(
            @Param("participantId") Long participantId,
            @Param("status") InterviewStatus status,
            @Param("now") Instant now,
            Pageable pageable
    );


}
