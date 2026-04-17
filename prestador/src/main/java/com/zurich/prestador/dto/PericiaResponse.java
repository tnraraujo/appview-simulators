package com.zurich.prestador.dto;

public record PericiaResponse(
        String periciaId,
        String status,
        PrestadorResponse prestador,
        String dataAgendamento
) {

    public record PrestadorResponse(
            String nome,
            String telefone
    ) {
    }
}

