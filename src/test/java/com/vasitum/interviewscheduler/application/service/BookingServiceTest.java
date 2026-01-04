package com.vasitum.interviewscheduler.application.service;

import com.vasitum.interviewscheduler.application.exception.SlotFullyBookedException;
import com.vasitum.interviewscheduler.application.exception.WeeklyLimitExceededException;
import com.vasitum.interviewscheduler.domain.model.Booking;
import com.vasitum.interviewscheduler.domain.model.InterviewSlot;
import com.vasitum.interviewscheduler.domain.model.Interviewer;
import com.vasitum.interviewscheduler.domain.repository.BookingRepository;
import com.vasitum.interviewscheduler.domain.repository.InterviewSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BookingServiceTest {

    private InterviewSlotRepository slotRepository;
    private BookingRepository bookingRepository;
    private BookingService bookingService;

    @BeforeEach
    void setUp() {
        slotRepository = mock(InterviewSlotRepository.class);
        bookingRepository = mock(BookingRepository.class);
        bookingService = new BookingService(slotRepository, bookingRepository);
    }

    @Test
    void createBooking_success_whenCapacityAndWeeklyLimitAvailable() {
        Interviewer interviewer = new Interviewer();
        interviewer.setId(1L);
        interviewer.setMaxWeeklyInterviews(10);

        InterviewSlot slot = new InterviewSlot();
        slot.setId(5L);
        slot.setInterviewer(interviewer);
        slot.setStartTime(LocalDateTime.now().withHour(10));
        slot.setEndTime(slot.getStartTime().plusMinutes(30));
        slot.setBookedCount(0);

        when(slotRepository.findWithLockingById(5L)).thenReturn(Optional.of(slot));
        when(bookingRepository.countBySlot_Interviewer_IdAndSlot_StartTimeBetween(any(), any(), any()))
                .thenReturn(0L);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Booking booking = bookingService.createBooking(5L, "Alice", "alice@example.com");

        assertThat(booking.getSlot()).isEqualTo(slot);
        assertThat(slot.getBookedCount()).isEqualTo(1);

        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository).save(captor.capture());
        assertThat(captor.getValue().getCandidateEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void createBooking_throws_whenSlotFull() {
        Interviewer interviewer = new Interviewer();
        interviewer.setId(1L);
        interviewer.setMaxWeeklyInterviews(10);

        InterviewSlot slot = new InterviewSlot();
        slot.setId(5L);
        slot.setInterviewer(interviewer);
        slot.setStartTime(LocalDateTime.now().withHour(10));
        slot.setEndTime(slot.getStartTime().plusMinutes(30));
        slot.setBookedCount(1); // capacity 1

        when(slotRepository.findWithLockingById(5L)).thenReturn(Optional.of(slot));

        assertThatThrownBy(() -> bookingService.createBooking(5L, "Alice", "alice@example.com"))
                .isInstanceOf(SlotFullyBookedException.class);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_throws_whenWeeklyLimitExceeded() {
        Interviewer interviewer = new Interviewer();
        interviewer.setId(1L);
        interviewer.setMaxWeeklyInterviews(1);

        InterviewSlot slot = new InterviewSlot();
        slot.setId(5L);
        slot.setInterviewer(interviewer);
        slot.setStartTime(LocalDateTime.now().withHour(10));
        slot.setEndTime(slot.getStartTime().plusMinutes(30));
        slot.setBookedCount(0);

        when(slotRepository.findWithLockingById(5L)).thenReturn(Optional.of(slot));
        when(bookingRepository.countBySlot_Interviewer_IdAndSlot_StartTimeBetween(any(), any(), any()))
                .thenReturn(1L);

        assertThatThrownBy(() -> bookingService.createBooking(5L, "Alice", "alice@example.com"))
                .isInstanceOf(WeeklyLimitExceededException.class);
        verify(bookingRepository, never()).save(any());
    }
}


