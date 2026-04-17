package com.zurich.santander.simulator.jarvis.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record DocumentReadingRequest(
        @NotBlank String documentCode,
        @NotNull Integer documentIndex,
        @NotBlank String sequential,
        @NotBlank String channel,
        @NotBlank String channelId,
        @NotBlank String holderName,
        @NotBlank String origin,
        @NotBlank String claimId,
        @NotBlank @Pattern(regexp = "https?://.+", message = "callbackUrl must be a valid http/https URL") String callbackUrl,
        @NotBlank String documentBase64,
        String documentType
) {
}
