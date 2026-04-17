package com.zurich.santander.simulator.jarvis.service;

import com.zurich.santander.simulator.jarvis.config.JarvisProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitServiceTest {

    @Test
    void shouldBlockWhenRateLimitExceeded() {
        JarvisProperties properties = new JarvisProperties();
        properties.setRateLimitPerMinute(2);

        RateLimitService service = new RateLimitService(properties);

        assertTrue(service.isAllowed("key"));
        assertTrue(service.isAllowed("key"));
        assertFalse(service.isAllowed("key"));
    }
}

