package com.vasitum.interviewscheduler.domain.repository;

import com.vasitum.interviewscheduler.domain.model.WeeklyAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WeeklyAvailabilityRepository extends JpaRepository<WeeklyAvailability, Long> {

    List<WeeklyAvailability> findByInterviewerId(Long interviewerId);
}


