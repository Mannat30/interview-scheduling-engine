
package com.hire.interview.controller;

import com.hire.interview.dto.AvailableSlotResponse;
import com.hire.interview.service.AvailableSlotService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/interviewers")
public class AvailableSlotController {

    private final AvailableSlotService availableSlotService;

    public AvailableSlotController(
            AvailableSlotService availableSlotService
    ) {
        this.availableSlotService = availableSlotService;
    }

    @GetMapping("/{interviewerId}/available-slots")
    public List<AvailableSlotResponse> getAvailableSlots(
            @PathVariable Long interviewerId,

            @RequestParam Long candidateId,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @RequestParam(defaultValue = "30")
            int durationMinutes
    ) {
        return availableSlotService.getAvailableSlots(
                interviewerId,
                candidateId,
                date,
                durationMinutes
        );
    }
}
