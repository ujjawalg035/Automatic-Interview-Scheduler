package com.vasitum.interviewscheduler.domain.repository;

import com.vasitum.interviewscheduler.domain.model.Booking;
import com.vasitum.interviewscheduler.domain.model.InterviewSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    long countBySlot_Interviewer_IdAndSlot_StartTimeBetween(Long interviewerId,
                                                            LocalDateTime startOfWeek,
                                                            LocalDateTime endOfWeek);

    long countBySlot(InterviewSlot slot);

    long countByCandidateEmailAndSlot_StartTimeBetween(String candidateEmail,
                                                       LocalDateTime startDateTime,
                                                       LocalDateTime endDateTime);

    List<Booking> findByCandidateEmailOrderBySlot_StartTimeAsc(String candidateEmail);

    List<Booking> findBySlot_Interviewer_IdOrderBySlot_StartTimeAsc(Long interviewerId);

    long countByCandidateEmailAndSlot_StartTimeAfter(String candidateEmail, LocalDateTime dateTime);
}


