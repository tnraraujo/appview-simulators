package com.zurich.prestador;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "prestador.sim.default-delay-ms=10",
        "prestador.sim.default-mode=success"
})
@AutoConfigureMockMvc
class PericiaControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Success: retorna payload esperado e ecoa correlacao")
    void successShouldReturnExpectedResponse() throws Exception {
        mockMvc.perform(post("/api/prestador/v1/pericias")
                        .with(httpBasic("appview", "appview-secret-123"))
                        .header("X-Request-ID", "req-123")
                        .header("X-Correlation-ID", "corr-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-ID", "req-123"))
                .andExpect(header().string("X-Correlation-ID", "corr-123"))
                .andExpect(jsonPath("$.periciaId").value(org.hamcrest.Matchers.startsWith("PER-")))
                .andExpect(jsonPath("$.status").value("AGENDADA"))
                .andExpect(jsonPath("$.prestador.nome").value("Auto Vistoria SP"))
                .andExpect(jsonPath("$.dataAgendamento").value("2026-02-10T14:00:00Z"));
    }

    @Test
    @DisplayName("Auth: sem Basic Auth retorna 401")
    void missingTokenShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/prestador/v1/pericias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Timeout: mode=timeout demora pelo menos 12s")
    void timeoutModeShouldDelayResponse() throws Exception {
        long start = System.currentTimeMillis();

        mockMvc.perform(post("/api/prestador/v1/pericias")
                        .with(httpBasic("appview", "appview-secret-123"))
                        .header("X-Sim-Mode", "timeout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isOk());

        long elapsed = System.currentTimeMillis() - start;
        assertThat(elapsed).isGreaterThanOrEqualTo(12_000);
    }

    @Test
    @DisplayName("Fail: mode=fail retorna 500 com body esperado")
    void failModeShouldReturn500() throws Exception {
        mockMvc.perform(post("/api/prestador/v1/pericias")
                        .with(httpBasic("appview", "appview-secret-123"))
                        .header("X-Sim-Mode", "fail")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("Simulated failure"));
    }

    @Test
    @DisplayName("Flaky: primeira chamada falha e segunda chamada com mesma chave tem sucesso")
    void flakyModeShouldFailThenSucceed() throws Exception {
        mockMvc.perform(post("/api/prestador/v1/pericias")
                        .with(httpBasic("appview", "appview-secret-123"))
                        .header("X-Sim-Mode", "flaky")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isServiceUnavailable());

        MvcResult secondResult = mockMvc.perform(post("/api/prestador/v1/pericias")
                        .with(httpBasic("appview", "appview-secret-123"))
                        .header("X-Sim-Mode", "flaky")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode secondJson = objectMapper.readTree(secondResult.getResponse().getContentAsString());
        assertThat(secondJson.get("periciaId").asText()).startsWith("PER-");
    }

    @Test
    @DisplayName("Idempotencia: duas chamadas success com mesma chave retornam o mesmo periciaId")
    void shouldReturnSamePericiaIdForSameKeyInSuccessMode() throws Exception {
        MvcResult first = mockMvc.perform(post("/api/prestador/v1/pericias")
                        .with(httpBasic("appview", "appview-secret-123"))
                        .header("X-Sim-Mode", "success")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult second = mockMvc.perform(post("/api/prestador/v1/pericias")
                        .with(httpBasic("appview", "appview-secret-123"))
                        .header("X-Sim-Mode", "success")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validPayload()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode firstJson = objectMapper.readTree(first.getResponse().getContentAsString());
        JsonNode secondJson = objectMapper.readTree(second.getResponse().getContentAsString());

        assertThat(firstJson.get("periciaId").asText()).isEqualTo(secondJson.get("periciaId").asText());
        assertThat(firstJson).isEqualTo(secondJson);
    }

    private String validPayload() {
        return """
                {
                  "numeroSinistro": "SIN123456",
                  "tipoPericia": "VISTORIA_VEICULAR",
                  "prestadorId": "PREST-001",
                  "dataAgendamento": "2026-02-10T14:00:00Z",
                  "local": {
                    "endereco": "Rua das Flores, 123",
                    "cidade": "São Paulo",
                    "uf": "SP",
                    "cep": "01234-567"
                  },
                  "contato": {
                    "nome": "João da Silva",
                    "telefone": "+5511999998888"
                  },
                  "observacoes": "Veículo disponível das 14h às 17h"
                }
                """;
    }
}

