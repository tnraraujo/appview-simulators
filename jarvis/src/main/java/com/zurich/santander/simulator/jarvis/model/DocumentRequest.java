package com.zurich.santander.simulator.jarvis.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class DocumentRequest {

    private Long id;

    private String requestId;

    private String documentCode;

    private String claimId;

    private String documentType;


    private DocumentProcessingStatus status;

    private int callbackAttempts;

    private LocalDateTime nextAttemptAt;

    private String lastError;

    private LocalDateTime createdAt;
    
    private LocalDateTime processedAt;

}

