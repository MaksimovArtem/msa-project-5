package com.example.batchprocessing;

public record JobLaunchResponse(
	Long executionId,
	String jobName,
	String status,
	String traceId,
	String spanId,
	String requestUri
) {
}
