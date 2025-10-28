package com.example.demomodulithapp.ingestjob.tasklets;

import com.example.demomodulithapp.ingestjob.domain.AggregatedOrder;
import com.example.demomodulithapp.ingestjob.entity.AggregatedOrderEntity;
import com.example.demomodulithapp.ingestjob.repo.AggregatedOrderRepository;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Component
public class PersistAggregatesTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(PersistAggregatesTasklet.class);

    private final AggregatedOrderRepository repository;
    private final MeterRegistry meterRegistry;

    public PersistAggregatesTasklet(AggregatedOrderRepository repository, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.meterRegistry = meterRegistry;
    }

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        @SuppressWarnings("unchecked")
        List<AggregatedOrder> aggregates = (List<AggregatedOrder>) chunkContext.getStepContext().getStepExecution()
                .getJobExecution().getExecutionContext().get("aggregates");
        if (CollectionUtils.isEmpty(aggregates)) {
            log.info("No aggregates to persist");
            return RepeatStatus.FINISHED;
        }
        LocalDate dataDate = aggregates.get(0).getDataDate();
        repository.deleteByDataDate(dataDate);
        List<AggregatedOrderEntity> entities = new ArrayList<>();
        for (AggregatedOrder aggregate : aggregates) {
            AggregatedOrderEntity entity = new AggregatedOrderEntity(
                    aggregate.getDataDate(),
                    aggregate.getClient(),
                    aggregate.getSide(),
                    aggregate.getTicker(),
                    aggregate.getTotalQuantity(),
                    aggregate.getVwap());
            entities.add(entity);
        }
        repository.saveAll(entities);
        meterRegistry.counter("aggregates.persisted").increment(entities.size());
        log.info("Persisted {} aggregate rows for {}", entities.size(), dataDate);
        return RepeatStatus.FINISHED;
    }
}
