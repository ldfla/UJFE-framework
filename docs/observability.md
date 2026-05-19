# Observability

UJFE exposes dependency-free render and event traces for applications that
need operational telemetry without tying `ujfe-core` to Micrometer,
OpenTelemetry, Prometheus, Datadog, New Relic, or any other vendor API.

The core model is intentionally small:

- `RenderTrace` describes one page render attempt.
- `EventTrace` describes one live event handling attempt.
- `TraceStatus` provides stable success and failure categories.
- `TraceSink` receives completed traces.
- `ObservabilityConfig` wires sinks and the clock used for deterministic tests.

The default configuration enables trace creation but uses a no-op sink. This
keeps the runtime safe and low overhead until an application registers a sink
or runtime trace action.

## Render Traces

`RenderTrace` is emitted after a render operation completes, fails, or resolves
to a safe route-not-found response. It may include:

- trace id
- request id or correlation id
- route or path known to the runtime
- HTTP method and status when provided by the adapter
- adapter name, such as `netty`, `servlet`, or `spring`
- render duration
- response size in bytes
- `TraceStatus`
- safe UJFE error code
- safe exception type
- start timestamp
- render source, such as normal render, routing, or safe error handling

It does not include request bodies, cookies, authorization headers, CSRF
tokens, session secrets, browser state, rendered HTML, user form values, raw
exception messages, or stack traces.

## Event Traces

`EventTrace` is emitted after live event handling completes, fails, is rejected,
or references a stale or unknown event id. It may include:

- trace id
- request id or correlation id
- route when available
- opaque event id
- HTTP method and status when provided by the adapter
- adapter name
- handler duration
- response size in bytes
- `TraceStatus`
- safe UJFE error code
- safe exception type
- whether a handler was found
- whether the handler completed
- start timestamp

It does not include raw event payloads, submitted form values, browser state,
request bodies, cookies, authorization headers, CSRF tokens, session secrets,
raw exception messages, or stack traces.

## Status Mapping

`TraceStatus` is stable API intended for logs and metrics adapters:

| Status | Meaning |
| --- | --- |
| `SUCCESS` | Operation completed normally. |
| `CLIENT_ERROR` | Invalid client request or generic 4xx response. |
| `SERVER_ERROR` | Runtime failure or generic 5xx response. |
| `NOT_FOUND` | Route or event handler was not found. |
| `FORBIDDEN` | Request was rejected by CSRF or origin policy. |
| `RATE_LIMITED` | Request was rejected by rate limiting. |
| `VALIDATION_FAILED` | Payload or request validation failed. |
| `ERROR` | Non-HTTP or uncategorized failure. |

HTTP status mapping uses the most specific category when possible. For example,
`404` maps to `NOT_FOUND`, `403` maps to `FORBIDDEN`, `429` maps to
`RATE_LIMITED`, other `4xx` responses map to `CLIENT_ERROR`, and `5xx`
responses map to `SERVER_ERROR`.

## Registering A Sink

Use `LiveSessionConfig` to register a sink:

```java
TraceSink sink = new TraceSink() {
    @Override
    public void onRenderTrace(RenderTrace trace) {
        logger.info("render route={} status={} duration={}",
                trace.route(), trace.status(), trace.duration());
    }

    @Override
    public void onEventTrace(EventTrace trace) {
        logger.info("event route={} status={} duration={}",
                trace.route(), trace.status(), trace.duration());
    }
};

LiveSessionConfig config = LiveSessionConfig.builder()
        .observability(ObservabilityConfig.builder()
                .traceSink(sink)
                .build())
        .build();
```

For tests, configure a fixed clock:

```java
LiveSessionConfig config = LiveSessionConfig.builder()
        .observability(ObservabilityConfig.builder()
                .clock(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))
                .traceSink(recordingSink)
                .build())
        .build();
```

## Runtime Actions

Observability integrates with the existing runtime actions model. Applications
can register trace actions alongside render, event, error, and head actions:

```java
RuntimeActionRegistry registry = RuntimeActionRegistry.builder()
        .renderTrace(trace -> metrics.recordRender(trace))
        .eventTrace(trace -> metrics.recordEvent(trace))
        .build();
```

Trace action failures are isolated. A broken listener must not fail rendering
or event handling.

## Adapter Context

The runtime remains adapter-independent. Netty, Servlet, and Spring adapters
provide safe request metadata when available:

- adapter name
- HTTP method
- route/path
- request id or correlation id

Adapters must not pass sensitive headers, cookies, request bodies, tokens, or
payload values into trace objects.

## Metrics Guidance

Trace fields are safe to forward to logs or enterprise monitoring when the
default policy is used. For metrics tags, keep cardinality low:

- Good tags: route pattern, HTTP method, HTTP status, trace status, adapter
  name, safe error code.
- Avoid tags: request id, correlation id, user id, session id, raw path with
  dynamic ids, query string, opaque event id, event payload, form values.

Spring or enterprise modules can adapt traces to Micrometer later by recording
render and event durations as timers and failure statuses as counters. That
adapter belongs outside `ujfe-core` so the core module stays dependency-light.
