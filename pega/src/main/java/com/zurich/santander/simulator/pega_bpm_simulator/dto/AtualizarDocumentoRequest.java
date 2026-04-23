package com.zurich.santander.simulator.pega_bpm_simulator.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AtualizarDocumentoRequest {
    @Valid
    @NotNull(message = "DocumentoAlfresco é obrigatório")
    private DocumentoAlfresco documentoAlfresco;

    @Valid
    @NotNull(message = "OCR scores são obrigatórios")
    private OcrScores ocrScores;

    @Data
    public static class DocumentoAlfresco {
        @NotBlank(message = "DocumentId é obrigatório")
        private String documentId;

        @NotBlank(message = "DocumentType é obrigatório")
        private String documentType;
    }

    @Data
    public static class OcrScores {
        @NotNull(message = "Legibilidade é obrigatória")
        @Min(value = 0, message = "Legibilidade mínima é 0")
        @Max(value = 100, message = "Legibilidade máxima é 100")
        private Integer legibilidade;

        @NotNull(message = "Acuracidade é obrigatória")
        @Min(value = 0, message = "Acuracidade mínima é 0")
        @Max(value = 100, message = "Acuracidade máxima é 100")
        private Integer acuracidade;
    }
}
