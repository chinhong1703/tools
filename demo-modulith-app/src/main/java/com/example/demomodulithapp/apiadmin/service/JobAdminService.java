package com.example.demomodulithapp.apiadmin.service;

import com.example.demomodulithapp.ingestjob.service.OrderIngestJobRunner;
import java.time.LocalDate;
import java.util.List;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.stereotype.Service;

@Service
public class JobAdminService {

    private final OrderIngestJobRunner jobRunner;

    public JobAdminService(OrderIngestJobRunner jobRunner) {
        this.jobRunner = jobRunner;
    }

    public JobExecution triggerOrderIngest(LocalDate dataDate) throws JobExecutionAlreadyRunningException,
            JobRestartException, JobInstanceAlreadyCompleteException {
        LocalDate targetDate = dataDate != null ? dataDate : jobRunner.currentDate();
        return jobRunner.run(targetDate);
    }

    public List<JobExecution> recentExecutions(int limit) {
        return jobRunner.findRecentExecutions(limit);
    }
}
