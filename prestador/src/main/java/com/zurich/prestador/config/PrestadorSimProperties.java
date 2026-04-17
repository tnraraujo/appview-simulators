package com.zurich.prestador.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "prestador.sim")
public class PrestadorSimProperties {

    private String defaultMode = "success";
    private long defaultDelayMs = 100;

    public String getDefaultMode() {
        return defaultMode;
    }

    public void setDefaultMode(String defaultMode) {
        this.defaultMode = defaultMode;
    }


    public long getDefaultDelayMs() {
        return defaultDelayMs;
    }

    public void setDefaultDelayMs(long defaultDelayMs) {
        this.defaultDelayMs = defaultDelayMs;
    }
}

