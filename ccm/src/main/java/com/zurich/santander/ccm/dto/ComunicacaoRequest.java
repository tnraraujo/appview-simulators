package com.zurich.santander.ccm.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ComunicacaoRequest {

    @NotBlank(message = "tipo Ã© obrigatÃ³rio")
    private String tipo;

    @NotBlank(message = "numeroSinistro Ã© obrigatÃ³rio")
    private String numeroSinistro;

    @Valid
    @NotNull(message = "destinatario Ã© obrigatÃ³rio")
    private Destinatario destinatario;

    @Valid
    @NotNull(message = "conteudo Ã© obrigatÃ³rio")
    private Conteudo conteudo;

    @NotEmpty(message = "canais sÃ£o obrigatÃ³rios")
    private List<String> canais;

    private String prioridade;
    
    private String agendarPara;

    @Data
    public static class Destinatario {
        @NotBlank(message = "nome do destinatÃ¡rio Ã© obrigatÃ³rio")
        private String nome;
        private String cpfCnpj;
        private String email;
        private String telefone;
    }

    @Data
    public static class Conteudo {
        @NotBlank(message = "template Ã© obrigatÃ³rio")
        private String template;
        private Map<String, String> parametros;
    }
}

