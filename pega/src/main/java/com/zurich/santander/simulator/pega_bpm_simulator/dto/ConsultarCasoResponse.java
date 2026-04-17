package com.zurich.santander.simulator.pega_bpm_simulator.dto;

import lombok.Data;

@Data
public class ConsultarCasoResponse {
    private String caseId;
    private String numeroSinistro;
    private String status;
    private String currentStep;
    private String assignedTo;
    private String createdAt;
    private String updatedAt;
    private Sla sla;

    @Data
    public static class Sla {
        private String deadline;
        private Integer remainingHours;
    }
}
