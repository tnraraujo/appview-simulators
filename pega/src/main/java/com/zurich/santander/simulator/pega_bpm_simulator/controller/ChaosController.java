package com.zurich.santander.simulator.pega_bpm_simulator.controller;

import com.zurich.santander.simulator.pega_bpm_simulator.dto.ChaosRequest;
import com.zurich.santander.simulator.pega_bpm_simulator.service.SimulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/simulator/chaos")
@RequiredArgsConstructor
@Slf4j
public class ChaosController {

    private final SimulationService simulationService;

    @PostMapping
    public ResponseEntity<String> configureChaos(@RequestBody ChaosRequest request) {
        log.warn("ATENÇÃO: Chaos Engineering ativado! Injetando Status HTTP={}, Timeout: {} ms",
                 request.getStatus(), request.getDelayMs());

        simulationService.configureChaos(request.getStatus(), request.getDelayMs());

        return ResponseEntity.ok("Ambiente de Caos configurado com sucesso.");
    }

    @DeleteMapping
    public ResponseEntity<String> resetChaos() {
        log.info("Resetando ambiente de Chaos. Sistema voltando à operação normal.");
        simulationService.resetChaos();
        return ResponseEntity.ok("Ambiente de Caos resetado.");
    }
}
