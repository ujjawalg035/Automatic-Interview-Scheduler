package com.vasitum.interviewscheduler.application.service;

import com.vasitum.interviewscheduler.domain.model.InterviewSlot;
import com.vasitum.interviewscheduler.domain.model.Interviewer;
import com.vasitum.interviewscheduler.domain.model.WeeklyAvailability;
import com.vasitum.interviewscheduler.domain.repository.InterviewSlotRepository;
import com.vasitum.interviewscheduler.domain.repository.InterviewerRepository;
import com.vasitum.interviewscheduler.domain.repository.WeeklyAvailabilityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SlotGenerationServiceTest {

    private InterviewerRepository interviewerRepository;
    private WeeklyAvailabilityRepository availabilityRepository;
    private InterviewSlotRepository slotRepository;
    private SlotGenerationService service;

    @BeforeEach
    void setUp() {
        interviewerRepository = mock(InterviewerRepository.class);
        availabilityRepository = mock(WeeklyAvailabilityRepository.class);
        slotRepository = mock(InterviewSlotRepository.class);
        service = new SlotGenerationService(interviewerRepository, availabilityRepository, slotRepository);
    }

    @Test
    void generateSlots_splitsWindowByDuration() {
        Interviewer interviewer = new Interviewer();
        interviewer.setId(1L);

        WeeklyAvailability availability = new WeeklyAvailability();
        availability.setInterviewer(interviewer);
        availability.setDayOfWeek(DayOfWeek.MONDAY);
        availability.setStartTime(LocalTime.of(9, 0));
        availability.setEndTime(LocalTime.of(11, 0));
        availability.setSlotDurationMinutes(30);

        when(interviewerRepository.findById(1L)).thenReturn(Optional.of(interviewer));
        when(availabilityRepository.findByInterviewerId(1L)).thenReturn(List.of(availability));
        when(slotRepository.findByInterviewerIdAndStartTimeBetweenOrderByStartTimeAsc(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        int created = service.generateSlotsForInterviewer(1L, monday, monday);

        // 9-9:30, 9:30-10, 10-10:30, 10:30-11 => 4 slots
        assertThat(created).isEqualTo(4);
        verify(slotRepository, times(4)).save(any(InterviewSlot.class));
    }
}


