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
        "jarvis.simulator.fixed-callback-url="
})
@AutoConfigureMockMvc
class JarvisConfigurationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Deve retornar 500 quando fixed-callback-url nao estiver configurada")
    void shouldReturnInternalErrorWhenFixedCallbackUrlIsMissing() throws Exception {
        mockMvc.perform(post("/api/jarvis/cognitive-services/v1/documents-reading")
                        .header("Ocp-Apim-Subscription-Key", "test-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(minimalPayload(base64("pdf-content"))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("CONFIGURATION_ERROR"));
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

    private String base64(String content) {
        return Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
    }
}

