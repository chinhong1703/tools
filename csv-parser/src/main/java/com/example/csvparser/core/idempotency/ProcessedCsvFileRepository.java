package com.example.csvparser.core.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcessedCsvFileRepository extends JpaRepository<ProcessedCsvFile, Long> {
    Optional<ProcessedCsvFile> findTopByObjectKeyAndChecksumSha256OrderByProcessedAtDesc(
            String objectKey,
            String checksumSha256
    );
}
