package com.zurich.prestador.web;

public final class RequestContextKeys {

    public static final String REQUEST_ID_ATTR = "sim.requestId";
    public static final String CORRELATION_ID_ATTR = "sim.correlationId";
    public static final String TRACEPARENT_ATTR = "sim.traceparent";

    private RequestContextKeys() {
    }
}

