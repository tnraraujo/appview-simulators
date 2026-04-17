package com.zurich.prestador.controller;

import com.zurich.prestador.domain.SimMode;
import com.zurich.prestador.dto.SimModeRequest;
import com.zurich.prestador.service.PericiaSimulationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/__admin")
@RequiredArgsConstructor
public class AdminController {

    private final PericiaSimulationService simulationService;

    @PostMapping("/sim-mode")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> setSimMode(@Valid @RequestBody SimModeRequest request) {
        SimMode mode = SimMode.from(request.mode());
        simulationService.setGlobalMode(mode);
        return Map.of("mode", mode.name().toLowerCase());
    }
}

