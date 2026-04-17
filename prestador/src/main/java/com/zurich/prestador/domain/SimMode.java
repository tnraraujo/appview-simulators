package com.zurich.prestador.domain;

public enum SimMode {
    SUCCESS,
    TIMEOUT,
    FAIL,
    UNAVAILABLE,
    SLOW_SLA,
    FLAKY;

    public static SimMode from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return SimMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid mode. Allowed values: SUCCESS, TIMEOUT, FAIL, UNAVAILABLE, SLOW_SLA, FLAKY");
        }
    }
}
