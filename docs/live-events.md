# Live Events

UJFE live events are server-side interactions sent by the browser bridge to the JVM runtime. The browser sends a small JSON request to UJFE internal endpoints, the server runs the registered Java handler, and the response contains the updated HTML and CSS needed to refresh the page.

This document defines the shared HTTP JSON contract used by all supported UJFE runtimes.

## Shared Codec

`LiveHttpCodec` is the single shared codec for live HTTP JSON requests and responses.

It owns:

- event payload parsing;
- client state payload parsing;
- live response serialization;
- cookie parsing;
- requested CSS class parsing;
- payload size validation;
- safe validation errors;
- safe rejected-payload logging metadata.

Runtime adapters must call `LiveHttpCodec` instead of parsing or serializing live JSON themselves.

The current supported adapters are:

| Runtime | Adapter | Codec use |
| --- | --- | --- |
| Netty | `UjfeHttpHandler` | Uses `LiveHttpCodec.parseEventPayload(...)`, `parseStatePayload(...)`, and `livePayload(...)` |
| Jakarta Servlet | `UjfeServlet` | Uses the same codec for `/_ujfe/event` and `/_ujfe/state` |
| Spring MVC | `UjfeSpringHandler` | Uses the same codec through the Servlet request pipeline |

Keeping the codec in `ujfe-live` makes the behavior runtime-neutral and suitable for future AOT-oriented deployments. The codec does not depend on Spring, Servlet APIs, or Netty APIs.

## Internal Endpoints

Live HTTP requests use UJFE internal endpoints:

| Path | Method | Purpose |
| --- | --- | --- |
| `/_ujfe/event` | `POST` | Dispatch a browser event to its server-side Java handler |
| `/_ujfe/state` | `POST` | Synchronize browser cookie/localStorage state with the current live session |
| `/_ujfe/css` | `GET` | Render server-side CSS for requested classes |
| `/_ujfe/client.js` | `GET` | Serve the browser event bridge |
| `/_ujfe/dev.js` | `GET` | Serve optional development preview tooling |

Application code usually does not call these endpoints directly. They are documented to make the runtime behavior explicit and testable.

## Event Payload

`/_ujfe/event` accepts a JSON object with an opaque event id and optional client state:

```json
{
  "eventId": "opaque-event-id",
  "clientState": {
    "cookies": "ujfe_demo=active; theme=dark",
    "localStorage": {
      "ujfe.theme": "dark"
    }
  }
}
```

Rules:

- the request body must be a JSON object;
- `eventId` is required and must be a nonblank string;
- `clientState.cookies` is a normal HTTP cookie header string when present;
- `clientState.localStorage` is an object whose values are strings or `null`;
- unknown fields are ignored by the current codec.

The server response is serialized by `LiveHttpCodec.livePayload(...)`:

```json
{
  "html": "<main>...</main>",
  "css": ".generated-css{...}"
}
```

## State Payload

`/_ujfe/state` accepts a JSON object with a required `clientState` object:

```json
{
  "clientState": {
    "cookies": "ujfe_demo=active",
    "localStorage": {
      "ujfe.theme": "dark"
    }
  }
}
```

The response uses the same live response shape:

```json
{
  "html": "<main>...</main>",
  "css": ".generated-css{...}"
}
```

## Payload Size Limit

UJFE enforces a default maximum JSON request payload size of `1,048,576` bytes.

The limit applies to live JSON request bodies for:

- `/_ujfe/event`;
- `/_ujfe/state`.

Adapters enforce the limit before JSON parsing whenever the runtime exposes the request length cheaply:

- Netty checks the readable byte count and configures `HttpObjectAggregator` with the same limit.
- Servlet and Spring check `getContentLengthLong()` when available and also enforce the limit while reading the request body.

Oversized payloads fail closed with:

```text
HTTP 413
Live JSON payload exceeds maximum size.
```

## Configuring The Limit

### Netty

```java
var server = UjfeServer.create(
        router,
        UjfeServerConfig.builder()
                .host("0.0.0.0")
                .port(8080)
                .maxJsonPayloadBytes(262_144)
                .build()
);
```

### Jakarta Servlet

Programmatic setup:

```java
var servlet = new UjfeServlet(
        router,
        LiveSessionConfig.defaults(),
        262_144
);
```

`application.properties`:

```properties
ujfe.live.max-json-payload-bytes=262144
```

`application.yml`:

```yaml
ujfe:
  live:
    max-json-payload-bytes: 262144
```

### Spring MVC

```java
var handler = new UjfeSpringHandler(liveSession, 262_144);
```

Spring Boot applications can expose this through their own configuration class while still delegating the actual parsing and validation to `LiveHttpCodec`.

## Safe Error Model

Invalid live JSON requests produce deterministic JSON error responses through the shared `ErrorResponseRenderer`. The client does not receive parser internals, stack traces, implementation class names, raw request bodies, headers, cookies, CSRF tokens, authorization headers, or secrets.

Example response:

```json
{
  "error": {
    "code": "UJFE_BAD_REQUEST",
    "message": "The request is invalid.",
    "requestId": "req-123"
  }
}
```

| Category | HTTP status | Error code | Client message |
| --- | --- | --- | --- |
| empty request body | `400` | `UJFE_BAD_REQUEST` | `The request is invalid.` |
| empty JSON object | `400` | `UJFE_BAD_REQUEST` | `The request is invalid.` |
| malformed JSON | `400` | `UJFE_BAD_REQUEST` | `The request is invalid.` |
| missing event id | `400` | `UJFE_BAD_REQUEST` | `The request is invalid.` |
| missing state object | `400` | `UJFE_BAD_REQUEST` | `The request is invalid.` |
| payload too large | `413` | `UJFE_BAD_REQUEST` | `The request is invalid.` |
| missing csrf token | `403` | `UJFE_CSRF_VALIDATION_FAILED` | `The request could not be verified.` |
| invalid csrf token | `403` | `UJFE_CSRF_VALIDATION_FAILED` | `The request could not be verified.` |
| cross origin request | `403` | `UJFE_CSRF_VALIDATION_FAILED` | `The request could not be verified.` |
| rate limit exceeded | `429` | `UJFE_RATE_LIMITED` | `Too many requests.` |

Malformed, empty, incomplete, oversized, or rate-limited payloads are rejected before dispatching live event handlers. CSRF failures are also rejected before dispatching. Same-origin validation compares request scheme, host, and effective port.

## Logging And Observability

Rejected payloads are logged through `LiveHttpCodec.logRejectedPayload(...)` with safe structured metadata:

```text
event=ujfe.live_http_payload_rejected
category=payload_too_large
adapter=servlet
httpStatus=413
payloadLimitBytes=262144
payloadSizeBytes=524288
traceId=req-123
message="Live JSON payload exceeds maximum size."
```

The log entry may include:

- failure category;
- runtime adapter name;
- HTTP status;
- configured payload limit;
- observed payload size when available;
- request trace or correlation id when available.

Adapters currently read correlation from `X-Request-Id` first and `X-Correlation-Id` second when those headers are available.

The log entry must not include:

- full request bodies;
- cookies;
- authorization headers;
- tokens;
- raw parser errors;
- stack traces;
- request headers containing credentials.

Rate limit rejections are logged as `event=ujfe.live_rate_limit_rejected` with endpoint, adapter, key type, retry delay, and correlation id when available. Future observability integrations can consume the same failure category and adapter metadata without changing the HTTP payload contract.

## Runtime Reuse Contract

New HTTP runtimes should follow this sequence for live JSON endpoints:

1. Determine the adapter name for logs, such as `netty`, `servlet`, or `spring`.
2. Build safe request metadata.
3. Apply internal endpoint rate limiting before reading the body.
4. Enforce the configured maximum body size before parsing when the request length is available.
5. Read the request body through a bounded path.
6. Call `LiveHttpCodec.parseEventPayload(...)` or `LiveHttpCodec.parseStatePayload(...)`.
7. Dispatch to `LiveSession`.
8. Serialize the response through `LiveHttpCodec.livePayload(...)`.
9. Catch `LiveRateLimitException`, log it with safe metadata, and return `safeMessage()` with HTTP status `429`.
10. Catch `LiveCsrfException`, log it with safe metadata, and return `safeMessage()` with HTTP status `403`.
11. Catch `LiveHttpCodecException`, log it with safe metadata, and return `safeMessage()` with its HTTP status.

Adapters should not duplicate JSON parsing rules or expose raw exception messages to clients.
