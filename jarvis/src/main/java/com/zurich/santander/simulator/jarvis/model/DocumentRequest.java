package com.zurich.santander.simulator.jarvis.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class DocumentRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String requestId;

    @Column(nullable = false)
    private String documentCode;

    @Column(nullable = false)
    private String claimId;

    private String documentType;

    private String callbackUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DocumentProcessingStatus status;

    @Column(nullable = false)
    private int callbackAttempts;

    private LocalDateTime nextAttemptAt;

    @Column(length = 300)
    private String lastError;

    private LocalDateTime createdAt;
    
    private LocalDateTime processedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (nextAttemptAt == null) {
            nextAttemptAt = createdAt;
        }
    }
}

