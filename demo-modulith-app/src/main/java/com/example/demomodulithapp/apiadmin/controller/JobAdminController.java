package com.example.demomodulithapp.apiadmin.controller;

import com.example.demomodulithapp.apiadmin.service.JobAdminService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/jobs/orderIngest")
@PreAuthorize("hasRole('ADMIN')")
public class JobAdminController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final JobAdminService jobAdminService;

    public JobAdminController(JobAdminService jobAdminService) {
        this.jobAdminService = jobAdminService;
    }

    @PostMapping("/run")
    public ResponseEntity<?> runJob(@RequestParam(value = "dataDate", required = false) String dataDate) {
        try {
            LocalDate parsedDate = dataDate != null ? LocalDate.parse(dataDate, FORMATTER) : null;
            JobExecution execution = jobAdminService.triggerOrderIngest(parsedDate);
            return ResponseEntity.accepted().body(toDto(execution));
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid dataDate format"));
        } catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/executions")
    public List<Map<String, Object>> executions(@RequestParam(value = "limit", defaultValue = "20") int limit) {
        return jobAdminService.recentExecutions(limit).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private Map<String, Object> toDto(JobExecution execution) {
        return Map.of(
                "id", execution.getId(),
                "status", execution.getStatus().name(),
                "startTime", execution.getStartTime(),
                "endTime", execution.getEndTime(),
                "exitStatus", execution.getExitStatus().getExitCode(),
                "parameters", convertParameters(execution.getJobParameters())
        );
    }

    private Map<String, Object> convertParameters(JobParameters parameters) {
        return parameters.getParameters().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> formatParameter(entry.getValue())));
    }

    private Object formatParameter(JobParameter parameter) {
        if (parameter == null) {
            return null;
        }
        if (parameter.getType() == JobParameter.ParameterType.DATE) {
            return parameter.getValue();
        }
        return parameter.getValue();
    }
}
