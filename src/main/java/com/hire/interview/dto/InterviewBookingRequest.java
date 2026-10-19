
package com.hire.interview.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record InterviewBookingRequest(

        @NotNull
        @Positive
        Long candidateId,

        @NotNull
        @Positive
        Long interviewerId,

        @NotNull
        Instant startAt,

        @NotNull
        Instant endAt

) {}
