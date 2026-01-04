package com.vasitum.interviewscheduler.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookingRequest(
        @NotNull Long slotId,
        @NotBlank String candidateName,
        @NotBlank @Email String candidateEmail
) {
}


