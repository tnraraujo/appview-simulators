package com.zurich.santander.simulator.jarvis.repository;

import com.zurich.santander.simulator.jarvis.model.DocumentRequest;
import com.zurich.santander.simulator.jarvis.model.DocumentProcessingStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface DocumentRequestRepository {
    DocumentRequest save(DocumentRequest request);

    List<DocumentRequest> findByStatusInAndNextAttemptAtLessThanEqual(List<DocumentProcessingStatus> statuses, LocalDateTime now);
}
