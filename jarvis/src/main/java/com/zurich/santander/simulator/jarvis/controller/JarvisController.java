package com.zurich.santander.simulator.jarvis.controller;

import com.zurich.santander.simulator.jarvis.config.JarvisProperties;
import com.zurich.santander.simulator.jarvis.dto.DocumentReadingRequest;
import com.zurich.santander.simulator.jarvis.dto.DocumentReadingResponse;
import com.zurich.santander.simulator.jarvis.exception.ApiException;
import com.zurich.santander.simulator.jarvis.model.DocumentProcessingStatus;
import com.zurich.santander.simulator.jarvis.model.DocumentRequest;
import com.zurich.santander.simulator.jarvis.repository.DocumentRequestRepository;
import com.zurich.santander.simulator.jarvis.service.ApiKeyValidationService;
import com.zurich.santander.simulator.jarvis.service.JarvisMetricsService;
import com.zurich.santander.simulator.jarvis.service.PayloadValidationService;
import com.zurich.santander.simulator.jarvis.service.RateLimitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@RestController
@RequestMapping("/api/jarvis/cognitive-services/v1")
@RequiredArgsConstructor
public class JarvisController {

    private final DocumentRequestRepository repository;
    private final JarvisProperties properties;
    private final ApiKeyValidationService apiKeyValidationService;
    private final RateLimitService rateLimitService;
    private final PayloadValidationService payloadValidationService;
    private final JarvisMetricsService metricsService;

    @PostMapping("/documents-reading")
    public ResponseEntity<DocumentReadingResponse> receiveDocumentRequest(
            @RequestHeader(value = "X-Simulate-Error", required = false) Integer simulateError,
            @RequestHeader(value = "Ocp-Apim-Subscription-Key", required = false) String subscriptionKey,
            @Valid @RequestBody DocumentReadingRequest payload) {

        if (simulateError != null) {
            metricsService.incrementRequestRejected("simulated_" + simulateError);
            throw new ApiException(HttpStatus.valueOf(simulateError), "SIMULATED_ERROR", "Error simulated by X-Simulate-Error");
        }

        if (!apiKeyValidationService.isValid(subscriptionKey)) {
            metricsService.incrementRequestRejected("invalid_api_key");
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid Ocp-Apim-Subscription-Key");
        }

        if (!rateLimitService.isAllowed(subscriptionKey)) {
            metricsService.incrementRequestRejected("rate_limited");
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "Rate limit exceeded");
        }

        payloadValidationService.validate(payload);

        String requestId = UUID.randomUUID().toString();

        DocumentRequest request = new DocumentRequest();
        request.setRequestId(requestId);
        request.setDocumentCode(payload.documentCode());
        request.setClaimId(payload.claimId());
        request.setDocumentType(payload.documentType());
        request.setCallbackUrl(payload.callbackUrl());
        request.setStatus(DocumentProcessingStatus.PENDING);
        request.setCallbackAttempts(0);
        request.setNextAttemptAt(LocalDateTime.now());
        repository.save(request);

        metricsService.incrementRequestAccepted();

        OffsetDateTime eta = OffsetDateTime.now(ZoneOffset.UTC).plusSeconds(Math.max(1, properties.getDelayMs() / 1000));

        return ResponseEntity.accepted().body(new DocumentReadingResponse(requestId, "PROCESSING", eta));
    }
}
