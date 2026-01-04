package com.vasitum.interviewscheduler.api;

import com.vasitum.interviewscheduler.api.dto.BookingRequest;
import com.vasitum.interviewscheduler.api.dto.BookingResponse;
import com.vasitum.interviewscheduler.api.dto.BookingUpdateRequest;
import com.vasitum.interviewscheduler.application.service.BookingService;
import com.vasitum.interviewscheduler.domain.model.Booking;
import com.vasitum.interviewscheduler.domain.repository.BookingRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final BookingRepository bookingRepository;

    public BookingController(BookingService bookingService, BookingRepository bookingRepository) {
        this.bookingService = bookingService;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/by-candidate")
    @Transactional(readOnly = true)
    public List<BookingResponse> getByCandidate(@RequestParam String candidateEmail) {
        return bookingRepository.findByCandidateEmailOrderBySlot_StartTimeAsc(candidateEmail)
                .stream()
                .map(booking -> new BookingResponse(
                        booking.getId(),
                        booking.getSlot().getId(),
                        booking.getSlot().getInterviewer().getId(),
                        booking.getCandidateName(),
                        booking.getCandidateEmail(),
                        booking.getSlot().getStartTime(),
                        booking.getSlot().getEndTime(),
                        booking.isConfirmed()
                ))
                .toList();
    }

    @GetMapping("/by-interviewer/{interviewerId}")
    @Transactional(readOnly = true)
    public List<BookingResponse> getByInterviewer(@PathVariable Long interviewerId) {
        return bookingRepository.findBySlot_Interviewer_IdOrderBySlot_StartTimeAsc(interviewerId)
                .stream()
                .map(booking -> new BookingResponse(
                        booking.getId(),
                        booking.getSlot().getId(),
                        booking.getSlot().getInterviewer().getId(),
                        booking.getCandidateName(),
                        booking.getCandidateEmail(),
                        booking.getSlot().getStartTime(),
                        booking.getSlot().getEndTime(),
                        booking.isConfirmed()
                ))
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Booking create(@Valid @RequestBody BookingRequest request) {
        return bookingService.createBooking(
                request.slotId(),
                request.candidateName(),
                request.candidateEmail()
        );
    }

    @PutMapping("/{bookingId}")
    public Booking updateSlot(@PathVariable Long bookingId,
                              @Valid @RequestBody BookingUpdateRequest request) {
        return bookingService.updateBookingSlot(bookingId, request.newSlotId());
    }

    @DeleteMapping("/{bookingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long bookingId) {
        bookingService.cancelBooking(bookingId);
    }
}


