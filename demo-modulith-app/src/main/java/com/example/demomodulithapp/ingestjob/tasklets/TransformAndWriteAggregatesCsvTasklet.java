package com.example.demomodulithapp.ingestjob.tasklets;

import com.example.demomodulithapp.ingestjob.config.AppProperties;
import com.example.demomodulithapp.ingestjob.domain.AggregatedOrder;
import com.example.demomodulithapp.ingestjob.domain.OrderRecord;
import com.example.demomodulithapp.ingestjob.util.CsvIO;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class TransformAndWriteAggregatesCsvTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(TransformAndWriteAggregatesCsvTasklet.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final MathContext MATH_CONTEXT = new MathContext(20, RoundingMode.HALF_UP);

    private final AppProperties properties;
    private final MeterRegistry meterRegistry;

    public TransformAndWriteAggregatesCsvTasklet(AppProperties properties, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        String dataDateValue = (String) chunkContext.getStepContext().getJobParameters().get("dataDate");
        LocalDate dataDate = LocalDate.parse(dataDateValue, FORMATTER);
        @SuppressWarnings("unchecked")
        List<OrderRecord> validRecords = (List<OrderRecord>) chunkContext.getStepContext().getStepExecution()
                .getJobExecution().getExecutionContext().get("validRecords");
        if (CollectionUtils.isEmpty(validRecords)) {
            log.info("No valid records to aggregate");
            chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext()
                    .put("aggregates", new ArrayList<AggregatedOrder>());
            return RepeatStatus.FINISHED;
        }
        Map<String, AggregateAccumulator> accumulatorMap = new LinkedHashMap<>();
        for (OrderRecord record : validRecords) {
            if (!"colocated".equalsIgnoreCase(record.getSourceSystem())) {
                continue;
            }
            String key = record.getClient() + "|" + record.getSide().toUpperCase(Locale.ROOT) + "|" + record.getTicker();
            accumulatorMap.computeIfAbsent(key, k -> new AggregateAccumulator(record.getClient(),
                    record.getSide().toUpperCase(Locale.ROOT), record.getTicker()))
                    .accumulate(record.getPrice(), record.getQuantity());
        }
        List<AggregatedOrder> aggregates = new ArrayList<>();
        for (AggregateAccumulator accumulator : accumulatorMap.values()) {
            if (accumulator.totalQuantity == 0L) {
                continue;
            }
            BigDecimal vwap = accumulator.totalPriceQuantity.divide(BigDecimal.valueOf(accumulator.totalQuantity),
                    8, RoundingMode.HALF_UP);
            aggregates.add(new AggregatedOrder(dataDate, accumulator.client, accumulator.side,
                    accumulator.ticker, accumulator.totalQuantity, vwap));
        }
        Path aggregatesPath = Path.of(properties.getIo().getAggregatesPattern().replace("{dataDate}", dataDateValue));
        CsvIO.writeAggregates(aggregatesPath, aggregates);
        meterRegistry.counter("aggregates.rows.out").increment(aggregates.size());
        log.info("Wrote {} aggregates to {}", aggregates.size(), aggregatesPath);
        chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext()
                .put("aggregates", (Serializable) aggregates);
        return RepeatStatus.FINISHED;
    }

    private static final class AggregateAccumulator {
        private final String client;
        private final String side;
        private final String ticker;
        private long totalQuantity;
        private BigDecimal totalPriceQuantity = BigDecimal.ZERO;

        AggregateAccumulator(String client, String side, String ticker) {
            this.client = client;
            this.side = side;
            this.ticker = ticker;
        }

        void accumulate(BigDecimal price, long quantity) {
            totalQuantity += quantity;
            totalPriceQuantity = totalPriceQuantity.add(price.multiply(BigDecimal.valueOf(quantity), MATH_CONTEXT));
        }
    }
}
