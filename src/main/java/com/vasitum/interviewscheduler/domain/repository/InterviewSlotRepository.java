package com.vasitum.interviewscheduler.domain.repository;

import com.vasitum.interviewscheduler.domain.model.InterviewSlot;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InterviewSlotRepository extends JpaRepository<InterviewSlot, Long> {

    List<InterviewSlot> findByInterviewerIdAndStartTimeBetweenOrderByStartTimeAsc(
            Long interviewerId,
            LocalDateTime from,
            LocalDateTime to
    );

    @Query("select s from InterviewSlot s where s.startTime >= :from and s.startTime <= :to and s.id > :cursorId order by s.id asc")
    List<InterviewSlot> findUpcomingSlotsAfterCursor(LocalDateTime from, LocalDateTime to, Long cursorId, Pageable pageable);

    @Lock(LockModeType.OPTIMISTIC)
    Optional<InterviewSlot> findWithLockingById(Long id);
}


