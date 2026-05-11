package com.example.batchprocessing;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.batch.core.JobExecution;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/batch/jobs")
public class BatchJobController {

	private final BatchJobService batchJobService;

	public BatchJobController(BatchJobService batchJobService) {
		this.batchJobService = batchJobService;
	}

	@PostMapping("/import-products")
	public ResponseEntity<JobLaunchResponse> launchImportProductsJob(HttpServletRequest request) {
		JobExecution jobExecution = batchJobService.launchImportProductsJob(request.getRequestURI());
		JobLaunchResponse response = new JobLaunchResponse(
			jobExecution.getId(),
			jobExecution.getJobInstance().getJobName(),
			jobExecution.getStatus().name(),
			jobExecution.getJobParameters().getString("traceId"),
			jobExecution.getJobParameters().getString("spanId"),
			jobExecution.getJobParameters().getString("requestUri")
		);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
	}
}
