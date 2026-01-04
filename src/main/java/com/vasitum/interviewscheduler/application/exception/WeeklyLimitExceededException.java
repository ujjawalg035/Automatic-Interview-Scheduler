package com.vasitum.interviewscheduler.application.exception;

public class WeeklyLimitExceededException extends DomainException {

    public WeeklyLimitExceededException(Long interviewerId) {
        super("WEEKLY_LIMIT_EXCEEDED",
                "Weekly interview limit exceeded for interviewer " + interviewerId);
    }
}


