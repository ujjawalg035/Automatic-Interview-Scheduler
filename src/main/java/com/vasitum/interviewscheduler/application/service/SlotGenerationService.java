package com.vasitum.interviewscheduler.application.service;

import com.vasitum.interviewscheduler.application.exception.NotFoundException;
import com.vasitum.interviewscheduler.domain.model.InterviewSlot;
import com.vasitum.interviewscheduler.domain.model.Interviewer;
import com.vasitum.interviewscheduler.domain.model.WeeklyAvailability;
import com.vasitum.interviewscheduler.domain.repository.InterviewSlotRepository;
import com.vasitum.interviewscheduler.domain.repository.InterviewerRepository;
import com.vasitum.interviewscheduler.domain.repository.WeeklyAvailabilityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class SlotGenerationService {

    private final InterviewerRepository interviewerRepository;
    private final WeeklyAvailabilityRepository availabilityRepository;
    private final InterviewSlotRepository slotRepository;

    public SlotGenerationService(InterviewerRepository interviewerRepository,
                                 WeeklyAvailabilityRepository availabilityRepository,
                                 InterviewSlotRepository slotRepository) {
        this.interviewerRepository = interviewerRepository;
        this.availabilityRepository = availabilityRepository;
        this.slotRepository = slotRepository;
    }

    @Transactional
    public int generateSlotsForInterviewer(Long interviewerId, LocalDate from, LocalDate to) {
        Interviewer interviewer = interviewerRepository.findById(interviewerId)
                .orElseThrow(() -> new NotFoundException("Interviewer " + interviewerId + " not found"));

        List<WeeklyAvailability> availabilities = availabilityRepository.findByInterviewerId(interviewerId);

        int created = 0;
        LocalDate current = from;
        while (!current.isAfter(to)) {
            for (WeeklyAvailability availability : availabilities) {
                if (availability.getDayOfWeek() == current.getDayOfWeek()) {
                    created += generateSlotsForDay(interviewer, availability, current);
                }
            }
            current = current.plusDays(1);
        }
        return created;
    }

    private int generateSlotsForDay(Interviewer interviewer,
                                    WeeklyAvailability availability,
                                    LocalDate day) {
        LocalTime start = availability.getStartTime();
        LocalTime end = availability.getEndTime();
        int duration = availability.getSlotDurationMinutes();

        int created = 0;
        LocalDateTime slotStart = LocalDateTime.of(day, start);
        while (!slotStart.plusMinutes(duration).isAfter(LocalDateTime.of(day, end))) {
            LocalDateTime slotEnd = slotStart.plusMinutes(duration);

            // avoid duplicate slots by checking for existing in range
            List<InterviewSlot> existing = slotRepository
                    .findByInterviewerIdAndStartTimeBetweenOrderByStartTimeAsc(
                            interviewer.getId(),
                            slotStart,
                            slotEnd
                    );
            if (existing.isEmpty()) {
                InterviewSlot slot = new InterviewSlot();
                slot.setInterviewer(interviewer);
                slot.setStartTime(slotStart);
                slot.setEndTime(slotEnd);
                slot.setBookedCount(0);
                slotRepository.save(slot);
                created++;
            }

            slotStart = slotEnd;
        }
        return created;
    }
}


