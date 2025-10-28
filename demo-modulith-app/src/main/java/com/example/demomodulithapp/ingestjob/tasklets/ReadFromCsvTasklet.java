package com.example.demomodulithapp.ingestjob.tasklets;

import com.example.demomodulithapp.ingestjob.config.AppProperties;
import com.example.demomodulithapp.ingestjob.domain.OrderRecord;
import com.example.demomodulithapp.ingestjob.util.CsvIO;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ReadFromCsvTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(ReadFromCsvTasklet.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final AppProperties properties;
    private final MeterRegistry meterRegistry;

    public ReadFromCsvTasklet(AppProperties properties, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        String dataDateValue = (String) chunkContext.getStepContext().getJobParameters().get("dataDate");
        if (!StringUtils.hasText(dataDateValue)) {
            throw new IllegalStateException("Missing dataDate job parameter");
        }
        LocalDate dataDate = LocalDate.parse(dataDateValue, FORMATTER);
        String inputPattern = properties.getIo().getInputPattern().replace("{dataDate}", dataDateValue);
        Path inputPath = Path.of(inputPattern);
        if (!Files.exists(inputPath)) {
            log.error("Input file {} does not exist", inputPath);
            throw new IOException("Input file missing: " + inputPath);
        }
        List<OrderRecord> rawRecords = CsvIO.readOrders(inputPath);
        chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext()
                .put("rawRecords", (Serializable) rawRecords);
        meterRegistry.counter("ingest.records.total").increment(rawRecords.size());
        log.info("Loaded {} raw records for {} from {}", rawRecords.size(), dataDate, inputPath);
        return RepeatStatus.FINISHED;
    }
}
