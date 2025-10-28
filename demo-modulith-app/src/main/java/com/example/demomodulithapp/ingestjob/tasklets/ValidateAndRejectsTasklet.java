package com.example.demomodulithapp.ingestjob.tasklets;

import com.example.demomodulithapp.ingestjob.config.AppProperties;
import com.example.demomodulithapp.ingestjob.domain.OrderRecord;
import com.example.demomodulithapp.ingestjob.util.CsvIO;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Component
public class ValidateAndRejectsTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(ValidateAndRejectsTasklet.class);

    private final AppProperties properties;
    private final MeterRegistry meterRegistry;

    public ValidateAndRejectsTasklet(AppProperties properties, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        @SuppressWarnings("unchecked")
        List<OrderRecord> rawRecords = (List<OrderRecord>) chunkContext.getStepContext().getStepExecution()
                .getJobExecution().getExecutionContext().get("rawRecords");
        if (CollectionUtils.isEmpty(rawRecords)) {
            log.warn("No raw records present in execution context");
            chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext()
                    .put("validRecords", new ArrayList<OrderRecord>());
            return RepeatStatus.FINISHED;
        }
        List<OrderRecord> valid = new ArrayList<>();
        List<OrderRecord> invalid = new ArrayList<>();
        List<String> reasons = new ArrayList<>();
        for (OrderRecord record : rawRecords) {
            String reason = validate(record);
            if (reason == null) {
                valid.add(new OrderRecord(
                        record.getClient(),
                        record.getSide().toUpperCase(Locale.ROOT),
                        record.getTicker(),
                        record.getPrice(),
                        record.getQuantity(),
                        record.getSourceSystem()));
            } else {
                invalid.add(record);
                reasons.add(reason);
            }
        }
        String dataDate = (String) chunkContext.getStepContext().getJobParameters().get("dataDate");
        Path rejectsPath = Path.of(properties.getIo().getRejectsPattern().replace("{dataDate}", dataDate));
        if (!invalid.isEmpty()) {
            CsvIO.writeRejects(rejectsPath, invalid, reasons);
            log.warn("Wrote {} invalid rows to {}. Sample reasons: {}", invalid.size(), rejectsPath,
                    reasons.stream().limit(3).toList());
        }
        meterRegistry.counter("ingest.records.valid").increment(valid.size());
        meterRegistry.counter("ingest.records.invalid").increment(invalid.size());
        chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext()
                .put("validRecords", (Serializable) valid);
        return RepeatStatus.FINISHED;
    }

    private String validate(OrderRecord record) {
        if (!StringUtils.hasText(record.getClient())) {
            return "client is blank";
        }
        if (!StringUtils.hasText(record.getTicker())) {
            return "ticker is blank";
        }
        if (!StringUtils.hasText(record.getSourceSystem())) {
            return "sourceSystem is blank";
        }
        if (!StringUtils.hasText(record.getSide())) {
            return "side missing";
        }
        String side = record.getSide().toUpperCase(Locale.ROOT);
        if (!side.equals("BUY") && !side.equals("SELL")) {
            return "side must be BUY or SELL";
        }
        if (record.getPrice() == null || record.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return "price must be > 0";
        }
        if (record.getQuantity() <= 0) {
            return "quantity must be > 0";
        }
        return null;
    }
}
