package com.zurich.santander.simulator.jarvis.service;

import com.zurich.santander.simulator.jarvis.config.JarvisProperties;
import com.zurich.santander.simulator.jarvis.dto.DocumentReadingRequest;
import com.zurich.santander.simulator.jarvis.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class PayloadValidationService {

    private final JarvisProperties properties;

    public PayloadValidationService(JarvisProperties properties) {
        this.properties = properties;
    }

    public void validate(DocumentReadingRequest request) {
        long decodedSize = estimateDecodedSize(request.documentBase64());
        if (decodedSize > properties.getMaxPayloadBytes()) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "PAYLOAD_TOO_LARGE", "Document exceeds max size limit");
        }
    }

    private long estimateDecodedSize(String base64) {
        try {
            return Base64.getDecoder().decode(base64).length;
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "documentBase64 is not valid Base64");
        }
    }
}

