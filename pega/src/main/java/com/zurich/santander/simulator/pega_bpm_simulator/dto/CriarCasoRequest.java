package com.zurich.santander.simulator.pega_bpm_simulator.dto;

import lombok.Data;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Data
public class CriarCasoRequest {

    @NotBlank(message = "O numero do sinistro é obrigatório")
    private String numeroSinistro;

    @NotBlank(message = "O tipo de documento é obrigatório")
    private String tipoDocumento;

    @NotBlank(message = "O canal é obrigatório")
    private String canal;

    @Valid
    @NotNull(message = "As informações de LossInfo são obrigatórias")
    private LossInfo lossInfo;

    @Valid
    @NotNull(message = "DocumentoAlfresco é obrigatório")
    private DocumentoAlfresco documentoAlfresco;

    @Valid
    @NotNull(message = "OCR scores são obrigatórios")
    private OcrScores ocrScores;

    private Map<String, Object> metadata;

    @Data
    public static class LossInfo {
        @NotBlank(message = "LossNumber é obrigatório")
        private String lossNumber;
        private String lossSequence;
        private Integer lossYear;
        private String branch;
    }

    @Data
    public static class DocumentoAlfresco {
        @NotBlank(message = "DocumentId é obrigatório")
        private String documentId;
        private String documentPath;
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

        @NotNull(message = "MatchDocumento é obrigatório")
        @Min(value = 0, message = "MatchDocumento mínimo é 0")
        @Max(value = 100, message = "MatchDocumento máximo é 100")
        private Integer matchDocumento;
    }
}
