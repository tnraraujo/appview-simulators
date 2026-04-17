package com.zurich.santander.simulator.jarvis.service;

import com.zurich.santander.simulator.jarvis.config.JarvisProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private static final long WINDOW_MILLIS = 60_000;

    private final JarvisProperties properties;
    private final Map<String, Deque<Long>> callsByApiKey = new ConcurrentHashMap<>();

    public RateLimitService(JarvisProperties properties) {
        this.properties = properties;
    }

    public boolean isAllowed(String apiKey) {
        Deque<Long> calls = callsByApiKey.computeIfAbsent(apiKey, key -> new ArrayDeque<>());
        long now = Instant.now().toEpochMilli();

        synchronized (calls) {
            while (!calls.isEmpty() && now - calls.peekFirst() > WINDOW_MILLIS) {
                calls.removeFirst();
            }

            if (calls.size() >= properties.getRateLimitPerMinute()) {
                return false;
            }

            calls.addLast(now);
            return true;
        }
    }
}

