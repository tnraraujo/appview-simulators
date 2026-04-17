package com.zurich.santander.simulator.jarvis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jarvis.simulator")
public class JarvisProperties {

    private long delayMs = 5000;
    private int maxRetries = 2;
    private long retryBackoffMs = 5000;
    private long maxPayloadBytes = 10 * 1024 * 1024;
    private String apiKey = "jarvis-dev-key";
    private int rateLimitPerMinute = 50;
    private boolean webhookSignatureEnabled = true;
    private String webhookSecret = "jarvis-webhook-secret";
    private long webhookReplayWindowSeconds = 300;
    private String webhookSignatureHeader = "X-Jarvis-Signature";
    private String webhookTimestampHeader = "X-Jarvis-Timestamp";
    private String webhookCorrelationHeader = "X-Correlation-Id";
    private String webhookIdempotencyHeader = "Idempotency-Key";

    public long getDelayMs() {
        return delayMs;
    }

    public void setDelayMs(long delayMs) {
        this.delayMs = delayMs;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getRetryBackoffMs() {
        return retryBackoffMs;
    }

    public void setRetryBackoffMs(long retryBackoffMs) {
        this.retryBackoffMs = retryBackoffMs;
    }

    public long getMaxPayloadBytes() {
        return maxPayloadBytes;
    }

    public void setMaxPayloadBytes(long maxPayloadBytes) {
        this.maxPayloadBytes = maxPayloadBytes;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    public void setRateLimitPerMinute(int rateLimitPerMinute) {
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public boolean isWebhookSignatureEnabled() {
        return webhookSignatureEnabled;
    }

    public void setWebhookSignatureEnabled(boolean webhookSignatureEnabled) {
        this.webhookSignatureEnabled = webhookSignatureEnabled;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public long getWebhookReplayWindowSeconds() {
        return webhookReplayWindowSeconds;
    }

    public void setWebhookReplayWindowSeconds(long webhookReplayWindowSeconds) {
        this.webhookReplayWindowSeconds = webhookReplayWindowSeconds;
    }

    public String getWebhookSignatureHeader() {
        return webhookSignatureHeader;
    }

    public void setWebhookSignatureHeader(String webhookSignatureHeader) {
        this.webhookSignatureHeader = webhookSignatureHeader;
    }

    public String getWebhookTimestampHeader() {
        return webhookTimestampHeader;
    }

    public void setWebhookTimestampHeader(String webhookTimestampHeader) {
        this.webhookTimestampHeader = webhookTimestampHeader;
    }

    public String getWebhookCorrelationHeader() {
        return webhookCorrelationHeader;
    }

    public void setWebhookCorrelationHeader(String webhookCorrelationHeader) {
        this.webhookCorrelationHeader = webhookCorrelationHeader;
    }

    public String getWebhookIdempotencyHeader() {
        return webhookIdempotencyHeader;
    }

    public void setWebhookIdempotencyHeader(String webhookIdempotencyHeader) {
        this.webhookIdempotencyHeader = webhookIdempotencyHeader;
    }
}

