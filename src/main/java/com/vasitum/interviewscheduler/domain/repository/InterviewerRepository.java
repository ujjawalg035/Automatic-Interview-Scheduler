package com.vasitum.interviewscheduler.domain.repository;

import com.vasitum.interviewscheduler.domain.model.Interviewer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewerRepository extends JpaRepository<Interviewer, Long> {
}


