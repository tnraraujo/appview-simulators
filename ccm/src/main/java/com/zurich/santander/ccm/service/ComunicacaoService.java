package com.zurich.santander.ccm.service;

import com.zurich.santander.ccm.dto.ComunicacaoRequest;
import com.zurich.santander.ccm.dto.ComunicacaoResponse;
import com.zurich.santander.ccm.dto.ComunicacaoStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.MeterRegistry;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComunicacaoService {

    @Value("${simulador.mode:success}")
    private String globalSimMode;

    @Value("${simulador.delay-ms:100}")
    private long defaultDelayMs;

    private final ConcurrentHashMap<String, AtomicInteger> flakyCounterMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ComunicacaoStatusResponse> inMemoryDb = new ConcurrentHashMap<>();
    private final MeterRegistry meterRegistry;

    public ComunicacaoResponse processarComunicacao(ComunicacaoRequest request, String simModeHeader, String simModeParam) {
        String mode = determinarModo(simModeHeader, simModeParam);
        
        String idempotencyKey = request.getNumeroSinistro() + "-" + request.getConteudo().getTemplate();
        
        switch (mode.toLowerCase()) {
            case "timeout":
                log.info("Modo Timeout: atrasando por 12s...");
                meterRegistry.counter("ccm.requests.failed", "mode", "timeout").increment();
                sleep(12000);
                break;
                
            case "fail":
                log.info("Modo Fail: retornando erro interno 500");
                meterRegistry.counter("ccm.requests.failed", "mode", "fail").increment();
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Simulated internal server error in CCM");
                
            case "flaky":
                AtomicInteger count = flakyCounterMap.computeIfAbsent(idempotencyKey, k -> new AtomicInteger(1));
                int currentAttempt = count.getAndIncrement();
                log.info("Modo Flaky: tentativa {} para chave {}", currentAttempt, idempotencyKey);
                if (currentAttempt % 2 != 0) { // first try fails
                    meterRegistry.counter("ccm.requests.failed", "mode", "flaky").increment();
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Flaky simulated failure");
                }
                // successful the second time
                break;
                
            default: // success
                sleep(defaultDelayMs);
                break;
        }

        meterRegistry.counter("ccm.requests.total").increment();
        return buildSuccessResponse(request);
    }

    private String determinarModo(String header, String param) {
        if (header != null && !header.isBlank()) return header;
        if (param != null && !param.isBlank()) return param;
        return globalSimMode;
    }

    private ComunicacaoResponse buildSuccessResponse(ComunicacaoRequest request) {
        String comunicacaoId = "COM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String envioEstimado = request.getAgendarPara() != null 
            ? request.getAgendarPara() 
            : OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(5).toString();

        List<ComunicacaoResponse.CanalAgendado> canaisAgendados = request.getCanais().stream()
            .map(canal -> ComunicacaoResponse.CanalAgendado.builder()
                .canal(canal.toUpperCase())
                .status("AGENDADO")
                .envioEstimado(envioEstimado)
                .build())
            .collect(Collectors.toList());

        String agora = OffsetDateTime.now(ZoneOffset.UTC).toString();

        ComunicacaoResponse response = ComunicacaoResponse.builder()
                .comunicacaoId(comunicacaoId)
                .status("AGENDADA")
                .createdAt(agora)
                .canaisAgendados(canaisAgendados)
                .build();

        List<ComunicacaoStatusResponse.CanalStatus> canaisStatus = request.getCanais().stream()
            .map(canal -> ComunicacaoStatusResponse.CanalStatus.builder()
                .canal(canal.toUpperCase())
                .status("EM PROCESSAMENTO")
                .build())
            .collect(Collectors.toList());

        ComunicacaoStatusResponse status = ComunicacaoStatusResponse.builder()
            .comunicacaoId(comunicacaoId)
            .numeroSinistro(request.getNumeroSinistro())
            .status("PROCESSANDO")
            .createdAt(agora)
            .canais(canaisStatus)
            .build();

        inMemoryDb.put(comunicacaoId, status);

        return response;
    }

    public ComunicacaoStatusResponse consultarStatus(String comunicacaoId, String simModeHeader, String simModeParam) {
        String mode = determinarModo(simModeHeader, simModeParam);

        if ("notfound".equalsIgnoreCase(mode)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ComunicaÃ§Ã£o nÃ£o encontrada");
        }

        ComunicacaoStatusResponse status = inMemoryDb.get(comunicacaoId);

        if (status == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "ComunicaÃ§Ã£o nÃ£o encontrada no simulador");
        }

        // Simular entrega apÃ³s um certo tempo ou de acordo com um header especÃ­fico
        if ("entregue".equalsIgnoreCase(mode)) {
            status.setStatus("ENTREGUE");
            status.setEnviadoEm(OffsetDateTime.parse(status.getCreatedAt()).plusSeconds(2).toString());
            status.setEntregueEm(OffsetDateTime.now(ZoneOffset.UTC).toString());
            status.getCanais().forEach(c -> c.setStatus("ENTREGUE"));
        } else if ("falha".equalsIgnoreCase(mode)) {
            status.setStatus("FALHA");
            status.setFalhaEm(OffsetDateTime.now(ZoneOffset.UTC).toString());
            status.getCanais().forEach(c -> c.setStatus("FALHA_ENTREGA"));
        } else {
            // default behavior - auto update status based on time (10 seconds)
            OffsetDateTime created = OffsetDateTime.parse(status.getCreatedAt());
            if (OffsetDateTime.now(ZoneOffset.UTC).isAfter(created.plusSeconds(10))) {
                status.setStatus("ENTREGUE");
                status.setEnviadoEm(created.plusSeconds(3).toString());
                status.setEntregueEm(created.plusSeconds(6).toString());
                status.getCanais().forEach(c -> c.setStatus("ENTREGUE"));
            }
        }

        return status;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
