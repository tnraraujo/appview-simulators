package com.zurich.santander.simulator.jarvis.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JarvisMetricsServiceTest {

    @Test
    void shouldCountIgnoredLegacyCallbackUrlRequests() {
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        JarvisMetricsService service = new JarvisMetricsService(meterRegistry);

        service.incrementLegacyCallbackUrlIgnored();
        service.incrementLegacyCallbackUrlIgnored();

        assertEquals(2.0, meterRegistry.get("jarvis_legacy_callback_url_ignored_total").counter().count());
    }
}

