package com.zurich.santander.simulator.jarvis.service;

import com.zurich.santander.simulator.jarvis.config.JarvisProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookSignatureServiceTest {

    @Test
    void shouldCreateDeterministicSignature() {
        JarvisProperties properties = new JarvisProperties();
        properties.setWebhookSecret("test-secret");
        WebhookSignatureService service = new WebhookSignatureService(properties);

        String body = "{\"a\":1}";
        String signature = service.sign("POST", "/api/jarvis/callback", "1700000000", body);
        String sameSignature = service.sign("POST", "/api/jarvis/callback", "1700000000", body);
        String differentTimestampSignature = service.sign("POST", "/api/jarvis/callback", "1700000001", body);

        assertTrue(signature.startsWith("v1="));
        assertEquals(signature, sameSignature);
        assertNotEquals(signature, differentTimestampSignature);
    }
}

