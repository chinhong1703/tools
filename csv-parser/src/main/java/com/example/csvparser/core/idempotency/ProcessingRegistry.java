package com.example.csvparser.core.idempotency;

import java.util.Optional;

public interface ProcessingRegistry {
    Optional<ProcessedCsvFile> findProcessed(String objectKey, String checksumSha256);

    ProcessedCsvFile record(ProcessedCsvFile processedCsvFile);
}
