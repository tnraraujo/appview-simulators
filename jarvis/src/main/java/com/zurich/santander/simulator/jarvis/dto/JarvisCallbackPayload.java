package com.zurich.santander.simulator.jarvis.dto;

import java.time.OffsetDateTime;

public record JarvisCallbackPayload(
        String documentCode,
        String claimId,
        String status,
        JarvisCallbackResult result,
        OffsetDateTime processedAt,
        long processingDurationMs
) {
}
