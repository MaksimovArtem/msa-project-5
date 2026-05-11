# Spring Batch API Trigger With Tracing

## What changed

- Spring Batch job no longer starts on application startup.
- Added HTTP endpoint `POST /api/batch/jobs/import-products` to trigger the ETL job.
- Added Micrometer-based request tracing for Spring Boot logs.
- Added `uri` to MDC-backed JSON logs, alongside `traceId` and `spanId`.
- Added a small Python client that calls the endpoint and sends a W3C `traceparent` header.
- Kept ELK delivery through Docker stdout -> Filebeat -> Logstash -> Elasticsearch.

## Why this tracing approach

The most suitable option here is `Micrometer Tracing` with the Brave bridge:

- it is already aligned with Spring Boot 3;
- it automatically creates HTTP server spans;
- it populates MDC with `traceId` and `spanId`;
- it works well with structured JSON logging and ELK.

For the `uri` field, a lightweight servlet filter writes the request URI to MDC.

## Run

From [initial](/home/amax/Documents/YaCourse/Sprint5/msa-project-5-main/task-6/initial):

```bash
docker compose up --build
```

The client container will call:

```text
POST http://app:8080/api/batch/jobs/import-products
```

The same API is also available from the host:

```bash
curl -X POST http://localhost:8080/api/batch/jobs/import-products
```

## What to look for in logs

Client log example:

```json
{
  "app": "batch-trigger-client",
  "traceId": "9e1d4b4cdcf477cf4d029abbcdccb841",
  "spanId": "2893e5ab6fad5b3e",
  "uri": "http://app:8080/api/batch/jobs/import-products"
}
```

Server log example:

```json
{
  "app": "batch-processing",
  "traceId": "9e1d4b4cdcf477cf4d029abbcdccb841",
  "spanId": "5aacd948be88ff43",
  "uri": "/api/batch/jobs/import-products",
  "msg": "spring-batch job started: jobName=importProductJob, executionId=24, ..."
}
```

The key point is that client and server logs share one `traceId`.

## ELK demonstration

1. Open Kibana at `http://localhost:5601`.
2. Create a data view for `filebeat-*` if Kibana asks for one.
3. Open Discover.
4. Filter by one of the fields:

```text
traceId : "9e1d4b4cdcf477cf4d029abbcdccb841"
```

or

```text
uri : "/api/batch/jobs/import-products"
```

You should see:

- client request log;
- API launch log;
- Spring Batch job and step logs;
- completion logs with the same trace context.

## Verified locally

The stack was started with Docker Compose, the client successfully called the API, and Elasticsearch returned indexed records for the shared trace:

```text
traceId = 9e1d4b4cdcf477cf4d029abbcdccb841
```

## Important files

- [BatchJobController.java](/home/amax/Documents/YaCourse/Sprint5/msa-project-5-main/task-6/initial/src/main/java/com/example/batchprocessing/BatchJobController.java)
- [BatchJobService.java](/home/amax/Documents/YaCourse/Sprint5/msa-project-5-main/task-6/initial/src/main/java/com/example/batchprocessing/BatchJobService.java)
- [RequestTracingFilter.java](/home/amax/Documents/YaCourse/Sprint5/msa-project-5-main/task-6/initial/src/main/java/com/example/batchprocessing/RequestTracingFilter.java)
- [BatchLoggingListener.java](/home/amax/Documents/YaCourse/Sprint5/msa-project-5-main/task-6/initial/src/main/java/com/example/batchprocessing/BatchLoggingListener.java)
- [logback-spring.xml](/home/amax/Documents/YaCourse/Sprint5/msa-project-5-main/task-6/initial/src/main/resources/logback-spring.xml)
- [docker-compose.yml](/home/amax/Documents/YaCourse/Sprint5/msa-project-5-main/task-6/initial/docker-compose.yml)
- [trigger_job.py](/home/amax/Documents/YaCourse/Sprint5/msa-project-5-main/task-6/initial/client/trigger_job.py)
