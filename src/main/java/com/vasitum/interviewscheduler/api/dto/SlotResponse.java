package com.vasitum.interviewscheduler.api.dto;

import java.time.LocalDateTime;

public record SlotResponse(
        Long slotId,
        Long interviewerId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        int availableCapacity
) {
}


