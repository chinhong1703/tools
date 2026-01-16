# Generic CSV Parser Component

This module provides a reusable CSV ingestion service for Spring Boot applications. It maps CSV headers to POJO fields using annotations, supports configurable CSV formats, collects row-level errors without failing fast, and records idempotent processing using a checksum and database persistence.

## Features

- **Annotation-driven mapping**: map CSV headers to fields via `@CsvColumn` with optional trimming and per-field date formats.
- **Configurable parsing**: delimiter, quote, escape, and charset options with UTF-8 BOM handling.
- **Row-level error capture**: continue on error and return structured errors with row/column context.
- **Idempotent processing**: SHA-256 checksum + database tracking keyed by object key + checksum.
- **Optional Bean Validation**: automatically validates parsed records when Jakarta Validation is present.

## Dependency

The module uses uniVocity parsers for robust CSV handling.

```xml
<dependency>
  <groupId>com.univocity</groupId>
  <artifactId>univocity-parsers</artifactId>
  <version>2.9.1</version>
</dependency>
```

## Quick start

### 1) Annotate a record class

```java
import com.example.csvparser.core.mapping.CsvColumn;
import com.example.csvparser.core.mapping.CsvDateFormats;
import com.example.csvparser.core.mapping.CsvTrim;
import java.time.LocalDate;

public class PersonRecord {
  @CsvColumn(name = "id", required = true)
  private Integer id;

  @CsvColumn(name = "name", required = true)
  @CsvTrim
  private String name;

  @CsvColumn(name = "birthDate")
  @CsvDateFormats({"dd-MM-yyyy"})
  private LocalDate birthDate;

  public Integer id() { return id; }
  public String name() { return name; }
  public LocalDate birthDate() { return birthDate; }
}
```

### 2) Build the ingestion request

```java
import com.example.csvparser.core.CsvFormatOptions;
import com.example.csvparser.core.CsvIngestionRequest;
import com.example.csvparser.core.CsvMappingOptions;
import com.example.csvparser.core.CsvValidationOptions;

byte[] content = "id,name,birthDate\n1,Alice,25-12-2025\n".getBytes(StandardCharsets.UTF_8);

CsvIngestionRequest request = new CsvIngestionRequest(
  "people.csv",
  content,
  false,
  CsvFormatOptions.defaults(),
  CsvMappingOptions.defaults(),
  new CsvValidationOptions(true)
);
```

### 3) Ingest and inspect results

```java
import com.example.csvparser.core.DefaultCsvIngestionService;
import com.example.csvparser.core.IngestionResult;
import com.example.csvparser.core.idempotency.JpaProcessingRegistry;

DefaultCsvIngestionService service = new DefaultCsvIngestionService(new JpaProcessingRegistry(repository));
IngestionResult<PersonRecord> result = service.ingest(request, PersonRecord.class);

System.out.println(result.status());
System.out.println("records=" + result.records().size());
System.out.println("errors=" + result.errors().size());
```

## Options reference

### CsvFormatOptions

```java
new CsvFormatOptions(
  ',',  // delimiter
  '"',  // quote
  '"',  // escape
  StandardCharsets.UTF_8
);
```

### CsvMappingOptions

```java
new CsvMappingOptions(
  true,  // trimHeaders
  true,  // exactHeaderMatch
  true,  // ignoreUnknownColumns
  List.of("yyyyMMdd", "dd-MM-yyyy", "dd/MM/yyyy", "yyyy-MM-dd")
);
```

### CsvValidationOptions

```java
new CsvValidationOptions(true);
```

## Error and status model

The ingestion result includes:

- `records`: successfully parsed and validated objects.
- `errors`: row-level errors with row number, column name, raw value, and error type.
- `warnings`: header mismatches and unknown columns.
- `status`: `PROCESSED`, `SKIPPED_ALREADY_PROCESSED`, or `FAILED`.

Row numbering is 1-based, with the header row at row 1 and the first data row at row 2.

## Idempotency tracking

Each processed file is recorded in `processed_csv_file` with a unique constraint on `(object_key, checksum_sha256)`. If the same object key and checksum are seen again, the ingestion returns `SKIPPED_ALREADY_PROCESSED`.

## Running tests

```bash
mvn -f /path/to/csv-parser/pom.xml test
```
