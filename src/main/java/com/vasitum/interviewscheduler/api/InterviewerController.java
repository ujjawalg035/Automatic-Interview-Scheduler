package com.vasitum.interviewscheduler.api;

import com.vasitum.interviewscheduler.api.dto.InterviewerDto;
import com.vasitum.interviewscheduler.application.exception.NotFoundException;
import com.vasitum.interviewscheduler.domain.model.Interviewer;
import com.vasitum.interviewscheduler.domain.repository.InterviewerRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/interviewers")
@Validated
public class InterviewerController {

    private final InterviewerRepository interviewerRepository;

    public InterviewerController(InterviewerRepository interviewerRepository) {
        this.interviewerRepository = interviewerRepository;
    }

    @GetMapping("/{id}")
    public InterviewerDto get(@PathVariable Long id) {
        Interviewer interviewer = interviewerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Interviewer " + id + " not found"));
        return new InterviewerDto(interviewer.getId(), interviewer.getName(), interviewer.getEmail(), interviewer.getMaxWeeklyInterviews());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InterviewerDto create(@Valid @RequestBody InterviewerDto dto) {
        Interviewer interviewer = new Interviewer();
        interviewer.setName(dto.name());
        interviewer.setEmail(dto.email());
        interviewer.setMaxWeeklyInterviews(dto.maxWeeklyInterviews());
        Interviewer saved = interviewerRepository.save(interviewer);
        return new InterviewerDto(saved.getId(), saved.getName(), saved.getEmail(), saved.getMaxWeeklyInterviews());
    }

    @PatchMapping("/{id}/max-weekly-interviews")
    public InterviewerDto updateMaxWeekly(@PathVariable Long id,
                                          @RequestParam @Min(1) Integer maxWeeklyInterviews) {
        Interviewer interviewer = interviewerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Interviewer " + id + " not found"));
        interviewer.setMaxWeeklyInterviews(maxWeeklyInterviews);
        Interviewer saved = interviewerRepository.save(interviewer);
        return new InterviewerDto(saved.getId(), saved.getName(), saved.getEmail(), saved.getMaxWeeklyInterviews());
    }
}


