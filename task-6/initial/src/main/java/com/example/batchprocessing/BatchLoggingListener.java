package com.example.batchprocessing;

import java.time.Duration;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class BatchLoggingListener implements JobExecutionListener, StepExecutionListener {

	private static final Logger log = LoggerFactory.getLogger(BatchLoggingListener.class);

	@Override
	public void beforeJob(JobExecution jobExecution) {
		log.info(
			"spring-batch job started: jobName={}, executionId={}, traceId={}, spanId={}, uri={}, parameters={}",
			jobExecution.getJobInstance().getJobName(),
			jobExecution.getId(),
			jobExecution.getJobParameters().getString("traceId"),
			jobExecution.getJobParameters().getString("spanId"),
			jobExecution.getJobParameters().getString("requestUri"),
			jobExecution.getJobParameters()
		);
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		long durationMs = 0L;
		if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
			durationMs = Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime()).toMillis();
		}

		String failureMessages = jobExecution.getAllFailureExceptions().stream()
			.map(Throwable::getMessage)
			.collect(Collectors.joining("; "));

		log.info(
			"spring-batch job finished: jobName={}, executionId={}, traceId={}, spanId={}, uri={}, status={}, exitCode={}, durationMs={}, failures={}",
			jobExecution.getJobInstance().getJobName(),
			jobExecution.getId(),
			jobExecution.getJobParameters().getString("traceId"),
			jobExecution.getJobParameters().getString("spanId"),
			jobExecution.getJobParameters().getString("requestUri"),
			jobExecution.getStatus(),
			jobExecution.getExitStatus().getExitCode(),
			durationMs,
			failureMessages.isBlank() ? "none" : failureMessages
		);
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		log.info(
			"spring-batch step started: stepName={}, jobExecutionId={}, traceId={}, spanId={}, uri={}, readCount={}, writeCount={}",
			stepExecution.getStepName(),
			stepExecution.getJobExecutionId(),
			stepExecution.getJobExecution().getJobParameters().getString("traceId"),
			stepExecution.getJobExecution().getJobParameters().getString("spanId"),
			stepExecution.getJobExecution().getJobParameters().getString("requestUri"),
			stepExecution.getReadCount(),
			stepExecution.getWriteCount()
		);
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		log.info(
			"spring-batch step finished: stepName={}, jobExecutionId={}, traceId={}, spanId={}, uri={}, status={}, readCount={}, writeCount={}, commitCount={}, rollbackCount={}, skipCount={}",
			stepExecution.getStepName(),
			stepExecution.getJobExecutionId(),
			stepExecution.getJobExecution().getJobParameters().getString("traceId"),
			stepExecution.getJobExecution().getJobParameters().getString("spanId"),
			stepExecution.getJobExecution().getJobParameters().getString("requestUri"),
			stepExecution.getStatus(),
			stepExecution.getReadCount(),
			stepExecution.getWriteCount(),
			stepExecution.getCommitCount(),
			stepExecution.getRollbackCount(),
			stepExecution.getReadSkipCount() + stepExecution.getProcessSkipCount() + stepExecution.getWriteSkipCount()
		);
		return stepExecution.getExitStatus();
	}
}
