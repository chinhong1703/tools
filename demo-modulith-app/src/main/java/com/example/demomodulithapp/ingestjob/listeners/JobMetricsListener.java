package com.example.demomodulithapp.ingestjob.listeners;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

public class JobMetricsListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(JobMetricsListener.class);

    private final MeterRegistry meterRegistry;
    private final Map<Long, Timer.Sample> samples = new ConcurrentHashMap<>();

    public JobMetricsListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        samples.put(jobExecution.getId(), Timer.start(meterRegistry));
        log.info("Starting job {} with parameters {}", jobExecution.getJobInstance().getJobName(), jobExecution.getJobParameters());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        Timer.Sample sample = samples.remove(jobExecution.getId());
        if (sample != null) {
            sample.stop(meterRegistry.timer("ingest.job.duration", "job", jobExecution.getJobInstance().getJobName(),
                    "status", jobExecution.getStatus().name()));
        }
        log.info("Job {} completed with status {}", jobExecution.getJobInstance().getJobName(), jobExecution.getStatus());
    }
}
