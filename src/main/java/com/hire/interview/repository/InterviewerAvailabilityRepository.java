
package com.hire.interview.repository;

import com.hire.interview.entity.InterviewerAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;

public interface InterviewerAvailabilityRepository
        extends JpaRepository<InterviewerAvailability, Long> {

    List<InterviewerAvailability>
    findByInterviewer_IdAndDayOfWeekAndEnabledTrue(
            Long interviewerId,
            DayOfWeek dayOfWeek
    );
}
