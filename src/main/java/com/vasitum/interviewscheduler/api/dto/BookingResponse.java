package com.vasitum.interviewscheduler.api.dto;

import java.time.LocalDateTime;

public record BookingResponse(
        Long bookingId,
        Long slotId,
        Long interviewerId,
        String candidateName,
        String candidateEmail,
        LocalDateTime startTime,
        LocalDateTime endTime,
        boolean confirmed
) {
}

