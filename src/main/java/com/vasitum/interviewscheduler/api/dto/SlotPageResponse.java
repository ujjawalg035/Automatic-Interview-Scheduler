package com.vasitum.interviewscheduler.api.dto;

import java.util.List;

public record SlotPageResponse(
        List<SlotResponse> items,
        Long nextCursor,
        boolean hasMore
) {
}


