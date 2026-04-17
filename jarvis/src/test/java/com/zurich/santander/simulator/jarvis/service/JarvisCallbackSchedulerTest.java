package com.zurich.santander.simulator.jarvis.service;

import com.zurich.santander.simulator.jarvis.config.JarvisProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zurich.santander.simulator.jarvis.model.DocumentProcessingStatus;
import com.zurich.santander.simulator.jarvis.model.DocumentRequest;
import com.zurich.santander.simulator.jarvis.repository.DocumentRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JarvisCallbackSchedulerTest {

    @Mock
    private DocumentRequestRepository repository;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private JarvisMetricsService metricsService;

    private JarvisProperties properties;
    private JarvisCallbackScheduler scheduler;

    @BeforeEach
    void setup() {
        properties = new JarvisProperties();
        properties.setMaxRetries(1);
        properties.setRetryBackoffMs(10);
        WebhookSignatureService webhookSignatureService = new WebhookSignatureService(properties);
        scheduler = new JarvisCallbackScheduler(repository, restTemplate, properties, metricsService, webhookSignatureService, createObjectMapper());
    }

    @Test
    void shouldMarkAsCompletedWhenCallbackSucceeds() {
        DocumentRequest request = baseRequest();
        when(repository.findByStatusInAndNextAttemptAtLessThanEqual(any(), any())).thenReturn(List.of(request));

        scheduler.processPendingRequests();

        assertEquals(DocumentProcessingStatus.COMPLETED, request.getStatus());
        verify(restTemplate).postForEntity(eq(request.getCallbackUrl()), argThat((HttpEntity<String> httpEntity) -> {
            String signature = httpEntity.getHeaders().getFirst(properties.getWebhookSignatureHeader());
            String timestamp = httpEntity.getHeaders().getFirst(properties.getWebhookTimestampHeader());
            String idempotency = httpEntity.getHeaders().getFirst(properties.getWebhookIdempotencyHeader());
            return signature != null && signature.startsWith("v1=")
                    && timestamp != null && !timestamp.isBlank()
                    && idempotency != null && idempotency.startsWith("jarvis-callback-");
        }), eq(String.class));
        verify(metricsService).incrementCallbackSent();
    }

    @Test
    void shouldSendWithoutSignatureWhenFeatureIsDisabled() {
        properties.setWebhookSignatureEnabled(false);
        WebhookSignatureService webhookSignatureService = new WebhookSignatureService(properties);
        scheduler = new JarvisCallbackScheduler(repository, restTemplate, properties, metricsService, webhookSignatureService, createObjectMapper());

        DocumentRequest request = baseRequest();
        when(repository.findByStatusInAndNextAttemptAtLessThanEqual(any(), any())).thenReturn(List.of(request));

        scheduler.processPendingRequests();

        verify(restTemplate).postForEntity(eq(request.getCallbackUrl()), argThat((HttpEntity<String> httpEntity) ->
                !httpEntity.getHeaders().containsKey(properties.getWebhookSignatureHeader())), eq(String.class));
    }

    @Test
    void shouldScheduleRetryOnFirstFailure() {
        DocumentRequest request = baseRequest();
        when(repository.findByStatusInAndNextAttemptAtLessThanEqual(any(), any())).thenReturn(List.of(request));
        doThrow(new RuntimeException("boom")).when(restTemplate).postForEntity(eq(request.getCallbackUrl()), any(), eq(String.class));

        scheduler.processPendingRequests();

        assertEquals(DocumentProcessingStatus.RETRY_PENDING, request.getStatus());
        assertEquals(1, request.getCallbackAttempts());
        verify(metricsService).incrementCallbackRetryScheduled();
        verify(metricsService, never()).incrementCallbackFailed();
    }

    @Test
    void shouldFailAfterRetryLimitExceeded() {
        DocumentRequest request = baseRequest();
        request.setCallbackAttempts(1);
        request.setStatus(DocumentProcessingStatus.RETRY_PENDING);
        when(repository.findByStatusInAndNextAttemptAtLessThanEqual(any(), any())).thenReturn(List.of(request));
        doThrow(new RuntimeException("boom")).when(restTemplate).postForEntity(eq(request.getCallbackUrl()), any(), eq(String.class));

        scheduler.processPendingRequests();

        assertEquals(DocumentProcessingStatus.FAILED, request.getStatus());
        verify(metricsService).incrementCallbackFailed();
    }

    private DocumentRequest baseRequest() {
        DocumentRequest request = new DocumentRequest();
        request.setRequestId("req-1");
        request.setDocumentCode("doc-1");
        request.setClaimId("SIN123");
        request.setDocumentType("RG");
        request.setCallbackUrl("http://localhost/callback");
        request.setStatus(DocumentProcessingStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now().minusSeconds(1));
        request.setNextAttemptAt(LocalDateTime.now());
        request.setCallbackAttempts(0);
        return request;
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }
}

