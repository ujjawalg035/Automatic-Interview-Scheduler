package com.vasitum.interviewscheduler.api;

import com.vasitum.interviewscheduler.api.dto.WeeklyAvailabilityRequest;
import com.vasitum.interviewscheduler.application.service.AvailabilityService;
import com.vasitum.interviewscheduler.domain.model.WeeklyAvailability;
import com.vasitum.interviewscheduler.domain.repository.WeeklyAvailabilityRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/interviewers/{interviewerId}/weekly-availability")
public class AvailabilityController {

    private final AvailabilityService availabilityService;
    private final WeeklyAvailabilityRepository availabilityRepository;

    public AvailabilityController(AvailabilityService availabilityService,
                                 WeeklyAvailabilityRepository availabilityRepository) {
        this.availabilityService = availabilityService;
        this.availabilityRepository = availabilityRepository;
    }

    @GetMapping
    public List<WeeklyAvailability> get(@PathVariable Long interviewerId) {
        return availabilityRepository.findByInterviewerId(interviewerId);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public List<WeeklyAvailability> replace(
            @PathVariable Long interviewerId,
            @Valid @RequestBody List<WeeklyAvailabilityRequest> requests
    ) {
        List<AvailabilityService.WeeklyAvailabilityInput> inputs = requests.stream()
                .map(r -> new AvailabilityService.WeeklyAvailabilityInput(
                        r.dayOfWeek(),
                        r.startTime(),
                        r.endTime(),
                        r.slotDurationMinutes()
                ))
                .toList();
        return availabilityService.replaceWeeklyAvailability(interviewerId, inputs);
    }
}


