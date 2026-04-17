package com.zurich.santander.simulator.pega_bpm_simulator.dto;

import lombok.Data;
import jakarta.validation.Valid;
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
    private DocumentoAlfresco documentoAlfresco;

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
}
