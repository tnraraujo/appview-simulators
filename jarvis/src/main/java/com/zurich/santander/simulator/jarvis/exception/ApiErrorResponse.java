package com.zurich.santander.simulator.jarvis.exception;

import java.time.OffsetDateTime;

public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String code,
        String message
) {
}

