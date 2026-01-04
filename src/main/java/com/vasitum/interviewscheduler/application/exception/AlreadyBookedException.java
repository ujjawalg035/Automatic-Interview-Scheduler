package com.vasitum.interviewscheduler.application.exception;

public class AlreadyBookedException extends DomainException {
    public AlreadyBookedException(String candidateEmail) {
        super("ALREADY_BOOKED", "Candidate " + candidateEmail + " already has an active booking. Please cancel the existing booking or update it.");
    }
}

