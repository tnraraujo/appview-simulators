package com.zurich.santander.simulator.pega_bpm_simulator.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PegaBpmControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${simulator.pega.auth.username}")
    private String username;

    @Value("${simulator.pega.auth.password}")
    private String password;

    @Test
    void deveCriarCasoQuandoPayloadValido() throws Exception {
        String numeroSinistro = "SIN" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        String payload = objectMapper.writeValueAsString(payloadValido(numeroSinistro, 85));

        mockMvc.perform(post("/api/pega/v1/cases/sinistro")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.caseId").exists())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.workflowStarted").value(true));
    }

    @Test
    void deveRetornar400QuandoOcrScoresAusente() throws Exception {
        String numeroSinistro = "SIN" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        Map<String, Object> payload = payloadValido(numeroSinistro, 85);
        payload.remove("ocrScores");

        mockMvc.perform(post("/api/pega/v1/cases/sinistro")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.ocrScores").exists());
    }

    @Test
    void deveRetornar400QuandoLegibilidadeMenorQue60() throws Exception {
        String numeroSinistro = "SIN" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();

        mockMvc.perform(post("/api/pega/v1/cases/sinistro")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payloadValido(numeroSinistro, 59))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("RN-020")));
    }

    @Test
    void deveRetornar409ComMesmoCaseIdQuandoSinistroDuplicado() throws Exception {
        String numeroSinistro = "SIN" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        String payload = objectMapper.writeValueAsString(payloadValido(numeroSinistro, 85));

        String firstResponse = mockMvc.perform(post("/api/pega/v1/cases/sinistro")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode createdBody = objectMapper.readTree(firstResponse);
        String firstCaseId = createdBody.get("caseId").asText();

        mockMvc.perform(post("/api/pega/v1/cases/sinistro")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.caseId").value(firstCaseId));
    }

    private String authorizationHeader() {
        String token = Base64.getEncoder()
                .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        return "Basic " + token;
    }

    private Map<String, Object> payloadValido(String numeroSinistro, int legibilidade) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("numeroSinistro", numeroSinistro);
        payload.put("tipoDocumento", "LAUDO_MEDICO");
        payload.put("canal", "WPC");

        Map<String, Object> lossInfo = new HashMap<>();
        lossInfo.put("lossNumber", "LOSS123");
        lossInfo.put("lossSequence", "01");
        lossInfo.put("lossYear", 2026);
        lossInfo.put("branch", "AUTO");
        payload.put("lossInfo", lossInfo);

        Map<String, Object> documento = new HashMap<>();
        documento.put("documentId", "workspace://SpacesStore/abc-123-def");
        documento.put("documentPath", "/2026/01/" + numeroSinistro + "/laudo.pdf");
        payload.put("documentoAlfresco", documento);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("apolice", "POL-2026-001");
        metadata.put("ramo", "AUTO");
        metadata.put("certificado", "CERT-123");
        payload.put("metadata", metadata);

        Map<String, Object> ocrScores = new HashMap<>();
        ocrScores.put("legibilidade", legibilidade);
        ocrScores.put("acuracidade", 92);
        ocrScores.put("matchDocumento", 78);
        payload.put("ocrScores", ocrScores);

        return payload;
    }
}

