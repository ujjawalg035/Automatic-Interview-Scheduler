package com.vasitum.interviewscheduler.api;

import com.vasitum.interviewscheduler.api.dto.SlotPageResponse;
import com.vasitum.interviewscheduler.api.dto.SlotResponse;
import com.vasitum.interviewscheduler.application.exception.NotFoundException;
import com.vasitum.interviewscheduler.application.service.SlotGenerationService;
import com.vasitum.interviewscheduler.domain.model.InterviewSlot;
import com.vasitum.interviewscheduler.domain.repository.InterviewSlotRepository;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Validated
public class SlotController {

    private static final int SLOT_CAPACITY = 1;

    private final SlotGenerationService slotGenerationService;
    private final InterviewSlotRepository slotRepository;

    public SlotController(SlotGenerationService slotGenerationService,
                          InterviewSlotRepository slotRepository) {
        this.slotGenerationService = slotGenerationService;
        this.slotRepository = slotRepository;
    }

    @PostMapping("/interviewers/{interviewerId}/generate-slots")
    @ResponseStatus(HttpStatus.OK)
    public int generateSlots(@PathVariable Long interviewerId,
                             @RequestParam(required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                             @RequestParam(required = false)
                             @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.now();
        LocalDate end = to != null ? to : start.plusDays(14);
        return slotGenerationService.generateSlotsForInterviewer(interviewerId, start, end);
    }

    @GetMapping("/slots")
    public SlotPageResponse listSlots(
            @RequestParam(required = false) Long interviewerId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") Long cursor,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit,
            @RequestParam(defaultValue = "false") boolean hideFull
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = from != null ? from : now;
        LocalDateTime end = to != null ? to : now.plusDays(14);

        Long effectiveCursor = cursor != null && cursor > 0 ? cursor : 0L;

        List<InterviewSlot> slots;
        if (interviewerId != null) {
            // simple filter by interviewer via stream for brevity
            slots = slotRepository.findUpcomingSlotsAfterCursor(start, end, effectiveCursor, PageRequest.of(0, limit))
                    .stream()
                    .filter(s -> s.getInterviewer().getId().equals(interviewerId))
                    .toList();
        } else {
            slots = slotRepository.findUpcomingSlotsAfterCursor(start, end, effectiveCursor, PageRequest.of(0, limit));
        }

        List<SlotResponse> items = slots.stream()
                .map(slot -> new SlotResponse(
                        slot.getId(),
                        slot.getInterviewer().getId(),
                        slot.getStartTime(),
                        slot.getEndTime(),
                        Math.max(0, SLOT_CAPACITY - slot.getBookedCount())
                ))
                .filter(sr -> !hideFull || sr.availableCapacity() > 0)
                .toList();

        Long nextCursor = items.isEmpty() ? effectiveCursor :
                items.get(items.size() - 1).slotId();

        boolean hasMore = !items.isEmpty() &&
                slotRepository.count() > nextCursor; // simple heuristic

        return new SlotPageResponse(items, nextCursor, hasMore);
    }

    @GetMapping("/slots/{slotId}")
    public SlotResponse getSlot(@PathVariable Long slotId) {
        InterviewSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new NotFoundException("Slot " + slotId + " not found"));

        return new SlotResponse(
                slot.getId(),
                slot.getInterviewer().getId(),
                slot.getStartTime(),
                slot.getEndTime(),
                Math.max(0, 1 - slot.getBookedCount())
        );
    }
}


