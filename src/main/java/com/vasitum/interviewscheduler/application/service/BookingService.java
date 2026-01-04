package com.vasitum.interviewscheduler.application.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vasitum.interviewscheduler.application.exception.AlreadyBookedException;
import com.vasitum.interviewscheduler.application.exception.NotFoundException;
import com.vasitum.interviewscheduler.application.exception.SlotFullyBookedException;
import com.vasitum.interviewscheduler.application.exception.WeeklyLimitExceededException;
import com.vasitum.interviewscheduler.domain.model.Booking;
import com.vasitum.interviewscheduler.domain.model.InterviewSlot;
import com.vasitum.interviewscheduler.domain.model.Interviewer;
import com.vasitum.interviewscheduler.domain.repository.BookingRepository;
import com.vasitum.interviewscheduler.domain.repository.InterviewSlotRepository;

import jakarta.persistence.OptimisticLockException;

@Service
public class BookingService {

    private final InterviewSlotRepository slotRepository;
    private final BookingRepository bookingRepository;

    // capacity per slot; adjustable if you want more concurrent bookings per slot.
    private final int slotCapacity = 1;

    public BookingService(InterviewSlotRepository slotRepository,
                          BookingRepository bookingRepository) {
        this.slotRepository = slotRepository;
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public Booking createBooking(Long slotId, String candidateName, String candidateEmail) {
        InterviewSlot slot = slotRepository.findWithLockingById(slotId)
                .orElseThrow(() -> new NotFoundException("Slot " + slotId + " not found"));

        validateWeeklyAndCapacity(slot);
        validateNotAlreadyBooked(candidateEmail);

        Booking booking = new Booking();
        booking.setSlot(slot);
        booking.setCandidateName(candidateName);
        booking.setCandidateEmail(candidateEmail);
        booking.setConfirmed(true);

        slot.setBookedCount(slot.getBookedCount() + 1);

        try {
            // JPA will flush and check version at commit for optimistic lock
            return bookingRepository.save(booking);
        } catch (OptimisticLockException ex) {
            throw new SlotFullyBookedException(slotId);
        }
    }

    @Transactional
    public Booking updateBookingSlot(Long bookingId, Long newSlotId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking " + bookingId + " not found"));

        InterviewSlot oldSlot = slotRepository.findWithLockingById(booking.getSlot().getId())
                .orElseThrow(() -> new NotFoundException("Slot " + booking.getSlot().getId() + " not found"));
        InterviewSlot newSlot = slotRepository.findWithLockingById(newSlotId)
                .orElseThrow(() -> new NotFoundException("Slot " + newSlotId + " not found"));

        // decrement old
        if (oldSlot.getBookedCount() > 0) {
            oldSlot.setBookedCount(oldSlot.getBookedCount() - 1);
        }

        validateWeeklyAndCapacity(newSlot);
        // When updating, we don't need to validate not already booked since we're updating the existing booking

        newSlot.setBookedCount(newSlot.getBookedCount() + 1);
        booking.setSlot(newSlot);

        try {
            return bookingRepository.save(booking);
        } catch (OptimisticLockException ex) {
            throw new SlotFullyBookedException(newSlotId);
        }
    }

    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking " + bookingId + " not found"));

        InterviewSlot slot = slotRepository.findWithLockingById(booking.getSlot().getId())
                .orElseThrow(() -> new NotFoundException("Slot " + booking.getSlot().getId() + " not found"));

        if (slot.getBookedCount() > 0) {
            slot.setBookedCount(slot.getBookedCount() - 1);
        }
        bookingRepository.delete(booking);
    }

    private void validateWeeklyAndCapacity(InterviewSlot slot) {
        Interviewer interviewer = slot.getInterviewer();

        if (slot.getBookedCount() >= slotCapacity) {
            throw new SlotFullyBookedException(slot.getId());
        }

        LocalDate slotDate = slot.getStartTime().toLocalDate();
        LocalDate startOfWeek = slotDate.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = startOfWeek.plusDays(6);

        LocalDateTime startDateTime = startOfWeek.atStartOfDay();
        LocalDateTime endDateTime = endOfWeek.atTime(23, 59, 59);

        long countForWeek = bookingRepository.countBySlot_Interviewer_IdAndSlot_StartTimeBetween(
                interviewer.getId(),
                startDateTime,
                endDateTime
        );

        if (countForWeek >= interviewer.getMaxWeeklyInterviews()) {
            throw new WeeklyLimitExceededException(interviewer.getId());
        }
    }

    private void validateNotAlreadyBooked(String candidateEmail) {
        LocalDateTime now = LocalDateTime.now();
        long activeBookings = bookingRepository.countByCandidateEmailAndSlot_StartTimeAfter(
                candidateEmail,
                now
        );
        if (activeBookings > 0) {
            throw new AlreadyBookedException(candidateEmail);
        }
    }
}


