package com.example.batchprocessing;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.stereotype.Component;

@Component
public class BatchMetricsListener implements JobExecutionListener {

	private final BatchMetrics batchMetrics;
	private final JobExplorer jobExplorer;
	private final BatchMetricsPushGateway batchMetricsPushGateway;

	public BatchMetricsListener(BatchMetrics batchMetrics, JobExplorer jobExplorer,
								BatchMetricsPushGateway batchMetricsPushGateway) {
		this.batchMetrics = batchMetrics;
		this.jobExplorer = jobExplorer;
		this.batchMetricsPushGateway = batchMetricsPushGateway;
	}

	@Override
	public void beforeJob(JobExecution jobExecution) {
		// No-op.
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		long durationMs = 0L;
		if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
			durationMs = java.time.Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime()).toMillis();
		}
		long completedJobs = 0L;
		long failedJobs = 0L;
		String jobName = jobExecution.getJobInstance().getJobName();
		int start = 0;
		int pageSize = 100;
		while (true) {
			var jobInstances = jobExplorer.getJobInstances(jobName, start, pageSize);
			if (jobInstances.isEmpty()) {
				break;
			}
			for (var jobInstance : jobInstances) {
				for (JobExecution execution : jobExplorer.getJobExecutions(jobInstance)) {
					if (execution.getStatus() == BatchStatus.COMPLETED) {
						completedJobs++;
					}
					if (execution.getStatus() == BatchStatus.FAILED) {
						failedJobs++;
					}
				}
			}
			start += jobInstances.size();
		}
		batchMetrics.jobFinished(completedJobs, failedJobs, durationMs);
		batchMetricsPushGateway.push(batchMetrics);
	}
}
