package com.zurich.santander.simulator.jarvis.repository;

import com.zurich.santander.simulator.jarvis.model.DocumentRequest;
import com.zurich.santander.simulator.jarvis.model.DocumentProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DocumentRequestRepository extends JpaRepository<DocumentRequest, Long> {
    List<DocumentRequest> findByStatusInAndNextAttemptAtLessThanEqual(List<DocumentProcessingStatus> statuses, LocalDateTime now);
}
