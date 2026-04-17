package com.zurich.prestador.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class CorrelationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = headerOrGenerated(request.getHeader("X-Request-ID"));
        String correlationId = headerOrGenerated(request.getHeader("X-Correlation-ID"));
        String traceparent = request.getHeader("traceparent");

        request.setAttribute(RequestContextKeys.REQUEST_ID_ATTR, requestId);
        request.setAttribute(RequestContextKeys.CORRELATION_ID_ATTR, correlationId);
        request.setAttribute(RequestContextKeys.TRACEPARENT_ATTR, traceparent);

        response.setHeader("X-Request-ID", requestId);
        response.setHeader("X-Correlation-ID", correlationId);
        if (traceparent != null && !traceparent.isBlank()) {
            response.setHeader("traceparent", traceparent);
        }

        filterChain.doFilter(request, response);
    }

    private String headerOrGenerated(String value) {
        if (value == null || value.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return value;
    }
}

