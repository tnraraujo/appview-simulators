package com.zurich.santander.simulator.jarvis.service;

import com.zurich.santander.simulator.jarvis.config.JarvisProperties;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyValidationService {

    private final JarvisProperties properties;

    public ApiKeyValidationService(JarvisProperties properties) {
        this.properties = properties;
    }

    public boolean isValid(String apiKey) {
        return apiKey != null && !apiKey.isBlank() && apiKey.equals(properties.getApiKey());
    }
}

