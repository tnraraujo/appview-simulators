package com.zurich.santander.simulator.pega_bpm_simulator.controller;

import com.zurich.santander.simulator.pega_bpm_simulator.dto.*;
import com.zurich.santander.simulator.pega_bpm_simulator.service.SimulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pega/v1/cases")
@RequiredArgsConstructor
@Slf4j
public class PegaBpmController {

    private final SimulationService simulationService;
    private final Map<String, CriarCasoRequest> createdCases = new ConcurrentHashMap<>();

    @PostMapping("/sinistro")
    @RateLimiter(name = "pegaServer")
    public ResponseEntity<CasoCriadoResponse> criarCaso(
            @RequestHeader(value = "X-Simulate-Error", required = false) String simulateError,
            @RequestHeader(value = "X-Simulate-Delay", required = false) String simulateDelay,
            @Valid @RequestBody CriarCasoRequest request) {

        log.info("Recebida requisição para criar caso de sinistro: {}", request.getNumeroSinistro());
        simulationService.processSimulationHeaders(simulateError, simulateDelay);

        Map.Entry<String, CriarCasoRequest> existingEntry = createdCases.entrySet().stream()
                .filter(e -> e.getValue().getNumeroSinistro().equals(request.getNumeroSinistro()))
                .findFirst()
                .orElse(null);

        if (existingEntry != null || "409".equals(simulateError)) {
            String existingId = existingEntry != null ? existingEntry.getKey() : "PEGA-CASE-SIMULATED-409";
            log.warn("Caso duplicado simulado/detectado para sinistro: {}. Retornando caso existente. HTTP 409", request.getNumeroSinistro());

            CasoCriadoResponse response = new CasoCriadoResponse();
            response.setCaseId(existingId);
            response.setStatus("CREATED");
            response.setWorkflowStarted(true);
            response.setNextStep("ANALISE_DOCUMENTACAO");
            response.setCreatedAt(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        String caseId = "PEGA-CASE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        createdCases.put(caseId, request);

        CasoCriadoResponse response = new CasoCriadoResponse();
        response.setCaseId(caseId);
        response.setStatus("CREATED");
        response.setWorkflowStarted(true);
        response.setNextStep("ANALISE_DOCUMENTACAO");
        response.setCreatedAt(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        log.info("Caso de sinistro criado com sucesso. CaseId: {}", caseId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{caseId}/documents")
    @RateLimiter(name = "pegaServer")
    public ResponseEntity<DocumentoAtualizadoResponse> atualizarDocumento(
            @PathVariable String caseId,
            @RequestHeader(value = "X-Simulate-Error", required = false) String simulateError,
            @RequestHeader(value = "X-Simulate-Delay", required = false) String simulateDelay,
            @RequestBody AtualizarDocumentoRequest request) {

        log.info("Recebida requisição para atualizar documentos do caso: {}", caseId);
        simulationService.processSimulationHeaders(simulateError, simulateDelay);

        if (!createdCases.containsKey(caseId) && !"200".equals(simulateError)) { // Allow 200 override for testing
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        DocumentoAtualizadoResponse response = new DocumentoAtualizadoResponse();
        response.setCaseId(caseId);
        response.setDocumentsCount(3); // Mocked fixed value
        response.setUpdatedAt(OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        log.info("Documentos do caso {} atualizados com sucesso.", caseId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{caseId}")
    @RateLimiter(name = "pegaServer")
    public ResponseEntity<ConsultarCasoResponse> consultarCaso(
            @PathVariable String caseId,
            @RequestHeader(value = "X-Simulate-Error", required = false) String simulateError,
            @RequestHeader(value = "X-Simulate-Delay", required = false) String simulateDelay) {

        log.info("Recebida requisição para consultar caso: {}", caseId);
        simulationService.processSimulationHeaders(simulateError, simulateDelay);

        CriarCasoRequest originalRequest = createdCases.get(caseId);
        if (originalRequest == null && !"200".equals(simulateError)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        ConsultarCasoResponse response = new ConsultarCasoResponse();
        response.setCaseId(caseId);
        response.setNumeroSinistro(originalRequest != null ? originalRequest.getNumeroSinistro() : "SIN-MOCKED");
        response.setStatus("EM_ANALISE");
        response.setCurrentStep("ANALISE_DOCUMENTACAO");
        response.setAssignedTo("analista.jose@zurich.com");
        
        String now = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        response.setCreatedAt(now);
        response.setUpdatedAt(now);
        
        ConsultarCasoResponse.Sla sla = new ConsultarCasoResponse.Sla();
        sla.setDeadline(OffsetDateTime.now().plusHours(48).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        sla.setRemainingHours(46);
        response.setSla(sla);

        log.info("Consulta do caso {}, status: {}", caseId, response.getStatus());
        return ResponseEntity.ok(response);
    }
}
