package com.zurich.santander.ccm.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ComunicacaoStatusResponse {
    private String comunicacaoId;
    private String numeroSinistro;
    private String status;
    private String createdAt;
    private String enviadoEm;
    private String entregueEm;
    private String falhaEm;
    private List<CanalStatus> canais;

    @Data
    @Builder
    public static class CanalStatus {
        private String canal;
        private String status;
        private String detalhes;
    }
}

