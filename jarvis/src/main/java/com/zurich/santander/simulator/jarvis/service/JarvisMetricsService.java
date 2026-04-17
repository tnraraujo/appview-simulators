package com.zurich.santander.simulator.jarvis.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class JarvisMetricsService {

    private final MeterRegistry meterRegistry;

    public JarvisMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementRequestAccepted() {
        Counter.builder("jarvis_requests_total")
                .tag("result", "accepted")
                .register(meterRegistry)
                .increment();
    }

    public void incrementRequestRejected(String reason) {
        Counter.builder("jarvis_requests_total")
                .tag("result", "rejected")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    public void incrementCallbackSent() {
        Counter.builder("jarvis_callback_total")
                .tag("result", "success")
                .register(meterRegistry)
                .increment();
    }

    public void incrementCallbackFailed() {
        Counter.builder("jarvis_callback_total")
                .tag("result", "failed")
                .register(meterRegistry)
                .increment();
    }

    public void incrementCallbackRetryScheduled() {
        Counter.builder("jarvis_callback_retry_total")
                .register(meterRegistry)
                .increment();
    }

    public void recordProcessingDuration(long durationMs) {
        Timer.builder("jarvis_processing_duration")
                .publishPercentileHistogram()
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }
}

