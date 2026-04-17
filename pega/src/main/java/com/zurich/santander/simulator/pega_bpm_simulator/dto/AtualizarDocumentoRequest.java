package com.zurich.santander.simulator.pega_bpm_simulator.dto;

import lombok.Data;
import java.util.Map;

@Data
public class AtualizarDocumentoRequest {
    private DocumentoAlfresco documentoAlfresco;
    private OcrScores ocrScores;

    @Data
    public static class DocumentoAlfresco {
        private String documentId;
        private String documentType;
    }

    @Data
    public static class OcrScores {
        private Integer legibilidade;
        private Integer acuracidade;
    }
}
