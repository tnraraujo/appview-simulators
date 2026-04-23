package com.zurich.santander.simulator.jarvis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DocumentReadingRequest(
        @NotBlank String documentCode,
        Integer documentIndex,
        String sequential,
        String channel,
        String channelId,
        String holderName,
        String origin,
        @NotBlank String claimId,
        @Pattern(regexp = "https?://.+", message = "callbackUrl must be a valid http/https URL") String callbackUrl,
        @NotBlank String documentBase64,
        String documentType
) {
}
