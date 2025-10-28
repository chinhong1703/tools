package com.example.demomodulithapp.ingestjob.service;

import com.example.demomodulithapp.ingestjob.config.AppProperties;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.stereotype.Component;

@Component
public class OrderIngestJobRunner {

    private static final Logger log = LoggerFactory.getLogger(OrderIngestJobRunner.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final JobLauncher jobLauncher;
    private final Job orderIngestJob;
    private final JobExplorer jobExplorer;
    private final ZoneId zoneId;

    public OrderIngestJobRunner(JobLauncher jobLauncher, Job orderIngestJob, JobExplorer jobExplorer,
                                AppProperties properties) {
        this.jobLauncher = jobLauncher;
        this.orderIngestJob = orderIngestJob;
        this.jobExplorer = jobExplorer;
        this.zoneId = ZoneId.of(properties.getTimezone());
    }

    public JobExecution run(LocalDate dataDate) throws JobExecutionAlreadyRunningException,
            JobRestartException, JobInstanceAlreadyCompleteException {
        JobParameters parameters = new JobParametersBuilder()
                .addString("dataDate", DATE_FORMATTER.format(dataDate))
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        log.info("Triggering orderIngestJob for dataDate {}", dataDate);
        return jobLauncher.run(orderIngestJob, parameters);
    }

    public List<JobExecution> findRecentExecutions(int limit) {
        return jobExplorer.getJobInstances(orderIngestJob.getName(), 0, limit).stream()
                .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
                .sorted((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public LocalDate currentDate() {
        return LocalDate.now(zoneId);
    }
}
