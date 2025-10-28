package com.example.demomodulithapp.ingestjob.config;

import com.example.demomodulithapp.ingestjob.listeners.JobMetricsListener;
import com.example.demomodulithapp.ingestjob.listeners.StepLoggingListener;
import com.example.demomodulithapp.ingestjob.tasklets.PersistAggregatesTasklet;
import com.example.demomodulithapp.ingestjob.tasklets.ReadFromCsvTasklet;
import com.example.demomodulithapp.ingestjob.tasklets.TransformAndWriteAggregatesCsvTasklet;
import com.example.demomodulithapp.ingestjob.tasklets.ValidateAndRejectsTasklet;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AppProperties.class)
public class BatchConfig {

    @Bean
    Job orderIngestJob(JobRepository jobRepository,
                       Step readFromCsvStep,
                       Step validateStep,
                       Step transformStep,
                       Step persistStep,
                       JobMetricsListener jobMetricsListener) {
        return new JobBuilder("orderIngestJob", jobRepository)
                .listener(jobMetricsListener)
                .start(readFromCsvStep)
                .next(validateStep)
                .next(transformStep)
                .next(persistStep)
                .build();
    }

    @Bean
    Step readFromCsvStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                         ReadFromCsvTasklet tasklet, StepLoggingListener stepLoggingListener) {
        return new StepBuilder("readFromCsvStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .listener(stepLoggingListener)
                .build();
    }

    @Bean
    Step validateStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                      ValidateAndRejectsTasklet tasklet, StepLoggingListener stepLoggingListener) {
        return new StepBuilder("validateAndRejectsStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .listener(stepLoggingListener)
                .build();
    }

    @Bean
    Step transformStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                       TransformAndWriteAggregatesCsvTasklet tasklet, StepLoggingListener stepLoggingListener) {
        return new StepBuilder("transformAndWriteAggregatesStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .listener(stepLoggingListener)
                .build();
    }

    @Bean
    Step persistStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
                     PersistAggregatesTasklet tasklet, StepLoggingListener stepLoggingListener) {
        return new StepBuilder("persistAggregatesStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .listener(stepLoggingListener)
                .build();
    }

    @Bean
    JobMetricsListener jobMetricsListener(MeterRegistry meterRegistry) {
        return new JobMetricsListener(meterRegistry);
    }

    @Bean
    StepLoggingListener stepLoggingListener(MeterRegistry meterRegistry) {
        return new StepLoggingListener(meterRegistry);
    }
}
