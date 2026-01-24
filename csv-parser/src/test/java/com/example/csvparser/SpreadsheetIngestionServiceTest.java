package com.example.csvparser;

import com.example.csvparser.core.CsvMappingOptions;
import com.example.csvparser.core.CsvValidationOptions;
import com.example.csvparser.core.IngestionResult;
import com.example.csvparser.core.IngestionStatus;
import com.example.csvparser.core.idempotency.JpaProcessingRegistry;
import com.example.csvparser.core.idempotency.ProcessedCsvFileRepository;
import com.example.csvparser.core.mapping.CsvColumn;
import com.example.csvparser.spreadsheet.core.DefaultSpreadsheetIngestionService;
import com.example.csvparser.spreadsheet.core.SpreadsheetIngestionRequest;
import com.example.csvparser.spreadsheet.core.SpreadsheetOptions;
import com.example.csvparser.spreadsheet.core.template.ColumnRange;
import com.example.csvparser.spreadsheet.core.template.HeaderLocator;
import com.example.csvparser.spreadsheet.core.template.KeyValueBlock;
import com.example.csvparser.spreadsheet.core.template.KeyValueFieldMapping;
import com.example.csvparser.spreadsheet.core.template.LabelSearchOptions;
import com.example.csvparser.spreadsheet.core.template.RelativeCellRef;
import com.example.csvparser.spreadsheet.core.template.SectionTemplate;
import com.example.csvparser.spreadsheet.core.template.SheetSelector;
import com.example.csvparser.spreadsheet.core.template.SpreadsheetTemplate;
import com.example.csvparser.spreadsheet.core.template.TableBlock;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SpreadsheetIngestionServiceTest {
    @Autowired
    private ProcessedCsvFileRepository repository;

    @Test
    void ingestsSingleSectionWithKeyValuesAndTable() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Sheet1");
            setLabelAndValue(sheet, 1, 1, "Trade Date", LocalDate.of(2025, 11, 15));
            setLabelAndValue(sheet, 4, 1, "Settlement date", LocalDate.of(2025, 11, 20));

            Row headerRow = sheet.createRow(6);
            headerRow.createCell(1).setCellValue("Price");
            headerRow.createCell(2).setCellValue("Quantity");
            headerRow.createCell(3).setCellValue("Stock");

            Row row1 = sheet.createRow(7);
            row1.createCell(1).setCellValue(12.5);
            row1.createCell(2).setCellValue(10);
            row1.createCell(3).setCellValue("ABC");

            Row row2 = sheet.createRow(8);
            row2.createCell(1).setCellValue(15.0);
            row2.createCell(2).setCellValue(20);
            row2.createCell(3).setCellValue("XYZ");

            SpreadsheetTemplate template = new SpreadsheetTemplate(List.of(sectionTemplate("Sheet1")));
            SpreadsheetIngestionRequest request = new SpreadsheetIngestionRequest(
                    "trades.xlsx",
                    toBytes(workbook),
                    false,
                    SpreadsheetOptions.defaults(),
                    template,
                    CsvMappingOptions.spreadsheetDefaults(),
                    new CsvValidationOptions(true)
            );

            DefaultSpreadsheetIngestionService service = new DefaultSpreadsheetIngestionService(new JpaProcessingRegistry(repository));
            IngestionResult<TradeRow> result = service.ingest(request, TradeRow.class);

            assertThat(result.status()).isEqualTo(IngestionStatus.PROCESSED);
            assertThat(result.records()).hasSize(2);
            assertThat(result.records().get(0).tradeDate()).isEqualTo(LocalDate.of(2025, 11, 15));
            assertThat(result.records().get(0).settlementDate()).isEqualTo(LocalDate.of(2025, 11, 20));
            assertThat(result.errors()).isEmpty();
        }
    }

    @Test
    void ingestsMultipleSheetsWithSections() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            var sheet1 = workbook.createSheet("Sheet1");
            setLabelAndValue(sheet1, 1, 1, "Trade Date", LocalDate.of(2025, 11, 15));
            setLabelAndValue(sheet1, 4, 1, "Settlement date", LocalDate.of(2025, 11, 20));
            createTable(sheet1, "AAA");

            var sheet2 = workbook.createSheet("Sheet2");
            setLabelAndValue(sheet2, 1, 1, "Trade Date", LocalDate.of(2025, 12, 1));
            setLabelAndValue(sheet2, 4, 1, "Settlement date", LocalDate.of(2025, 12, 5));
            createTable(sheet2, "BBB");

            SpreadsheetTemplate template = new SpreadsheetTemplate(List.of(
                    sectionTemplate("Sheet1"),
                    sectionTemplate("Sheet2")
            ));
            SpreadsheetIngestionRequest request = new SpreadsheetIngestionRequest(
                    "multi.xlsx",
                    toBytes(workbook),
                    false,
                    SpreadsheetOptions.defaults(),
                    template,
                    CsvMappingOptions.spreadsheetDefaults(),
                    new CsvValidationOptions(true)
            );

            DefaultSpreadsheetIngestionService service = new DefaultSpreadsheetIngestionService(new JpaProcessingRegistry(repository));
            IngestionResult<TradeRow> result = service.ingest(request, TradeRow.class);

            assertThat(result.records()).hasSize(2);
            assertThat(result.records().get(1).stock()).isEqualTo("BBB");
            assertThat(result.errors()).isEmpty();
        }
    }

    @Test
    void missingRequiredLabelProducesError() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Sheet1");
            setLabelAndValue(sheet, 1, 1, "Trade Date", LocalDate.of(2025, 11, 15));
            createTable(sheet, "AAA");

            SpreadsheetTemplate template = new SpreadsheetTemplate(List.of(sectionTemplate("Sheet1")));
            SpreadsheetIngestionRequest request = new SpreadsheetIngestionRequest(
                    "missing-label.xlsx",
                    toBytes(workbook),
                    false,
                    SpreadsheetOptions.defaults(),
                    template,
                    CsvMappingOptions.spreadsheetDefaults(),
                    new CsvValidationOptions(true)
            );

            DefaultSpreadsheetIngestionService service = new DefaultSpreadsheetIngestionService(new JpaProcessingRegistry(repository));
            IngestionResult<TradeRow> result = service.ingest(request, TradeRow.class);

            assertThat(result.errors())
                    .anyMatch(error -> "Settlement date".equals(error.columnName()));
        }
    }

    @Test
    void headerMatchingIsCaseInsensitiveWithTrim() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Sheet1");
            setLabelAndValue(sheet, 1, 1, "Trade Date", LocalDate.of(2025, 11, 15));
            setLabelAndValue(sheet, 4, 1, "Settlement date", LocalDate.of(2025, 11, 20));

            Row headerRow = sheet.createRow(6);
            headerRow.createCell(1).setCellValue(" price ");
            headerRow.createCell(2).setCellValue("Quantity");
            headerRow.createCell(3).setCellValue("Stock");

            Row row1 = sheet.createRow(7);
            row1.createCell(1).setCellValue(12.5);
            row1.createCell(2).setCellValue(10);
            row1.createCell(3).setCellValue("AAA");

            SpreadsheetTemplate template = new SpreadsheetTemplate(List.of(sectionTemplate("Sheet1")));
            CsvMappingOptions options = new CsvMappingOptions(true, true, true, true, CsvMappingOptions.defaults().datePatterns());
            SpreadsheetIngestionRequest request = new SpreadsheetIngestionRequest(
                    "case-insensitive.xlsx",
                    toBytes(workbook),
                    false,
                    SpreadsheetOptions.defaults(),
                    template,
                    options,
                    new CsvValidationOptions(true)
            );

            DefaultSpreadsheetIngestionService service = new DefaultSpreadsheetIngestionService(new JpaProcessingRegistry(repository));
            IngestionResult<TradeRow> result = service.ingest(request, TradeRow.class);

            assertThat(result.records()).hasSize(1);
            assertThat(result.errors()).isEmpty();
        }
    }

    @Test
    void invalidDateInKeyValueProducesConversionError() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Sheet1");
            Row labelRow = sheet.createRow(1);
            labelRow.createCell(1).setCellValue("Trade Date");
            Row valueRow = sheet.createRow(2);
            valueRow.createCell(1).setCellValue("notadate");
            setLabelAndValue(sheet, 4, 1, "Settlement date", LocalDate.of(2025, 11, 20));
            createTable(sheet, "AAA");

            SpreadsheetTemplate template = new SpreadsheetTemplate(List.of(sectionTemplate("Sheet1")));
            SpreadsheetIngestionRequest request = new SpreadsheetIngestionRequest(
                    "invalid-date.xlsx",
                    toBytes(workbook),
                    false,
                    SpreadsheetOptions.defaults(),
                    template,
                    CsvMappingOptions.spreadsheetDefaults(),
                    new CsvValidationOptions(true)
            );

            DefaultSpreadsheetIngestionService service = new DefaultSpreadsheetIngestionService(new JpaProcessingRegistry(repository));
            IngestionResult<TradeRow> result = service.ingest(request, TradeRow.class);

            assertThat(result.errors())
                    .anyMatch(error -> "Trade Date".equals(error.columnName()) && error.rawValue() != null);
        }
    }

    @Test
    void evaluatesFormulasInTableCells() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Sheet1");
            setLabelAndValue(sheet, 1, 1, "Trade Date", LocalDate.of(2025, 11, 15));
            setLabelAndValue(sheet, 4, 1, "Settlement date", LocalDate.of(2025, 11, 20));

            Row headerRow = sheet.createRow(6);
            headerRow.createCell(1).setCellValue("Price");
            headerRow.createCell(2).setCellValue("Quantity");
            headerRow.createCell(3).setCellValue("Stock");

            Row row1 = sheet.createRow(7);
            row1.createCell(1).setCellFormula("1+2");
            row1.createCell(2).setCellValue(10);
            row1.createCell(3).setCellValue("ABC");

            SpreadsheetTemplate template = new SpreadsheetTemplate(List.of(sectionTemplate("Sheet1")));
            SpreadsheetIngestionRequest request = new SpreadsheetIngestionRequest(
                    "formula.xlsx",
                    toBytes(workbook),
                    false,
                    SpreadsheetOptions.defaults(),
                    template,
                    CsvMappingOptions.spreadsheetDefaults(),
                    new CsvValidationOptions(true)
            );

            DefaultSpreadsheetIngestionService service = new DefaultSpreadsheetIngestionService(new JpaProcessingRegistry(repository));
            IngestionResult<TradeRow> result = service.ingest(request, TradeRow.class);

            assertThat(result.records()).hasSize(1);
            assertThat(result.records().get(0).price()).isEqualTo(BigDecimal.valueOf(3.0));
        }
    }

    @Test
    void respectsIdempotencyForSpreadsheet() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Sheet1");
            setLabelAndValue(sheet, 1, 1, "Trade Date", LocalDate.of(2025, 11, 15));
            setLabelAndValue(sheet, 4, 1, "Settlement date", LocalDate.of(2025, 11, 20));
            createTable(sheet, "AAA");

            SpreadsheetTemplate template = new SpreadsheetTemplate(List.of(sectionTemplate("Sheet1")));
            SpreadsheetIngestionRequest request = new SpreadsheetIngestionRequest(
                    "idempotent.xlsx",
                    toBytes(workbook),
                    false,
                    SpreadsheetOptions.defaults(),
                    template,
                    CsvMappingOptions.spreadsheetDefaults(),
                    new CsvValidationOptions(true)
            );

            DefaultSpreadsheetIngestionService service = new DefaultSpreadsheetIngestionService(new JpaProcessingRegistry(repository));
            IngestionResult<TradeRow> first = service.ingest(request, TradeRow.class);
            IngestionResult<TradeRow> second = service.ingest(request, TradeRow.class);

            assertThat(first.status()).isEqualTo(IngestionStatus.PROCESSED);
            assertThat(second.status()).isEqualTo(IngestionStatus.SKIPPED_ALREADY_PROCESSED);
        }
    }

    private SectionTemplate sectionTemplate(String sheetName) {
        KeyValueBlock keyValueBlock = new KeyValueBlock(
                List.of(
                        new KeyValueFieldMapping("Trade Date", new RelativeCellRef(1, 0), "Trade Date", true),
                        new KeyValueFieldMapping("Settlement date", new RelativeCellRef(1, 0), "Settlement date", true)
                ),
                LabelSearchOptions.defaults()
        );
        TableBlock tableBlock = new TableBlock(
                new HeaderLocator(7, null, null),
                new ColumnRange("B", "D"),
                null,
                null,
                true
        );
        return new SectionTemplate("Trades", SheetSelector.byName(sheetName), keyValueBlock, tableBlock);
    }

    private void createTable(org.apache.poi.ss.usermodel.Sheet sheet, String stockValue) {
        Row headerRow = sheet.createRow(6);
        headerRow.createCell(1).setCellValue("Price");
        headerRow.createCell(2).setCellValue("Quantity");
        headerRow.createCell(3).setCellValue("Stock");

        Row row = sheet.createRow(7);
        row.createCell(1).setCellValue(12.5);
        row.createCell(2).setCellValue(10);
        row.createCell(3).setCellValue(stockValue);
    }

    private void setLabelAndValue(org.apache.poi.ss.usermodel.Sheet sheet, int labelRowIndex, int columnIndex, String label, LocalDate value) {
        Row labelRow = sheet.createRow(labelRowIndex);
        labelRow.createCell(columnIndex).setCellValue(label);
        Row valueRow = sheet.getRow(labelRowIndex + 1);
        if (valueRow == null) {
            valueRow = sheet.createRow(labelRowIndex + 1);
        }
        Cell cell = valueRow.createCell(columnIndex);
        cell.setCellValue(Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant()));
        CellStyle style = sheet.getWorkbook().createCellStyle();
        style.setDataFormat(sheet.getWorkbook().getCreationHelper().createDataFormat().getFormat("m/d/yy"));
        cell.setCellStyle(style);
    }

    private byte[] toBytes(Workbook workbook) throws Exception {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    static class TradeRow {
        @CsvColumn(name = "Trade Date", required = true)
        private LocalDate tradeDate;

        @CsvColumn(name = "Settlement date", required = true)
        private LocalDate settlementDate;

        @CsvColumn(name = "Price", required = true)
        private BigDecimal price;

        @CsvColumn(name = "Quantity", required = true)
        private Integer quantity;

        @CsvColumn(name = "Stock", required = true)
        private String stock;

        public LocalDate tradeDate() {
            return tradeDate;
        }

        public LocalDate settlementDate() {
            return settlementDate;
        }

        public BigDecimal price() {
            return price;
        }

        public Integer quantity() {
            return quantity;
        }

        public String stock() {
            return stock;
        }
    }
}
