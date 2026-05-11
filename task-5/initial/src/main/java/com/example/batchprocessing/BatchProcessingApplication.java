package com.example.batchprocessing;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class BatchProcessingApplication {

	public static void main(String[] args) {
		SpringApplication.run(BatchProcessingApplication.class, args);
	}

	@Bean
	ApplicationRunner launchBatchJob(JobLauncher jobLauncher, Job importProductJob) {
		return args -> jobLauncher.run(
			importProductJob,
			new JobParametersBuilder()
				.addLong("run.id", System.currentTimeMillis())
				.toJobParameters()
		);
	}
}
