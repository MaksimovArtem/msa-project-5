package com.example.batchprocessing;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BatchMetricsPushGateway {

	private static final Logger log = LoggerFactory.getLogger(BatchMetricsPushGateway.class);

	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final String pushGatewayUrl;

	public BatchMetricsPushGateway(@Value("${pushgateway.url:http://pushgateway:9091}") String pushGatewayUrl) {
		this.pushGatewayUrl = pushGatewayUrl;
	}

	public void push(BatchMetrics batchMetrics) {
		String body = """
			# TYPE spring_batch_completed_job_count_total counter
			spring_batch_completed_job_count_total %d
			# TYPE spring_batch_failed_job_count_total counter
			spring_batch_failed_job_count_total %d
			# TYPE spring_batch_last_job_duration_ms gauge
			spring_batch_last_job_duration_ms %d
			""".formatted(
			batchMetrics.getCompletedJobCount(),
			batchMetrics.getFailedJobCount(),
			batchMetrics.getLastJobDurationMs()
		);

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(pushGatewayUrl + "/metrics/job/batch-processing"))
			.header("Content-Type", "text/plain; version=0.0.4")
			.PUT(HttpRequest.BodyPublishers.ofString(body))
			.build();

		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() >= 200 && response.statusCode() < 300) {
				log.info("batch metrics pushed to Pushgateway");
			} else {
				log.warn("failed to push batch metrics to Pushgateway: status={}, body={}", response.statusCode(), response.body());
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			log.warn("failed to push batch metrics to Pushgateway", exception);
		} catch (IOException exception) {
			log.warn("failed to push batch metrics to Pushgateway", exception);
		}
	}
}
