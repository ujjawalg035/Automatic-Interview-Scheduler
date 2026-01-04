package com.vasitum.interviewscheduler.application.service;

import com.vasitum.interviewscheduler.application.exception.NotFoundException;
import com.vasitum.interviewscheduler.domain.model.Interviewer;
import com.vasitum.interviewscheduler.domain.model.WeeklyAvailability;
import com.vasitum.interviewscheduler.domain.repository.InterviewerRepository;
import com.vasitum.interviewscheduler.domain.repository.WeeklyAvailabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AvailabilityService {

    private final InterviewerRepository interviewerRepository;
    private final WeeklyAvailabilityRepository availabilityRepository;

    public AvailabilityService(InterviewerRepository interviewerRepository,
                               WeeklyAvailabilityRepository availabilityRepository) {
        this.interviewerRepository = interviewerRepository;
        this.availabilityRepository = availabilityRepository;
    }

    @Transactional
    public List<WeeklyAvailability> replaceWeeklyAvailability(
            Long interviewerId,
            List<WeeklyAvailabilityInput> inputs
    ) {
        Interviewer interviewer = interviewerRepository.findById(interviewerId)
                .orElseThrow(() -> new NotFoundException("Interviewer " + interviewerId + " not found"));

        availabilityRepository.deleteAll(availabilityRepository.findByInterviewerId(interviewerId));

        List<WeeklyAvailability> saved = new ArrayList<>();
        for (WeeklyAvailabilityInput input : inputs) {
            if (input.startTime().isAfter(input.endTime())) {
                throw new IllegalArgumentException("startTime must be before endTime");
            }
            WeeklyAvailability availability = new WeeklyAvailability();
            availability.setInterviewer(interviewer);
            availability.setDayOfWeek(input.dayOfWeek());
            availability.setStartTime(input.startTime());
            availability.setEndTime(input.endTime());
            availability.setSlotDurationMinutes(input.slotDurationMinutes());
            saved.add(availabilityRepository.save(availability));
        }
        return saved;
    }

    public record WeeklyAvailabilityInput(
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            Integer slotDurationMinutes
    ) {
    }
}


