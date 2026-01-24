package com.example.csvparser.spreadsheet.core;

import com.example.csvparser.core.CsvMappingOptions;
import com.example.csvparser.core.CsvValidationOptions;
import com.example.csvparser.core.ErrorType;
import com.example.csvparser.core.IngestionResult;
import com.example.csvparser.core.IngestionStatus;
import com.example.csvparser.core.ParseStats;
import com.example.csvparser.core.RowError;
import com.example.csvparser.core.conversion.ConverterRegistry;
import com.example.csvparser.core.idempotency.ChecksumService;
import com.example.csvparser.core.idempotency.ProcessedCsvFile;
import com.example.csvparser.core.idempotency.ProcessingRegistry;
import com.example.csvparser.core.idempotency.ProcessingStatus;
import com.example.csvparser.core.mapping.AnnotationIntrospector;
import com.example.csvparser.core.mapping.FieldMapping;
import com.example.csvparser.core.mapping.HeaderIndex;
import com.example.csvparser.core.mapping.MappingPlan;
import com.example.csvparser.core.validation.BeanValidatorAdapter;
import com.example.csvparser.spreadsheet.adapter.poi.PoiCellExtractor;
import com.example.csvparser.spreadsheet.adapter.poi.PoiLabelFinder;
import com.example.csvparser.spreadsheet.adapter.poi.PoiTableReader;
import com.example.csvparser.spreadsheet.adapter.poi.PoiWorkbookLoader;
import com.example.csvparser.spreadsheet.adapter.poi.TableData;
import com.example.csvparser.spreadsheet.core.mapping.KeyValueApplier;
import com.example.csvparser.spreadsheet.core.mapping.KeyValueCell;
import com.example.csvparser.spreadsheet.core.mapping.KeyValueExtractionResult;
import com.example.csvparser.spreadsheet.core.mapping.SpreadsheetCellConverter;
import com.example.csvparser.spreadsheet.core.mapping.SpreadsheetRowMapper;
import com.example.csvparser.spreadsheet.core.mapping.SpreadsheetRowMappingResult;
import com.example.csvparser.spreadsheet.core.template.KeyValueBlock;
import com.example.csvparser.spreadsheet.core.template.KeyValueFieldMapping;
import com.example.csvparser.spreadsheet.core.template.LabelSearchOptions;
import com.example.csvparser.spreadsheet.core.template.SectionTemplate;
import com.example.csvparser.spreadsheet.core.template.SheetSelector;
import com.example.csvparser.spreadsheet.core.template.SheetSelectorType;
import com.example.csvparser.spreadsheet.core.template.SpreadsheetTemplate;
import com.example.csvparser.spreadsheet.core.template.TableBlock;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellAddress;
import org.springframework.stereotype.Service;

import jakarta.validation.ConstraintViolation;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DefaultSpreadsheetIngestionService implements SpreadsheetIngestionService {
    private final ProcessingRegistry processingRegistry;
    private final ChecksumService checksumService;
    private final AnnotationIntrospector annotationIntrospector;
    private final ConverterRegistry converterRegistry;
    private final PoiWorkbookLoader workbookLoader;
    private final PoiCellExtractor cellExtractor;
    private final PoiLabelFinder labelFinder;
    private final PoiTableReader tableReader;

    public DefaultSpreadsheetIngestionService(ProcessingRegistry processingRegistry) {
        this(processingRegistry,
                new ChecksumService(),
                new AnnotationIntrospector(),
                new ConverterRegistry(),
                new PoiWorkbookLoader(),
                new PoiCellExtractor());
    }

    public DefaultSpreadsheetIngestionService(
            ProcessingRegistry processingRegistry,
            ChecksumService checksumService,
            AnnotationIntrospector annotationIntrospector,
            ConverterRegistry converterRegistry,
            PoiWorkbookLoader workbookLoader,
            PoiCellExtractor cellExtractor
    ) {
        this.processingRegistry = processingRegistry;
        this.checksumService = checksumService;
        this.annotationIntrospector = annotationIntrospector;
        this.converterRegistry = converterRegistry;
        this.workbookLoader = workbookLoader;
        this.cellExtractor = cellExtractor;
        this.labelFinder = new PoiLabelFinder(cellExtractor);
        this.tableReader = new PoiTableReader(cellExtractor, labelFinder);
    }

    @Override
    public <T> IngestionResult<T> ingest(SpreadsheetIngestionRequest request, Class<T> targetType) {
        long start = System.currentTimeMillis();
        SpreadsheetOptions options = request.spreadsheetOptions() == null
                ? SpreadsheetOptions.defaults()
                : request.spreadsheetOptions();
        CsvMappingOptions mappingOptions = request.mappingOptions() == null
                ? CsvMappingOptions.spreadsheetDefaults()
                : request.mappingOptions();
        CsvValidationOptions validationOptions = request.validationOptions() == null
                ? CsvValidationOptions.defaults(isValidatorAvailable())
                : request.validationOptions();

        String checksum = checksumService.sha256(request.content());
        if (!request.forceReprocess()) {
            Optional<ProcessedCsvFile> processed = processingRegistry.findProcessed(request.objectKey(), checksum);
            if (processed.isPresent() && processed.get().getStatus() == ProcessingStatus.PROCESSED) {
                return new IngestionResult<>(
                        IngestionStatus.SKIPPED_ALREADY_PROCESSED,
                        request.objectKey(),
                        checksum,
                        List.of(),
                        List.of(),
                        List.of(),
                        new ParseStats(0, 0, 0, System.currentTimeMillis() - start)
                );
            }
        }

        List<RowError> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<T> records = new ArrayList<>();
        long totalRows = 0;

        SpreadsheetTemplate template = request.template();
        if (template == null || template.sections() == null || template.sections().isEmpty()) {
            errors.add(new RowError(1, null, null, "Missing spreadsheet template", ErrorType.HEADER));
            ParseStats stats = new ParseStats(0, 0, errors.size(), System.currentTimeMillis() - start);
            recordProcessing(request, checksum, stats, ProcessingStatus.FAILED);
            return new IngestionResult<>(IngestionStatus.FAILED, request.objectKey(), checksum, records, errors, warnings, stats);
        }

        try (Workbook workbook = workbookLoader.load(request.content())) {
            FormulaEvaluator evaluator = cellExtractor.createFormulaEvaluator(workbook);
            MappingPlan mappingPlan = annotationIntrospector.introspect(targetType, mappingOptions);
            SpreadsheetCellConverter cellConverter = new SpreadsheetCellConverter(converterRegistry, cellExtractor);
            SpreadsheetRowMapper<T> rowMapper = new SpreadsheetRowMapper<>(cellConverter);
            KeyValueApplier keyValueApplier = new KeyValueApplier(cellConverter);
            BeanValidatorAdapter validatorAdapter = new BeanValidatorAdapter(validationOptions.beanValidationEnabled());

            for (SectionTemplate section : template.sections()) {
                if (section == null) {
                    continue;
                }
                for (Sheet sheet : resolveSheets(workbook, section, options)) {
                    String sectionPrefix = formatSectionPrefix(section.sectionName());
                    String contextPrefix = sectionPrefix + "[Sheet: " + sheet.getSheetName() + "] ";

                    KeyValueExtractionResult keyValueResult = extractKeyValues(
                            section.keyValueBlock(),
                            mappingPlan,
                            mappingOptions,
                            sheet,
                            evaluator,
                            contextPrefix
                    );
                    errors.addAll(keyValueResult.errors());

                    TableBlock tableBlock = section.tableBlock();
                    if (tableBlock == null) {
                        continue;
                    }

                    Optional<TableData> tableData = tableReader.readTable(sheet, tableBlock, evaluator);
                    if (tableData.isEmpty()) {
                        errors.add(new RowError(
                                1,
                                null,
                                null,
                                contextPrefix + "Unable to resolve table header",
                                ErrorType.HEADER
                        ));
                        continue;
                    }

                    TableData table = tableData.get();
                    totalRows += table.rows().size();
                    HeaderIndex headerIndex = new HeaderIndex(table.headers().toArray(new String[0]), mappingOptions);
                    warnings.addAll(findUnknownHeaders(table.headers(), mappingPlan, mappingOptions, contextPrefix));
                    errors.addAll(validateRequiredHeaders(
                            mappingPlan,
                            headerIndex,
                            mappingOptions,
                            table.headerRowNumber(),
                            contextPrefix,
                            keyValueResult.mappedFields()
                    ));

                    for (TableData.RowData row : table.rows()) {
                        SpreadsheetRowMappingResult<T> mappingResult = rowMapper.mapRow(
                                row,
                                mappingPlan,
                                headerIndex,
                                mappingOptions,
                                targetType,
                                evaluator,
                                sheet.getSheetName(),
                                contextPrefix
                        );
                        if (!mappingResult.errors().isEmpty()) {
                            errors.addAll(mappingResult.errors());
                            continue;
                        }
                        if (mappingResult.mappedObject() == null) {
                            continue;
                        }

                        List<RowError> keyValueErrors = keyValueApplier.applyKeyValues(
                                mappingResult.mappedObject(),
                                keyValueResult.keyValueCells(),
                                evaluator,
                                row.rowNumber(),
                                contextPrefix
                        );
                        if (!keyValueErrors.isEmpty()) {
                            errors.addAll(keyValueErrors);
                            continue;
                        }

                        Set<ConstraintViolation<T>> violations = validatorAdapter.validate(mappingResult.mappedObject());
                        if (!violations.isEmpty()) {
                            errors.addAll(toValidationErrors(violations, row.rowNumber(), contextPrefix));
                        } else {
                            records.add(mappingResult.mappedObject());
                        }
                    }
                }
            }
        } catch (Exception exception) {
            errors.add(new RowError(1, null, null, "Failed to parse spreadsheet: " + exception.getMessage(), ErrorType.PARSE));
        }

        ParseStats stats = new ParseStats(totalRows, records.size(), errors.size(), System.currentTimeMillis() - start);
        ProcessingStatus processingStatus = records.isEmpty() ? ProcessingStatus.FAILED : ProcessingStatus.PROCESSED;
        recordProcessing(request, checksum, stats, processingStatus);

        IngestionStatus status = records.isEmpty() ? IngestionStatus.FAILED : IngestionStatus.PROCESSED;
        return new IngestionResult<>(status, request.objectKey(), checksum, records, errors, warnings, stats);
    }

    private List<Sheet> resolveSheets(Workbook workbook, SectionTemplate section, SpreadsheetOptions options) {
        List<Sheet> candidates = new ArrayList<>();
        if (options.sheetSelectionMode() == SheetSelectionMode.ALL) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                candidates.add(workbook.getSheetAt(i));
            }
        } else if (options.sheetSelectionMode() == SheetSelectionMode.NAMES) {
            for (String name : options.sheetNames()) {
                Sheet sheet = workbook.getSheet(name);
                if (sheet != null) {
                    candidates.add(sheet);
                }
            }
        } else if (options.sheetSelectionMode() == SheetSelectionMode.INDEXES) {
            for (Integer index : options.sheetIndexes()) {
                if (index != null && index >= 0 && index < workbook.getNumberOfSheets()) {
                    candidates.add(workbook.getSheetAt(index));
                }
            }
        } else {
            candidates.addAll(resolveSectionSheets(workbook, section.sheetSelector()));
        }

        if (options.sheetSelectionMode() != SheetSelectionMode.TEMPLATE && section.sheetSelector() != null) {
            List<Sheet> filtered = resolveSectionSheets(workbook, section.sheetSelector());
            if (!filtered.isEmpty()) {
                candidates.retainAll(filtered);
            }
        }

        return candidates;
    }

    private List<Sheet> resolveSectionSheets(Workbook workbook, SheetSelector selector) {
        List<Sheet> sheets = new ArrayList<>();
        if (selector == null || selector.type() == SheetSelectorType.ANY) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                sheets.add(workbook.getSheetAt(i));
            }
            return sheets;
        }
        if (selector.type() == SheetSelectorType.BY_NAME && selector.sheetName() != null) {
            Sheet sheet = workbook.getSheet(selector.sheetName());
            if (sheet != null) {
                sheets.add(sheet);
            }
        }
        if (selector.type() == SheetSelectorType.BY_INDEX && selector.sheetIndex() != null) {
            int index = selector.sheetIndex();
            if (index >= 0 && index < workbook.getNumberOfSheets()) {
                sheets.add(workbook.getSheetAt(index));
            }
        }
        return sheets;
    }

    private KeyValueExtractionResult extractKeyValues(
            KeyValueBlock keyValueBlock,
            MappingPlan mappingPlan,
            CsvMappingOptions options,
            Sheet sheet,
            FormulaEvaluator evaluator,
            String contextPrefix
    ) {
        if (keyValueBlock == null || keyValueBlock.fields() == null || keyValueBlock.fields().isEmpty()) {
            return new KeyValueExtractionResult(List.of(), List.of(), Set.of());
        }
        List<RowError> errors = new ArrayList<>();
        List<KeyValueCell> keyValueCells = new ArrayList<>();
        Set<FieldMapping> mappedFields = new HashSet<>();

        for (KeyValueFieldMapping fieldMapping : keyValueBlock.fields()) {
            if (fieldMapping == null) {
                continue;
            }
            FieldMapping mapping = resolveFieldMapping(fieldMapping.targetFieldName(), mappingPlan, options);
            if (mapping == null) {
                errors.add(new RowError(
                        1,
                        fieldMapping.labelText(),
                        null,
                        contextPrefix + "Unknown target field: " + fieldMapping.targetFieldName(),
                        ErrorType.HEADER
                ));
                continue;
            }
            mappedFields.add(mapping);
            LabelSearchOptions labelOptions = keyValueBlock.labelSearchOptions() == null
                    ? LabelSearchOptions.defaults()
                    : keyValueBlock.labelSearchOptions();
            Optional<Cell> labelCell = labelFinder.findLabelCell(sheet, fieldMapping.labelText(), labelOptions, evaluator);
            if (labelCell.isEmpty()) {
                if (fieldMapping.required()) {
                    errors.add(new RowError(
                            1,
                            fieldMapping.labelText(),
                            null,
                            contextPrefix + "Missing required label (sheet=" + sheet.getSheetName() + ")",
                            ErrorType.HEADER
                    ));
                }
                continue;
            }
            Cell label = labelCell.get();
            int valueRow = label.getRowIndex() + fieldMapping.valueRef().rowOffset();
            int valueCol = label.getColumnIndex() + fieldMapping.valueRef().colOffset();
            Cell valueCell = sheet.getRow(valueRow) == null ? null : sheet.getRow(valueRow).getCell(valueCol);
            String valueAddress = valueCell == null
                    ? new CellAddress(valueRow, valueCol).formatAsString()
                    : cellExtractor.cellAddress(valueCell);
            if (valueCell == null && fieldMapping.required()) {
                errors.add(new RowError(
                        valueRow + 1,
                        fieldMapping.labelText(),
                        null,
                        contextPrefix + "Missing required value (sheet=" + sheet.getSheetName() + ", cell=" + valueAddress + ")",
                        ErrorType.HEADER
                ));
                continue;
            }
            keyValueCells.add(new KeyValueCell(mapping, valueCell, fieldMapping.labelText(), sheet.getSheetName(), valueAddress));
        }
        return new KeyValueExtractionResult(keyValueCells, errors, mappedFields);
    }

    private FieldMapping resolveFieldMapping(String targetFieldName, MappingPlan mappingPlan, CsvMappingOptions options) {
        if (targetFieldName == null) {
            return null;
        }
        String normalizedTarget = normalizeHeader(targetFieldName, options);
        for (FieldMapping mapping : mappingPlan.fieldMappings()) {
            String headerName = normalizeHeader(mapping.headerName(), options);
            if (headerName.equals(normalizedTarget)) {
                return mapping;
            }
        }
        for (FieldMapping mapping : mappingPlan.fieldMappings()) {
            String fieldName = normalizeHeader(mapping.field().getName(), options);
            if (fieldName.equals(normalizedTarget)) {
                return mapping;
            }
        }
        return null;
    }

    private List<RowError> validateRequiredHeaders(
            MappingPlan mappingPlan,
            HeaderIndex headerIndex,
            CsvMappingOptions options,
            int headerRowNumber,
            String contextPrefix,
            Set<FieldMapping> excludedFields
    ) {
        List<RowError> headerErrors = new ArrayList<>();
        for (FieldMapping mapping : mappingPlan.fieldMappings()) {
            if (excludedFields.contains(mapping)) {
                continue;
            }
            if (mapping.required() && headerIndex.indexOf(mapping.headerName(), options) == null) {
                headerErrors.add(new RowError(
                        headerRowNumber,
                        mapping.headerName(),
                        null,
                        contextPrefix + "Missing required header",
                        ErrorType.HEADER
                ));
            }
        }
        return headerErrors;
    }

    private List<String> findUnknownHeaders(
            List<String> headers,
            MappingPlan mappingPlan,
            CsvMappingOptions options,
            String contextPrefix
    ) {
        Set<String> mapped = mappingPlan.fieldMappings().stream()
                .map(FieldMapping::headerName)
                .map(name -> normalizeHeader(name, options))
                .collect(Collectors.toSet());
        Set<String> unknown = new HashSet<>();
        for (String header : headers) {
            String normalized = normalizeHeader(header, options);
            if (!mapped.contains(normalized)) {
                unknown.add(header);
            }
        }
        if (unknown.isEmpty()) {
            return List.of();
        }
        return unknown.stream()
                .map(name -> contextPrefix + "Unknown column: " + name)
                .toList();
    }

    private String normalizeHeader(String header, CsvMappingOptions options) {
        String normalized = header == null ? "" : header;
        if (options.trimHeaders()) {
            normalized = normalized.trim();
        }
        if (options.headerCaseInsensitive() || !options.exactHeaderMatch()) {
            normalized = normalized.toLowerCase(Locale.ROOT);
        }
        return normalized;
    }

    private <T> List<RowError> toValidationErrors(Set<ConstraintViolation<T>> violations, long rowNumber, String contextPrefix) {
        List<RowError> validationErrors = new ArrayList<>();
        for (ConstraintViolation<T> violation : violations) {
            String column = violation.getPropertyPath() == null ? null : violation.getPropertyPath().toString();
            validationErrors.add(new RowError(
                    rowNumber,
                    column,
                    null,
                    contextPrefix + violation.getMessage(),
                    ErrorType.VALIDATION
            ));
        }
        return validationErrors;
    }

    private boolean isValidatorAvailable() {
        try {
            Class.forName("jakarta.validation.Validation");
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    private String formatSectionPrefix(String sectionName) {
        if (sectionName == null || sectionName.isBlank()) {
            return "";
        }
        return "[Section: " + sectionName + "] ";
    }

    private void recordProcessing(SpreadsheetIngestionRequest request, String checksum, ParseStats stats, ProcessingStatus status) {
        ProcessedCsvFile processed = new ProcessedCsvFile();
        processed.setObjectKey(request.objectKey());
        processed.setChecksumSha256(checksum);
        processed.setStatus(status);
        processed.setProcessedAt(Instant.now());
        processed.setSuccessCount((int) stats.successCount());
        processed.setErrorCount((int) stats.errorCount());
        processingRegistry.record(processed);
    }
}
