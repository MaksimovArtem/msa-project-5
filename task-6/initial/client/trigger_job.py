import json
import os
import secrets
import time
import urllib.error
import urllib.request


BATCH_TRIGGER_URL = os.getenv(
    "BATCH_TRIGGER_URL",
    "http://localhost:8080/api/batch/jobs/import-products",
)
REQUEST_DELAY_SECONDS = int(os.getenv("CLIENT_REQUEST_DELAY_SECONDS", "5"))
MAX_RETRIES = int(os.getenv("CLIENT_MAX_RETRIES", "12"))


def log_event(level, message, **fields):
    payload = {
        "app": "batch-trigger-client",
        "level": level,
        "msg": message,
        "traceId": fields.pop("traceId", ""),
        "spanId": fields.pop("spanId", ""),
        "uri": fields.pop("uri", ""),
        **fields,
    }
    print(json.dumps(payload, ensure_ascii=True), flush=True)


def generate_trace_context():
    trace_id = secrets.token_hex(16)
    span_id = secrets.token_hex(8)
    traceparent = f"00-{trace_id}-{span_id}-01"
    return trace_id, span_id, traceparent


def trigger_job():
    trace_id, span_id, traceparent = generate_trace_context()
    request = urllib.request.Request(
        BATCH_TRIGGER_URL,
        method="POST",
        headers={
            "Content-Type": "application/json",
            "traceparent": traceparent,
        },
    )

    log_event(
        "INFO",
        "calling batch trigger api",
        traceId=trace_id,
        spanId=span_id,
        uri=BATCH_TRIGGER_URL,
        traceparent=traceparent,
    )

    with urllib.request.urlopen(request, timeout=30) as response:
        body = response.read().decode("utf-8")
        log_event(
            "INFO",
            "batch trigger api completed",
            traceId=trace_id,
            spanId=span_id,
            uri=BATCH_TRIGGER_URL,
            statusCode=response.status,
            response=json.loads(body),
        )


def main():
    time.sleep(REQUEST_DELAY_SECONDS)
    for attempt in range(1, MAX_RETRIES + 1):
        try:
            trigger_job()
            return
        except urllib.error.URLError as error:
            log_event(
                "WARN",
                "batch trigger api is not ready yet",
                uri=BATCH_TRIGGER_URL,
                attempt=attempt,
                error=str(error),
            )
            time.sleep(5)

    raise SystemExit("Batch trigger client failed to call API after retries")


if __name__ == "__main__":
    main()
