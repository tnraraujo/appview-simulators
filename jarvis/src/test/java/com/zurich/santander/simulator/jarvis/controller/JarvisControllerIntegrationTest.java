package com.zurich.santander.simulator.jarvis.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.task.scheduling.enabled=false",
        "jarvis.simulator.api-key=test-key",
        "jarvis.simulator.max-payload-bytes=1024",
        "jarvis.simulator.rate-limit-per-minute=50"
})
@AutoConfigureMockMvc
class JarvisControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Deve aceitar payload minimo documentado e retornar 202")
    void shouldAcceptMinimalPayload() throws Exception {
        mockMvc.perform(post("/api/jarvis/cognitive-services/v1/documents-reading")
                        .header("Ocp-Apim-Subscription-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(minimalPayload(base64("pdf-content"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    @DisplayName("Deve aceitar callbackUrl legado sem exigir uso")
    void shouldAcceptLegacyCallbackUrl() throws Exception {
        mockMvc.perform(post("/api/jarvis/cognitive-services/v1/documents-reading")
                        .header("Ocp-Apim-Subscription-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadWithLegacyCallback(base64("pdf-content"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    @DisplayName("Deve retornar 401 para API key invalida")
    void shouldReturnUnauthorizedForInvalidApiKey() throws Exception {
        mockMvc.perform(post("/api/jarvis/cognitive-services/v1/documents-reading")
                        .header("Ocp-Apim-Subscription-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(minimalPayload(base64("pdf-content"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Deve retornar 400 para base64 invalido")
    void shouldReturnBadRequestForInvalidBase64() throws Exception {
        mockMvc.perform(post("/api/jarvis/cognitive-services/v1/documents-reading")
                        .header("Ocp-Apim-Subscription-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(minimalPayload("%%%invalid%%%")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    @DisplayName("Deve retornar 413 para payload acima do limite")
    void shouldReturnPayloadTooLarge() throws Exception {
        byte[] content = new byte[1500];
        String oversized = Base64.getEncoder().encodeToString(content);

        mockMvc.perform(post("/api/jarvis/cognitive-services/v1/documents-reading")
                        .header("Ocp-Apim-Subscription-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(minimalPayload(oversized)))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("PAYLOAD_TOO_LARGE"));
    }

    private String minimalPayload(String base64) {
        return """
                {
                  "documentCode": "alfresco-document-id",
                  "claimId": "SIN123456",
                  "documentBase64": "%s"
                }
                """.formatted(base64);
    }

    private String payloadWithLegacyCallback(String base64) {
        return """
                {
                  "documentCode": "alfresco-document-id",
                  "claimId": "SIN123456",
                  "callbackUrl": "http://localhost:9999/api/jarvis/callback",
                  "documentBase64": "%s",
                  "documentType": "RG"
                }
                """.formatted(base64);
    }

    private String base64(String content) {
        return Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
    }
}

