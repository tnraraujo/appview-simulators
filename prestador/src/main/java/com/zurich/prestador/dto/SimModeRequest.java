package com.zurich.prestador.dto;

import jakarta.validation.constraints.NotBlank;

public record SimModeRequest(
        @NotBlank String mode
) {
}

