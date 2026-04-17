package com.zurich.santander.simulator.pega_bpm_simulator.dto;

import lombok.Data;

@Data
public class ChaosRequest {
    private Integer status;
    private Long delayMs;
}

