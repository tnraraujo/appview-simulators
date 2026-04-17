package com.zurich.santander.simulator.jarvis.dto;

import java.util.Map;

public record JarvisCallbackResult(
        int legibilidade,
        int acuracidade,
        int matchDocumento,
        String extractedText,
        double confidence,
        String documentType,
        Map<String, Object> fields
) {
}

