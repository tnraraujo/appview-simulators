package com.zurich.prestador.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zurich.prestador.domain.SimMode;
import com.zurich.prestador.dto.PericiaRequest;
import com.zurich.prestador.dto.PericiaResponse;
import com.zurich.prestador.service.PericiaSimulationService;
import com.zurich.prestador.web.RequestContextKeys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/prestador/v1/pericias")
@RequiredArgsConstructor
@Slf4j
public class PericiaController {

    private final PericiaSimulationService simulationService;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<?> agendarPericia(@Valid @RequestBody PericiaRequest request,
                                            @RequestHeader(value = "X-Sim-Mode", required = false) String simModeHeader,
                                            @RequestParam(value = "mode", required = false) String simModeQuery,
                                            HttpServletRequest rawRequest) throws InterruptedException {
        long start = System.currentTimeMillis();
        SimMode mode = simulationService.resolveMode(simModeHeader, simModeQuery);

        try {
            PericiaResponse response = simulationService.process(request, mode);
            logRequest(rawRequest, HttpStatus.OK.value(), System.currentTimeMillis() - start, request, response.periciaId(), mode);
            return ResponseEntity.ok(response);
        } catch (PericiaSimulationService.Simulated500Exception ex) {
            Map<String, String> error = Map.of("error", "INTERNAL_ERROR", "message", "Simulated failure");
            logRequest(rawRequest, HttpStatus.INTERNAL_SERVER_ERROR.value(), System.currentTimeMillis() - start, request, null, mode);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        } catch (PericiaSimulationService.Simulated503Exception ex) {
            Map<String, String> error = Map.of("error", "SERVICE_UNAVAILABLE", "message", "Service Temporarily Unavailable");
            logRequest(rawRequest, HttpStatus.SERVICE_UNAVAILABLE.value(), System.currentTimeMillis() - start, request, null, mode);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
        }
    }

    private void logRequest(HttpServletRequest request,
                            int status,
                            long durationMs,
                            PericiaRequest payload,
                            String periciaId,
                            SimMode mode) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("timestamp", Instant.now().toString());
        event.put("method", request.getMethod());
        event.put("path", request.getRequestURI());
        event.put("status", status);
        event.put("duration_ms", durationMs);
        event.put("requestId", request.getAttribute(RequestContextKeys.REQUEST_ID_ATTR));
        event.put("correlationId", request.getAttribute(RequestContextKeys.CORRELATION_ID_ATTR));
        event.put("traceparent", request.getAttribute(RequestContextKeys.TRACEPARENT_ATTR));
        event.put("numeroSinistro", payload.numeroSinistro());
        event.put("prestadorId", payload.prestadorId());
        event.put("periciaId", periciaId);
        event.put("simMode", mode.name().toLowerCase());

        try {
            log.info(objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException ex) {
            log.info("{\"message\":\"failed to serialize structured log\",\"status\":{}}", status);
        }
    }
}

