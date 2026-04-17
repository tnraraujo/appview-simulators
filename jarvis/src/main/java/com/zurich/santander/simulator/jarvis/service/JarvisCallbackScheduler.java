package com.zurich.santander.simulator.jarvis.service;

import com.zurich.santander.simulator.jarvis.config.JarvisProperties;
import com.zurich.santander.simulator.jarvis.dto.JarvisCallbackPayload;
import com.zurich.santander.simulator.jarvis.dto.JarvisCallbackResult;
import com.zurich.santander.simulator.jarvis.model.DocumentProcessingStatus;
import com.zurich.santander.simulator.jarvis.model.DocumentRequest;
import com.zurich.santander.simulator.jarvis.repository.DocumentRequestRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JarvisCallbackScheduler {

    private final DocumentRequestRepository repository;
    private final RestTemplate restTemplate;
    private final JarvisProperties properties;
    private final JarvisMetricsService metricsService;
    private final WebhookSignatureService webhookSignatureService;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${jarvis.simulator.delay-ms:5000}")
    public void processPendingRequests() {
        LocalDateTime now = LocalDateTime.now();
        List<DocumentRequest> pendingRequests = repository.findByStatusInAndNextAttemptAtLessThanEqual(
                List.of(DocumentProcessingStatus.PENDING, DocumentProcessingStatus.RETRY_PENDING),
                now
        );

        for (DocumentRequest request : pendingRequests) {
            log.info("Processing requestId: {}", request.getRequestId());

            JarvisCallbackPayload payload = buildPayload(request);
            long duration = java.time.Duration.between(request.getCreatedAt(), LocalDateTime.now()).toMillis();

            try {
                if (request.getCallbackUrl() != null) {
                    HttpEntity<String> callbackRequest = buildCallbackRequest(request.getCallbackUrl(), request.getRequestId(), payload);
                    restTemplate.postForEntity(request.getCallbackUrl(), callbackRequest, String.class);
                    log.info("Callback sent to {}", request.getCallbackUrl());
                }

                request.setStatus(DocumentProcessingStatus.COMPLETED);
                request.setProcessedAt(LocalDateTime.now());
                request.setLastError(null);
                repository.save(request);
                metricsService.incrementCallbackSent();
                metricsService.recordProcessingDuration(duration);
            } catch (Exception e) {
                log.error("Failed to send callback for requestId: {}", request.getRequestId(), e);
                handleRetryOrFail(request, e);
                repository.save(request);
            }
        }
    }

    private HttpEntity<String> buildCallbackRequest(String callbackUrl, String requestId, JarvisCallbackPayload payload) {
        String callbackId = requestId != null ? requestId : UUID.randomUUID().toString();
        String body = toJson(payload);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(properties.getWebhookCorrelationHeader(), callbackId);
        headers.set(properties.getWebhookIdempotencyHeader(), "jarvis-callback-" + callbackId);

        if (!properties.isWebhookSignatureEnabled()) {
            return new HttpEntity<>(body, headers);
        }

        String timestamp = String.valueOf(OffsetDateTime.now(ZoneOffset.UTC).toEpochSecond());
        String path = resolvePath(callbackUrl);
        String signature = webhookSignatureService.sign("POST", path, timestamp, body);
        headers.set(properties.getWebhookTimestampHeader(), timestamp);
        headers.set(properties.getWebhookSignatureHeader(), signature);
        return new HttpEntity<>(body, headers);
    }

    private String resolvePath(String callbackUrl) {
        try {
            URI uri = URI.create(callbackUrl);
            return uri.getRawPath() != null ? uri.getRawPath() : "/";
        } catch (Exception ex) {
            return "/api/jarvis/callback";
        }
    }

    private String toJson(JarvisCallbackPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize callback payload", ex);
        }
    }

    private void handleRetryOrFail(DocumentRequest request, Exception e) {
        int attempts = request.getCallbackAttempts() + 1;
        request.setCallbackAttempts(attempts);
        request.setLastError(e.getClass().getSimpleName() + ": " + e.getMessage());

        if (attempts <= properties.getMaxRetries()) {
            request.setStatus(DocumentProcessingStatus.RETRY_PENDING);
            request.setNextAttemptAt(LocalDateTime.now().plusNanos(properties.getRetryBackoffMs() * 1_000_000));
            metricsService.incrementCallbackRetryScheduled();
            return;
        }

        request.setStatus(DocumentProcessingStatus.FAILED);
        request.setProcessedAt(LocalDateTime.now());
        metricsService.incrementCallbackFailed();
    }

    private JarvisCallbackPayload buildPayload(DocumentRequest request) {
        Map<String, Object> extractedData = generateMockData(request.getDocumentType());
        int legibilidade = 85;
        int acuracidade = 92;
        int matchDocumento = 78;

        JarvisCallbackResult result = new JarvisCallbackResult(
                legibilidade,
                acuracidade,
                matchDocumento,
                "Texto extraido do documento...",
                0.92,
                request.getDocumentType() != null ? request.getDocumentType() : "UNKNOWN",
                extractedData
        );

        long durationMs = java.time.Duration.between(request.getCreatedAt(), LocalDateTime.now()).toMillis();
        return new JarvisCallbackPayload(
                request.getDocumentCode(),
                request.getClaimId(),
                "COMPLETED",
                result,
                OffsetDateTime.now(ZoneOffset.UTC),
                durationMs
        );
    }

    private Map<String, Object> generateMockData(String documentType) {
        String type = documentType != null ? documentType.toUpperCase() : "UNKNOWN";
        
        switch (type) {
            case "CNH":
                return Map.of(
                        "nome", "Usuario Simulador CNH",
                        "cpf", "111.222.333-44",
                        "numeroRegistro", "12345678900",
                        "categoria", "AB",
                        "dataValidade", "2030-12-31"
                );
            case "RG":
                return Map.of(
                        "nome", "Usuario Simulador RG",
                        "cpf", "111.222.333-44",
                        "rg", "12.345.678-9",
                        "dataNascimento", "1990-01-01",
                        "nomeMae", "Maria Simuladora"
                );
            case "COMPROVANTE_RESIDENCIA":
                return Map.of(
                        "nome", "Usuario Simulador Residencia",
                        "cep", "01001-000",
                        "logradouro", "Praca da Se",
                        "numero", "1",
                        "cidade", "Sao Paulo",
                        "uf", "SP"
                );
            default:
                return Map.of(
                        "nome", "Usuario Simulado Padrao",
                        "cpf", "123.456.789-00",
                        "dataNascimento", "1990-01-01"
                );
        }
    }
}
