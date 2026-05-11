package com.example.batchprocessing;

import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

@Component
public class BatchMetrics {

	private final AtomicLong completedJobCount = new AtomicLong();
	private final AtomicLong failedJobCount = new AtomicLong();
	private final AtomicLong lastJobDurationMs = new AtomicLong();

	void jobFinished(long completedJobs, long failedJobs, long durationMs) {
		lastJobDurationMs.set(durationMs);
		completedJobCount.set(completedJobs);
		failedJobCount.set(failedJobs);
	}

	public long getCompletedJobCount() {
		return completedJobCount.get();
	}

	public long getFailedJobCount() {
		return failedJobCount.get();
	}

	public long getLastJobDurationMs() {
		return lastJobDurationMs.get();
	}
}
