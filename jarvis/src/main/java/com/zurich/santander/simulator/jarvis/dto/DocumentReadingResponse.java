package com.zurich.santander.simulator.jarvis.dto;

import java.time.OffsetDateTime;

public record DocumentReadingResponse(
        String requestId,
        String status,
        OffsetDateTime estimatedCompletionTime
) {
}
