package com.zurich.prestador.service;

import com.zurich.prestador.config.PrestadorSimProperties;
import com.zurich.prestador.domain.SimMode;
import com.zurich.prestador.dto.PericiaRequest;
import com.zurich.prestador.dto.PericiaResponse;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PericiaSimulationService {

    private static final String PRESTADOR_NOME = "Auto Vistoria SP";
    private static final String PRESTADOR_TELEFONE = "+5511988887777";

    private final PrestadorSimProperties properties;
    
    private final Map<String, PericiaResponse> idempotentResponses = Collections.synchronizedMap(new LinkedHashMap<String, PericiaResponse>(1000, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, PericiaResponse> eldest) {
            return size() > 1000;
        }
    });
    
    private final Map<String, Integer> flakyAttempts = Collections.synchronizedMap(new LinkedHashMap<String, Integer>(1000, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
            return size() > 1000;
        }
    });
    
    private volatile SimMode globalMode;

    public PericiaSimulationService(PrestadorSimProperties properties) {
        this.properties = properties;
        this.globalMode = SimMode.from(properties.getDefaultMode());
        if (this.globalMode == null) {
            this.globalMode = SimMode.SUCCESS;
        }
    }

    public SimMode resolveMode(String headerMode, String queryMode) {
        SimMode fromHeader = safeParse(headerMode);
        if (fromHeader != null) {
            return fromHeader;
        }

        SimMode fromQuery = safeParse(queryMode);
        if (fromQuery != null) {
            return fromQuery;
        }

        return globalMode;
    }

    public PericiaResponse process(PericiaRequest request, SimMode mode) throws InterruptedException {
        String key = composeKey(request);

        if (mode == SimMode.FAIL) {
            throw new Simulated500Exception();
        }

        if (mode == SimMode.UNAVAILABLE) {
            throw new Simulated503Exception();
        }

        if (mode == SimMode.TIMEOUT) {
            Thread.sleep(12_000);
            return buildOrReuseResponse(key, request);
        }

        if (mode == SimMode.SLOW_SLA) {
            Thread.sleep(5_100);
            return buildOrReuseResponse(key, request);
        }

        if (mode == SimMode.FLAKY) {
            int attempt;
            synchronized (flakyAttempts) {
                attempt = flakyAttempts.merge(key, 1, Integer::sum);
            }
            if (attempt == 1) {
                throw new Simulated503Exception();
            }
            return buildOrReuseResponse(key, request);
        }

        long baseDelay = properties.getDefaultDelayMs();
        if (baseDelay > 0) {
            // Apply +/- 20% random variance
            long variance = (long) (baseDelay * 0.20);
            long randomDelay = baseDelay - variance + (long) (Math.random() * (variance * 2));
            Thread.sleep(Math.max(10, randomDelay));
        }

        return buildOrReuseResponse(key, request);
    }

    public void setGlobalMode(SimMode mode) {
        this.globalMode = mode;
    }

    public SimMode getGlobalMode() {
        return globalMode;
    }

    public String composeKey(PericiaRequest request) {
        return String.join("|",
                nullSafe(request.numeroSinistro()),
                nullSafe(request.prestadorId()),
                nullSafe(request.dataAgendamento()),
                nullSafe(request.tipoPericia())
        );
    }

    private PericiaResponse buildOrReuseResponse(String key, PericiaRequest request) {
        synchronized (idempotentResponses) {
            return idempotentResponses.computeIfAbsent(key, ignored -> new PericiaResponse(
                    generatePericiaId(key),
                    "AGENDADA",
                    new PericiaResponse.PrestadorResponse(PRESTADOR_NOME, PRESTADOR_TELEFONE),
                    request.dataAgendamento()
            ));
        }
    }

    private String generatePericiaId(String key) {
        int hash = Math.abs(key.hashCode());
        String paddedHash = String.format("%06d", hash);
        if (paddedHash.length() > 6) {
            paddedHash = paddedHash.substring(0, 6);
        }
        return "PER-" + paddedHash;
    }

    private SimMode safeParse(String value) {
        try {
            return SimMode.from(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value.trim();
    }

    public static class Simulated500Exception extends RuntimeException {}
    public static class Simulated503Exception extends RuntimeException {}
}
