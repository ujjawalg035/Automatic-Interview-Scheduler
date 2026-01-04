package com.vasitum.interviewscheduler.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InterviewerDto(
        Long id,
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotNull @Min(1) Integer maxWeeklyInterviews
) {
}


