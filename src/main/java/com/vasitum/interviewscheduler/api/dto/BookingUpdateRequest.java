package com.vasitum.interviewscheduler.api.dto;

import jakarta.validation.constraints.NotNull;

public record BookingUpdateRequest(
        @NotNull Long newSlotId
) {
}


