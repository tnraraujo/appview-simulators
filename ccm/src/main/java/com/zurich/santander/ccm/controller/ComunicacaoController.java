package com.zurich.santander.ccm.controller;

import com.zurich.santander.ccm.dto.ComunicacaoRequest;
import com.zurich.santander.ccm.dto.ComunicacaoResponse;
import com.zurich.santander.ccm.dto.ComunicacaoStatusResponse;
import com.zurich.santander.ccm.service.ComunicacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ccm/v1/comunicacoes")
@RequiredArgsConstructor
@Slf4j
public class ComunicacaoController {

    private final ComunicacaoService comunicacaoService;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ccm:comunicacao:write') or hasAuthority('ROLE_ccm:admin')")
    public ResponseEntity<ComunicacaoResponse> criarComunicacao(
            @RequestHeader(value = "X-Sim-Mode", required = false) String simModeHeader,
            @RequestParam(value = "mode", required = false) String simModeParam,
            @Valid @RequestBody ComunicacaoRequest request) {
        
        log.info("Recebida requisiÃ§Ã£o de comunicaÃ§Ã£o para sinistro: {}, template: {}", 
                 request.getNumeroSinistro(), request.getConteudo().getTemplate());

        ComunicacaoResponse response = comunicacaoService.processarComunicacao(request, simModeHeader, simModeParam);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{comunicacaoId}")
    @PreAuthorize("hasAuthority('ROLE_ccm:comunicacao:read') or hasAuthority('ROLE_ccm:admin') or hasRole('ADMIN')")
    public ResponseEntity<ComunicacaoStatusResponse> consultarStatus(
            @PathVariable String comunicacaoId,
            @RequestHeader(value = "X-Sim-Mode", required = false) String simModeHeader,
            @RequestParam(value = "mode", required = false) String simModeParam) {

        log.info("Recebida requisiÃ§Ã£o de consulta de status para comunicacaoId: {}", comunicacaoId);

        ComunicacaoStatusResponse response = comunicacaoService.consultarStatus(comunicacaoId, simModeHeader, simModeParam);

        return ResponseEntity.ok(response);
    }
}
