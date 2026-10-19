
package com.hire.interview.dto;

import java.time.Instant;
import java.time.LocalDateTime;

public record AvailableSlotResponse(
        Instant startAt,
        Instant endAt,
        LocalDateTime localStartAt,
        LocalDateTime localEndAt,
        String timeZone
) {}
