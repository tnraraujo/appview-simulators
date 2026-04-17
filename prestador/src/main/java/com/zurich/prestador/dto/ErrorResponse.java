package com.zurich.prestador.dto;

import java.util.List;

public record ErrorResponse(
        String timestamp,
        int status,
        String error,
        String message,
        List<FieldErrorDetail> errors
) {
    public record FieldErrorDetail(
            String field,
            Object rejectedValue,
            String message
    ) {}
}
