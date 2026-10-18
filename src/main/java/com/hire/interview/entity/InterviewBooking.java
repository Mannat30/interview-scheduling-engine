package com.hire.interview.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
        name = "interview_bookings",
        indexes = {
                @Index(
                        name = "idx_booking_interviewer_times",
                        columnList = "interviewer_id,status,start_at,end_at"
                ),
                @Index(
                        name = "idx_booking_candidate_times",
                        columnList = "candidate_id,status,start_at,end_at"
                )
        }
)
public class InterviewBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "interviewer_id", nullable = false)
    private User interviewer;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at", nullable = false)
    private Instant endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InterviewStatus status = InterviewStatus.SCHEDULED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected InterviewBooking() {
        // Required by JPA
    }

    public InterviewBooking(
            User candidate,
            User interviewer,
            Instant startAt,
            Instant endAt
    ) {
        this.candidate = candidate;
        this.interviewer = interviewer;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = InterviewStatus.SCHEDULED;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getCandidate() {
        return candidate;
    }

    public User getInterviewer() {
        return interviewer;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public InterviewStatus getStatus() {
        return status;
    }

    public void setStatus(InterviewStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}