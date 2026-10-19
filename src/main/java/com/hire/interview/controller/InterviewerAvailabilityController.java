
package com.hire.interview.controller;

import com.hire.interview.dto.InterviewerAvailabilityRequest;
import com.hire.interview.dto.InterviewerAvailabilityResponse;
import com.hire.interview.entity.InterviewerAvailability;
import com.hire.interview.service.InterviewerAvailabilityService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.List;

@RestController
@RequestMapping("/api/interviewers/{interviewerId}/availability")
public class InterviewerAvailabilityController {

    private final InterviewerAvailabilityService availabilityService;

    public InterviewerAvailabilityController(
            InterviewerAvailabilityService availabilityService
    ) {
        this.availabilityService = availabilityService;
    }

    @PostMapping
    public ResponseEntity<InterviewerAvailabilityResponse> addAvailability(
            @PathVariable Long interviewerId,
            @Valid @RequestBody InterviewerAvailabilityRequest request
    ) {
        InterviewerAvailability availability =
                availabilityService.addAvailability(
                        interviewerId,
                        request.dayOfWeek(),
                        request.startTime(),
                        request.endTime()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(InterviewerAvailabilityResponse.from(availability));
    }

    @GetMapping
    public List<InterviewerAvailabilityResponse> getAvailability(
            @PathVariable Long interviewerId,
            @RequestParam DayOfWeek dayOfWeek
    ) {
        return availabilityService
                .getAvailability(interviewerId, dayOfWeek)
                .stream()
                .map(InterviewerAvailabilityResponse::from)
                .toList();
    }
}
