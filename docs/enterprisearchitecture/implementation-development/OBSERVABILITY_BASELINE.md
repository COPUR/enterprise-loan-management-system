# Observability Baseline (All Microservices)

## Traceability
- **Trace ID propagation** using `traceparent` (W3C) and `X-FAPI-Interaction-ID` for Open Finance APIs.
- All inbound requests must produce a trace id; outbound calls must forward it.

## Metrics (Golden Signals)
- **Latency**: p50/p95/p99 response time.
- **Traffic**: requests per second.
- **Errors**: 4xx/5xx rate.
- **Saturation**: CPU/memory usage, queue depth.

## Logging
- Structured JSON logging.
- Required fields: `timestamp`, `service`, `environment`, `trace_id`, `interaction_id`, `status`, `duration_ms`.
- **PII masking**: IBAN, account names, customer identifiers must be redacted.

## Health & Readiness
- `/actuator/health` must expose readiness and liveness probes.
- Dependency checks must be non‑blocking and time‑boxed.

## Tooling Baseline
- Tracing: OpenTelemetry SDKs.
- Metrics: Prometheus/Micrometer.
- Logs: ELK/Splunk forwarder with masking policy.

