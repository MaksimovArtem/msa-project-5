package com.example.batchprocessing;

import java.util.Optional;
import java.util.UUID;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

@Service
public class BatchJobService {

	private static final Logger log = LoggerFactory.getLogger(BatchJobService.class);

	private final JobLauncher jobLauncher;
	private final Job importProductJob;
	private final Tracer tracer;

	public BatchJobService(JobLauncher jobLauncher, Job importProductJob, Tracer tracer) {
		this.jobLauncher = jobLauncher;
		this.importProductJob = importProductJob;
		this.tracer = tracer;
	}

	public JobExecution launchImportProductsJob(String requestUri) {
		TraceContextSnapshot traceContext = currentTraceContext(requestUri);
		JobParameters jobParameters = new JobParametersBuilder()
			.addLong("run.id", System.currentTimeMillis())
			.addString("traceId", traceContext.traceId())
			.addString("spanId", traceContext.spanId())
			.addString("requestUri", traceContext.requestUri())
			.toJobParameters();

		try {
			log.info(
				"launching spring-batch job via api: jobName={}, traceId={}, spanId={}, uri={}",
				importProductJob.getName(),
				traceContext.traceId(),
				traceContext.spanId(),
				traceContext.requestUri()
			);
			return jobLauncher.run(importProductJob, jobParameters);
		} catch (Exception exception) {
			throw new JobLaunchException("Failed to launch job " + importProductJob.getName(), exception);
		}
	}

	private TraceContextSnapshot currentTraceContext(String requestUri) {
		Span currentSpan = tracer.currentSpan();
		String fallbackTraceId = UUID.randomUUID().toString().replace("-", "");
		String fallbackSpanId = fallbackTraceId.substring(0, 16);

		return new TraceContextSnapshot(
			Optional.ofNullable(currentSpan)
				.map(span -> span.context().traceId())
				.orElse(fallbackTraceId),
			Optional.ofNullable(currentSpan)
				.map(span -> span.context().spanId())
				.orElse(fallbackSpanId),
			requestUri
		);
	}

	private record TraceContextSnapshot(String traceId, String spanId, String requestUri) {
	}
}
