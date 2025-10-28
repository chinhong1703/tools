package com.example.demomodulithapp.ingestjob.listeners;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.ExitStatus;

public class StepLoggingListener implements StepExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(StepLoggingListener.class);
    private final MeterRegistry meterRegistry;
    private final Map<String, Timer.Sample> samples = new ConcurrentHashMap<>();

    public StepLoggingListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        samples.put(stepExecution.getStepName(), Timer.start(meterRegistry));
        log.info("Starting step {}", stepExecution.getStepName());
    }

    @Override
    @AfterStep
    public ExitStatus afterStep(StepExecution stepExecution) {
        Timer.Sample sample = samples.remove(stepExecution.getStepName());
        if (sample != null) {
            sample.stop(meterRegistry.timer("ingest.step.duration", "step", stepExecution.getStepName(),
                    "status", stepExecution.getStatus().name()));
        }
        log.info("Completed step {} with status {}", stepExecution.getStepName(), stepExecution.getStatus());
        return stepExecution.getExitStatus();
    }
}
