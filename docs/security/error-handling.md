# Safe Error Responses

UJFE HTTP adapters must not expose internal exception details in response bodies. Production responses are stable JSON documents with framework error codes and generic messages. Full diagnostic details remain server-side for logs and `onError(...)` runtime actions.

This policy is shared by:

- standalone Netty runtime;
- standalone Jakarta Servlet runtime;
- Spring MVC adapter;
- future adapters that use `ErrorResponseRenderer`.

## Response Shape

Production responses use this shape:

```json
{
  "error": {
    "code": "UJFE_INTERNAL_ERROR",
    "message": "An internal error occurred.",
    "requestId": "req-123"
  }
}
```

`requestId` is included when the adapter receives `X-Request-Id` or `X-Correlation-Id`.

## Production Mode

Production mode is the default.

Production responses never include:

- raw exception messages;
- stack traces;
- implementation class names;
- reflection details;
- file paths;
- request bodies;
- cookies;
- authorization headers;
- CSRF tokens;
- session secrets;
- sensitive configuration values.

The response is intentionally generic. Operators should use server-side logs and runtime error hooks for diagnostics.

## Development Details

Controlled diagnostics are disabled by default and must be enabled explicitly:

```java
LiveSessionConfig config = LiveSessionConfig.builder()
        .enableDevelopmentErrorDetailsUnsafe()
        .build();
```

For Servlet configuration:

```properties
ujfe.errors.development-details.enabled=true
```

or:

```yaml
ujfe:
  errors:
    development-details:
      enabled: true
```

Spring Boot uses the same property path through `UjfeSpringProperties`. A custom `LiveSessionConfig` bean still overrides the auto-configured default when an application needs full control.

Development responses still use stable error codes. The optional `details` field is sanitized and intended only for local development. Do not enable it in production.

## Error Codes

| Code | HTTP status | Meaning |
| --- | --- | --- |
| `UJFE_INTERNAL_ERROR` | `500` | Unknown runtime failure. |
| `UJFE_ROUTE_NOT_FOUND` | `404` | No UJFE route matched the request. |
| `UJFE_RENDER_ERROR` | `500` | Page or component rendering failed. |
| `UJFE_EVENT_HANDLER_ERROR` | `500` | A live event handler failed. |
| `UJFE_INVALID_REQUEST` | `400` or `405` | Request method or shape is invalid for the endpoint. |
| `UJFE_BAD_REQUEST` | `400` or `413` | Live JSON payload validation failed. |
| `UJFE_UNAUTHORIZED` | `401` | Reserved for adapter authentication integration. |
| `UJFE_FORBIDDEN` | `403` | Reserved for adapter authorization integration. |
| `UJFE_RATE_LIMITED` | `429` | Internal endpoint rate limit was exceeded. |
| `UJFE_CSRF_VALIDATION_FAILED` | `403` | CSRF token or same-origin validation failed. |
| `UJFE_STATE_ERROR` | `500` | Client state update failed. |
| `UJFE_CONFIGURATION_ERROR` | `500` | Runtime configuration is invalid. |

## Status Mapping

| Failure | Status |
| --- | --- |
| Missing route | `404 Not Found` |
| Missing static asset | `404 Not Found` text response |
| Unsafe static asset path | `400 Bad Request` text response |
| Invalid request | `400 Bad Request` |
| Wrong method | `405 Method Not Allowed` |
| CSRF validation failure | `403 Forbidden` |
| Rate limit exceeded | `429 Too Many Requests` |
| Render failure | `500 Internal Server Error` |
| Event handler failure | `500 Internal Server Error` |
| Unknown internal failure | `500 Internal Server Error` |

Failed internal runtime operations must not return `200 OK`.

Static asset misses are not treated as UJFE page render failures. Requests such as `/poster.png`, `/demo.mp4`, and `/audio.mp3` bypass page rendering and return a normal static asset `404` without severe route error stack traces.

## Logging

`ErrorResponseRenderer` logs failures server-side with safe metadata:

```text
event=ujfe.http_error
code=UJFE_RENDER_ERROR
httpStatus=500
adapter=servlet
method=GET
path=/dashboard
phase=render
requestId=req-123
exceptionType=java.lang.IllegalStateException
```

The exception object is attached to the server-side log record so the stack trace is available to operators. Logs do not include request bodies, cookies, CSRF tokens, authorization headers, or session secrets.

## Runtime `onError(...)` Hooks

Runtime errors continue to flow through `RuntimeActionRegistry.onError(...)`. The hook receives a `RuntimeErrorContext` with structured metadata such as:

- `errorCode`;
- `httpStatus`;
- runtime phase;
- route path or endpoint path;
- event id when applicable;
- request metadata when the adapter reports an adapter-level failure.

The hook can log or observe failures, but it does not control the HTTP response body. If an `onError(...)` action itself fails, UJFE logs the hook failure server-side and still returns the generic safe response.

## Adapter Reuse

Netty, Servlet, and Spring adapters all use the same `ErrorResponseRenderer` and `UjfeErrorCode` model. Adapter code is responsible for:

- passing method, path, adapter name, and request id;
- applying standard security headers;
- preserving `Retry-After` for rate limited responses;
- avoiding direct `exception.getMessage()` response bodies.
