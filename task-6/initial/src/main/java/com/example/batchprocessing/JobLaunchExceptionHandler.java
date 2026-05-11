package com.example.batchprocessing;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class JobLaunchExceptionHandler {

	@ExceptionHandler(JobLaunchException.class)
	public ProblemDetail handleJobLaunchException(JobLaunchException exception, HttpServletRequest request) {
		ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		problemDetail.setTitle("Batch job launch failed");
		problemDetail.setDetail(exception.getMessage());
		problemDetail.setProperty("uri", request.getRequestURI());
		return problemDetail;
	}
}
