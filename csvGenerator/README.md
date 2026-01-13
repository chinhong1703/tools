# CSV Generator

## Overview
This module provides a reusable, streaming CSV generator designed for large files (1GB+). It supports multiple file types with different headers and formats without reimplementing per file type. The generator streams rows via `Iterator`/`Iterable` and writes to disk using buffered I/O with constant memory usage.

## Adding a New File Type
1. Add a new constant to `CsvFileType` with:
   - `headers`: ordered CSV header names.
   - `rowMapper`: maps your row object to a `String[]` aligned with the headers.
   - `formatSettings`: delimiter, quoting, charset, line ending, null policy.
   - `namingConfig`: output directory and base file naming settings.
2. Provide a row model (record/class) if needed.
3. Use `CsvGenerationRequest<>(CsvFileType.YOUR_TYPE, iterable)`.

No changes are required in `CsvGenerator` or `CsvGeneratorClient`.

## Design Summary (Minimal Production-Oriented)
- `CsvFileType` (enum): file-specific headers, row mapper, CSV settings, and naming rules.
- `CsvRowMapper<T>`: maps a row to a `String[]`.
- `CsvWriter`: low-level writer with proper escaping/quoting.
- `CsvGenerator`: orchestrates streaming, splitting, atomic writes, error handling, metrics/logging.
- `CsvGeneratorClient`: facade for generating multiple file types, optional parallelism.
- `MetricsSink` + `NoopMetricsSink`: metrics hooks without external dependencies.
- `StructuredLogger`: key=value structured logging helper.

## Usage Notes
- Files are written to a temp file and atomically moved on success.
- Splitting is optional and controlled by `CsvGeneratorOptions`.
- Errors per-row are logged, counted, and skipped until `maxSkips` is exceeded.

