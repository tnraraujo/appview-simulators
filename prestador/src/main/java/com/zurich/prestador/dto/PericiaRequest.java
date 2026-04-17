package com.zurich.prestador.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PericiaRequest(
        @NotBlank(message = "numeroSinistro is required")
        String numeroSinistro,
        
        @JsonAlias("tipoPéricia")
        @NotBlank(message = "tipoPericia is required")
        String tipoPericia,
        
        @NotBlank(message = "prestadorId is required")
        String prestadorId,
        
        @NotBlank(message = "dataAgendamento is required")
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$", message = "invalid date format, ISO-8601 expected")
        String dataAgendamento,
        
        @NotNull(message = "local is required")
        @Valid
        LocalRequest local,
        
        @NotNull(message = "contato is required")
        @Valid
        ContatoRequest contato,
        
        String observacoes
) {

    public record LocalRequest(
            @NotBlank(message = "endereco is required") String endereco,
            @NotBlank(message = "cidade is required") String cidade,
            @NotBlank(message = "uf is required") String uf,
            @NotBlank(message = "cep is required") String cep
    ) {
    }

    public record ContatoRequest(
            @NotBlank(message = "nome is required") String nome,
            @NotBlank(message = "telefone is required") String telefone
    ) {
    }
}
