package com.hire.interview.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record RescheduleBookingRequest(

        @NotNull
        Instant startAt,

        @NotNull
        Instant endAt

) {}