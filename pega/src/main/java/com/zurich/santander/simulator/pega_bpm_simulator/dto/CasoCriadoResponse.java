package com.zurich.santander.simulator.pega_bpm_simulator.dto;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class CasoCriadoResponse {
    private String caseId;
    private String status;
    private Boolean workflowStarted;
    private String nextStep;
    private String createdAt;
}
