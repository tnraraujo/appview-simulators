package com.zurich.santander.simulator.jarvis.model;

public enum DocumentProcessingStatus {
    PENDING,
    RETRY_PENDING,
    COMPLETED,
    POOR_QUALITY,
    TIMEOUT,
    FAILED
}

