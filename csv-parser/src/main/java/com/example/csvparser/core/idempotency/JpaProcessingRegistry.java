package com.example.csvparser.core.idempotency;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaProcessingRegistry implements ProcessingRegistry {
    private final ProcessedCsvFileRepository repository;

    public JpaProcessingRegistry(ProcessedCsvFileRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ProcessedCsvFile> findProcessed(String objectKey, String checksumSha256) {
        return repository.findTopByObjectKeyAndChecksumSha256OrderByProcessedAtDesc(objectKey, checksumSha256);
    }

    @Override
    public ProcessedCsvFile record(ProcessedCsvFile processedCsvFile) {
        return repository.save(processedCsvFile);
    }
}
