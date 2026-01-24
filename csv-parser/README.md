# Generic CSV & Spreadsheet Parser Component

This module provides reusable CSV and Excel ingestion services for Spring Boot applications. It maps headers to POJO fields using annotations, supports configurable formats, collects row-level errors without failing fast, and records idempotent processing using a checksum and database persistence.

## Features

- **Annotation-driven mapping**: map CSV headers or spreadsheet columns to fields via `@CsvColumn` with optional trimming and per-field date formats.
- **Configurable parsing**: CSV delimiter/quote/escape/charset options with UTF-8 BOM handling; Excel sheet/section templates.
- **Row-level error capture**: continue on error and return structured errors with row/column context.
- **Idempotent processing**: SHA-256 checksum + database tracking keyed by object key + checksum.
- **Optional Bean Validation**: automatically validates parsed records when Jakarta Validation is present.
- **Excel support**: key-value labels plus a tabular section per sheet, formula evaluation, and date-aware cell parsing.

## Dependencies

The module uses uniVocity for CSV parsing and Apache POI for Excel parsing.

```xml
<dependency>
  <groupId>com.univocity</groupId>
  <artifactId>univocity-parsers</artifactId>
  <version>2.9.1</version>
</dependency>
```

```xml
<dependency>
  <groupId>org.apache.poi</groupId>
  <artifactId>poi</artifactId>
  <version>5.3.0</version>
</dependency>
<dependency>
  <groupId>org.apache.poi</groupId>
  <artifactId>poi-ooxml</artifactId>
  <version>5.3.0</version>
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
  false, // headerCaseInsensitive
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

## Spreadsheet ingestion (Excel)

The spreadsheet ingestion API mirrors the CSV flow but uses a template describing key-value labels and a tabular section per sheet.

### Example layout

- B2: **Trade Date** label; B3: value
- B5: **Settlement date** label; B6: value
- B7..H7: table headers; data rows below

### Template definition

```java
import com.example.csvparser.spreadsheet.core.template.*;

SpreadsheetTemplate template = new SpreadsheetTemplate(List.of(
  new SectionTemplate(
    "Trades",
    SheetSelector.byName("Sheet1"),
    new KeyValueBlock(
      List.of(
        new KeyValueFieldMapping("Trade Date", new RelativeCellRef(1, 0), "Trade Date", true),
        new KeyValueFieldMapping("Settlement date", new RelativeCellRef(1, 0), "Settlement date", true)
      ),
      LabelSearchOptions.defaults()
    ),
    new TableBlock(
      new HeaderLocator(7, null, null),  // header row is 1-based
      new ColumnRange("B", "H"),
      null,
      null,
      true
    )
  )
));
```

### Spreadsheet ingestion request

```java
import com.example.csvparser.spreadsheet.core.*;

SpreadsheetIngestionRequest request = new SpreadsheetIngestionRequest(
  "trades.xlsx",
  contentBytes,
  false,
  SpreadsheetOptions.defaults(),
  template,
  CsvMappingOptions.spreadsheetDefaults(),
  new CsvValidationOptions(true)
);

SpreadsheetIngestionService service = new DefaultSpreadsheetIngestionService(new JpaProcessingRegistry(repository));
IngestionResult<TradeRow> result = service.ingest(request, TradeRow.class);
```

Spreadsheet errors include sheet name and cell address in the error message for easier debugging.

## Idempotency tracking

Each processed file is recorded in `processed_csv_file` with a unique constraint on `(object_key, checksum_sha256)`. If the same object key and checksum are seen again, the ingestion returns `SKIPPED_ALREADY_PROCESSED`.

## Running tests

```bash
mvn -f /path/to/csv-parser/pom.xml test
```
