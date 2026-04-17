package com.zurich.santander.ccm.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ComunicacaoResponse {
    private String comunicacaoId;
    private String status;
    private String createdAt;
    private List<CanalAgendado> canaisAgendados;

    @Data
    @Builder
    public static class CanalAgendado {
        private String canal;
        private String status;
        private String envioEstimado;
    }
}
