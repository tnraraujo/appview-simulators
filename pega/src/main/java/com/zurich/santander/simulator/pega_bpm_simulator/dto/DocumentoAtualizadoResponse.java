package com.zurich.santander.simulator.pega_bpm_simulator.dto;

import lombok.Data;

@Data
public class DocumentoAtualizadoResponse {
    private String caseId;
    private Integer documentsCount;
    private String updatedAt;
}
