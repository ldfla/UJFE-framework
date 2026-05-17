# Internal Endpoint Rate Limiting

UJFE includes application-level rate limiting for mutating internal live endpoints as a defense-in-depth control.

This protection is enabled by default for:

- `POST /_ujfe/event`
- `POST /_ujfe/state`

It does not apply to public page `GET` routes, `/_ujfe/client.js`, `/_ujfe/dev.js`, or `/_ujfe/css`.

## Why It Exists

Live endpoints can receive frequent requests from browser events, state synchronization, and runtime mutations. A bug in client code, an accidental event loop, malformed automation, or basic abuse can generate excessive request volume.

UJFE's limiter is intentionally application-level. It complements, but does not replace:

- reverse proxy limits
- CDN or WAF policies
- API gateway controls
- infrastructure DDoS protection
- global IP-based rate limits

Infrastructure still owns network-wide abuse protection. UJFE owns runtime-aware protection for its internal live endpoints.

## Default Behavior

UJFE uses a token bucket limiter.

Default values:

| Setting | Default |
| --- | ---: |
| Enabled | `true` |
| Bucket capacity | `120` requests |
| Refill amount | `60` tokens |
| Refill period | `1 minute` |

The defaults are intended to allow normal interactive usage while limiting tight loops and simple request floods.

When a request exceeds the limit, UJFE returns:

```http
HTTP/1.1 429 Too Many Requests
Retry-After: <seconds>
Content-Type: text/plain; charset=utf-8

Rate limit exceeded.
```

`Retry-After` is included when the token bucket can safely calculate the next retry window.

## Limiting Key

UJFE selects the limiting key deterministically:

1. Use the live session id when a valid `LiveSession` is available.
2. Fall back to the resolved client IP when no session id is available.

This makes normal live applications rate limited by session rather than by the entire application or by route alone. Different live sessions have independent buckets.

## IP Fallback

IP fallback is used when no valid live session identity is available. This fallback is fail-closed: malformed or missing session identity must not bypass rate limiting.

If the remote address cannot be resolved, UJFE uses a shared `unknown` fallback key. That is conservative: unknown clients share the same fallback bucket instead of bypassing the limiter.

## Trusted Proxy Headers

UJFE does not trust proxy headers by default.

These headers are ignored unless the direct remote address is explicitly configured as a trusted proxy:

- `Forwarded`
- `X-Forwarded-For`
- `X-Real-IP`

When the direct remote address is trusted, UJFE resolves the client IP from proxy headers in this order:

1. `Forwarded`
2. `X-Forwarded-For`
3. `X-Real-IP`
4. direct remote address

This prevents clients from bypassing the fallback IP limit by spoofing `X-Forwarded-For`.

## Java Configuration

```java
LiveSessionConfig config = LiveSessionConfig.builder()
        .internalEndpointRateLimit(
                120,                 // bucket capacity
                60,                  // refill tokens
                Duration.ofMinutes(1) // refill period
        )
        .trustedProxy("10.0.0.10")
        .build();

LiveSession session = new LiveSession(router, config);
```

Rate limiting can be disabled explicitly:

```java
LiveSessionConfig config = LiveSessionConfig.builder()
        .internalEndpointRateLimitingEnabled(false)
        .build();
```

Disable it only when another production-grade control is handling the same internal endpoints. Frontend throttling is not a security boundary.

## Servlet Configuration

The standalone Jakarta Servlet runtime can read rate limit settings from `application.properties`, `application.yml`, servlet init parameters, or servlet context parameters.

```properties
ujfe.live.rate-limit.enabled=true
ujfe.live.rate-limit.capacity=120
ujfe.live.rate-limit.refill-tokens=60
ujfe.live.rate-limit.refill-period-ms=60000
ujfe.live.trusted-proxies=10.0.0.10,10.0.0.11
```

Equivalent YAML:

```yaml
ujfe:
  live:
    rate-limit:
      enabled: true
      capacity: 120
      refill-tokens: 60
      refill-period-ms: 60000
    trusted-proxies: 10.0.0.10,10.0.0.11
```

Spring and Netty integrations use the same `LiveSessionConfig` object.

## Logging And Observability

Rejected requests are logged with safe metadata only:

- endpoint path
- limiting key type: `session` or `ip`
- runtime adapter name
- retry delay when available
- request id or correlation id when available
- reason code: `rate_limit_exceeded`

UJFE does not log:

- request bodies
- cookies
- authorization headers
- CSRF tokens
- session secrets

The token bucket also exposes counters through `RateLimitMetrics`:

```java
long allowed = session.rateLimitMetrics().allowedRequests();
long rejected = session.rateLimitMetrics().rejectedRequests();
long eventRejects = session.rateLimitMetrics().rejectedRequests("/_ujfe/event");
long sessionRejects = session.rateLimitMetrics().rejectedRequests(RateLimitKeyType.SESSION);
```

These counters are intentionally runtime-neutral and can be bridged to Micrometer, OpenTelemetry, logs, or application-specific observability later.

## Runtime Order

For `/_ujfe/event` and `/_ujfe/state`, adapters should apply the internal endpoint rate limit before reading and parsing JSON request bodies.

The expected order is:

1. Build safe request metadata.
2. Apply internal endpoint rate limiting.
3. Enforce payload size limits while reading the body.
4. Parse JSON through `LiveHttpCodec`.
5. Validate CSRF through `LiveSession`.
6. Dispatch the live runtime operation.

This order rejects excessive traffic before expensive JSON parsing while preserving CSRF and payload validation behavior for accepted requests.

## Frontend Throttling

Frontend throttling or debouncing can improve user experience and reduce unnecessary traffic. It is not a security control.

Clients can be modified, bypassed, automated, or broken. Server-side rate limiting remains the authoritative boundary for UJFE internal endpoints.
