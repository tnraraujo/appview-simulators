package com.zurich.santander.simulator.jarvis.repository;

import com.zurich.santander.simulator.jarvis.model.DocumentProcessingStatus;
import com.zurich.santander.simulator.jarvis.model.DocumentRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryDocumentRequestRepository implements DocumentRequestRepository {

    private final Map<Long, DocumentRequest> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1L);

    @Override
    public DocumentRequest save(DocumentRequest request) {
        if (request.getId() == null) {
            request.setId(sequence.getAndIncrement());
        }

        if (request.getCreatedAt() == null) {
            request.setCreatedAt(LocalDateTime.now());
        }

        if (request.getNextAttemptAt() == null) {
            request.setNextAttemptAt(request.getCreatedAt());
        }

        store.put(request.getId(), request);
        return request;
    }

    @Override
    public List<DocumentRequest> findByStatusInAndNextAttemptAtLessThanEqual(List<DocumentProcessingStatus> statuses,
                                                                              LocalDateTime now) {
        return store.values().stream()
                .filter(request -> request.getStatus() != null && statuses.contains(request.getStatus()))
                .filter(request -> request.getNextAttemptAt() != null && !request.getNextAttemptAt().isAfter(now))
                .sorted(Comparator.comparing(DocumentRequest::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }
}

