package com.example.demomodulithapp.ingestjob.scheduler;

import com.example.demomodulithapp.ingestjob.service.OrderIngestJobRunner;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobScheduler {

    private static final Logger log = LoggerFactory.getLogger(JobScheduler.class);
    private final OrderIngestJobRunner jobRunner;

    public JobScheduler(OrderIngestJobRunner jobRunner) {
        this.jobRunner = jobRunner;
    }

    @Scheduled(cron = "${app.schedule.cron}", zone = "${app.timezone}")
    public void scheduleOrderIngestJob() {
        LocalDate date = jobRunner.currentDate();
        try {
            jobRunner.run(date);
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException e) {
            log.warn("Unable to schedule job for {} due to {}", date, e.getMessage());
        }
    }
}
