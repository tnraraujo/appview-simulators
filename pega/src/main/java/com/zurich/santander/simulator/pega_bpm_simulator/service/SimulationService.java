package com.zurich.santander.simulator.pega_bpm_simulator.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SimulationService {

    private Integer globalError = null;
    private Long globalDelayMs = null;

    public void configureChaos(Integer status, Long delayMs) {
        this.globalError = status;
        this.globalDelayMs = delayMs;
    }

    public void resetChaos() {
        this.globalError = null;
        this.globalDelayMs = null;
    }

    public void processSimulationHeaders(String errorHeader, String delayHeader) {
        Long finalDelay = globalDelayMs;
        if (delayHeader != null && !delayHeader.isEmpty()) {
            try {
                finalDelay = Long.parseLong(delayHeader);
            } catch (NumberFormatException e) {
                // Ignore invalid header format
            }
        }

        if (finalDelay != null && finalDelay > 0) {
            try {
                Thread.sleep(finalDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        Integer finalError = globalError;
        if (errorHeader != null && !errorHeader.isEmpty()) {
            try {
                finalError = Integer.parseInt(errorHeader);
            } catch (NumberFormatException e) {
                // Ignore invalid header format
            }
        }

        if (finalError != null) {
            throw new ResponseStatusException(HttpStatus.valueOf(finalError), "Simulated error: " + finalError);
        }
    }
}
