package com.example.csvparser;

import com.example.csvparser.core.CsvFormatOptions;
import com.example.csvparser.core.CsvIngestionRequest;
import com.example.csvparser.core.CsvMappingOptions;
import com.example.csvparser.core.CsvValidationOptions;
import com.example.csvparser.core.DefaultCsvIngestionService;
import com.example.csvparser.core.ErrorType;
import com.example.csvparser.core.IngestionResult;
import com.example.csvparser.core.IngestionStatus;
import com.example.csvparser.core.idempotency.JpaProcessingRegistry;
import com.example.csvparser.core.idempotency.ProcessedCsvFileRepository;
import com.example.csvparser.core.mapping.CsvColumn;
import com.example.csvparser.core.mapping.CsvDateFormats;
import com.example.csvparser.core.mapping.CsvTrim;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class DefaultCsvIngestionServiceTest {
    @Autowired
    private ProcessedCsvFileRepository repository;

    @Test
    void continuesAfterRowErrorsAndParsesValidRows() {
        DefaultCsvIngestionService service = new DefaultCsvIngestionService(new JpaProcessingRegistry(repository));
        String csv = "id,name,birthDate\n" +
                "1,Alice,20251225\n" +
                "2,Bob,not-a-date\n";
        CsvIngestionRequest request = new CsvIngestionRequest(
                "people.csv",
                csv.getBytes(StandardCharsets.UTF_8),
                false,
                CsvFormatOptions.defaults(),
                CsvMappingOptions.defaults(),
                new CsvValidationOptions(true)
        );

        IngestionResult<PersonRecord> result = service.ingest(request, PersonRecord.class);

        assertThat(result.status()).isEqualTo(IngestionStatus.PROCESSED);
        assertThat(result.records()).hasSize(1);
        assertThat(result.errors()).anyMatch(error -> error.type() == ErrorType.CONVERSION);
        assertThat(result.stats().totalRows()).isEqualTo(2);
    }

    @Test
    void flagsMissingRequiredHeaders() {
        DefaultCsvIngestionService service = new DefaultCsvIngestionService(new JpaProcessingRegistry(repository));
        String csv = "name,birthDate\n" +
                "Alice,25-12-2025\n";
        CsvIngestionRequest request = new CsvIngestionRequest(
                "missing-header.csv",
                csv.getBytes(StandardCharsets.UTF_8),
                false,
                CsvFormatOptions.defaults(),
                CsvMappingOptions.defaults(),
                new CsvValidationOptions(true)
        );

        IngestionResult<PersonRecord> result = service.ingest(request, PersonRecord.class);

        assertThat(result.errors())
                .anyMatch(error -> error.type() == ErrorType.HEADER && "id".equals(error.columnName()));
    }

    @Test
    void warnsOnUnknownColumns() {
        DefaultCsvIngestionService service = new DefaultCsvIngestionService(new JpaProcessingRegistry(repository));
        String csv = "id,name,extra\n" +
                "1,Alice,ignored\n";
        CsvIngestionRequest request = new CsvIngestionRequest(
                "unknown-column.csv",
                csv.getBytes(StandardCharsets.UTF_8),
                false,
                CsvFormatOptions.defaults(),
                CsvMappingOptions.defaults(),
                new CsvValidationOptions(true)
        );

        IngestionResult<PersonRecord> result = service.ingest(request, PersonRecord.class);

        assertThat(result.warnings()).contains("Unknown column: extra");
    }

    @Test
    void skipsWhenAlreadyProcessed() {
        DefaultCsvIngestionService service = new DefaultCsvIngestionService(new JpaProcessingRegistry(repository));
        String csv = "id,name,birthDate\n" +
                "1,Alice,20251225\n";
        CsvIngestionRequest request = new CsvIngestionRequest(
                "idempotent.csv",
                csv.getBytes(StandardCharsets.UTF_8),
                false,
                CsvFormatOptions.defaults(),
                CsvMappingOptions.defaults(),
                new CsvValidationOptions(true)
        );

        IngestionResult<PersonRecord> first = service.ingest(request, PersonRecord.class);
        IngestionResult<PersonRecord> second = service.ingest(request, PersonRecord.class);

        assertThat(first.status()).isEqualTo(IngestionStatus.PROCESSED);
        assertThat(second.status()).isEqualTo(IngestionStatus.SKIPPED_ALREADY_PROCESSED);
    }

    @Test
    void usesFieldLevelDateFormats() {
        DefaultCsvIngestionService service = new DefaultCsvIngestionService(new JpaProcessingRegistry(repository));
        CsvMappingOptions options = new CsvMappingOptions(true, true, false, true, List.of("yyyyMMdd"));
        String csv = "id,name,birthDate\n" +
                "1,Alice,25-12-2025\n";
        CsvIngestionRequest request = new CsvIngestionRequest(
                "date-format.csv",
                csv.getBytes(StandardCharsets.UTF_8),
                false,
                CsvFormatOptions.defaults(),
                options,
                new CsvValidationOptions(true)
        );

        IngestionResult<DateOverrideRecord> result = service.ingest(request, DateOverrideRecord.class);

        assertThat(result.records()).hasSize(1);
        assertThat(result.records().get(0).birthDate()).isEqualTo(LocalDate.of(2025, 12, 25));
    }

    static class PersonRecord {
        @CsvColumn(name = "id", required = true)
        private Integer id;

        @CsvColumn(name = "name", required = true)
        @CsvTrim
        @NotBlank
        private String name;

        @CsvColumn(name = "birthDate")
        private LocalDate birthDate;

        public Integer id() {
            return id;
        }

        public String name() {
            return name;
        }

        public LocalDate birthDate() {
            return birthDate;
        }
    }

    static class DateOverrideRecord {
        @CsvColumn(name = "id", required = true)
        private Integer id;

        @CsvColumn(name = "name", required = true)
        private String name;

        @CsvColumn(name = "birthDate")
        @CsvDateFormats({"dd-MM-yyyy"})
        private LocalDate birthDate;

        public LocalDate birthDate() {
            return birthDate;
        }
    }
}
