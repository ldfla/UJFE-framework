# Cross-Site Request Forgery (CSRF) Protection

UJFE provides built-in CSRF protection for all live event endpoints (`/_ujfe/event` and `/_ujfe/state`) that mutate server-side state.

## Default Security Behavior
By default, UJFE requires a session-specific, cryptographically strong CSRF token for all mutating live event requests. If a request does not contain a valid token, or if the request originates from a different origin, it will be rejected with a `403 Forbidden` response and safely logged without exposing internal details.

## Token Lifecycle
The token is generated securely per `LiveSession` instance using `java.security.SecureRandom`. It is embedded into the initial HTML document within the `<head>` tag as a meta attribute:

```html
<meta name="ujfe-csrf-token" content="<secure-token>">
```

Tokens are never shared across unrelated live sessions and are not stored in cookies or URLs.

## Transport and Validation
The client script (`ujfe-client.js`) automatically extracts the token from the meta tag and includes it in the `X-UJFE-CSRF` HTTP header for all `fetch` requests sent to the internal endpoints.

The server validates this header against the session token. Validation failures (missing token, invalid token, session mismatch) result in immediate rejection.

## Origin and Referer Validation
As an additional layer of defense-in-depth against cross-origin POST attacks, UJFE strictly validates the `Origin` and `Referer` HTTP headers.
If either header is present, it must match the request scheme, host, and effective port. Requests lacking both `Origin` and `Referer` headers are rejected by default.

The validation treats default ports explicitly:

- `http://example.com` is equivalent to `Host: example.com` on port `80`.
- `https://example.com` is equivalent to `Host: example.com` on port `443`.
- `http://example.com` does not match an HTTPS request.
- `http://example.com` does not match `Host: example.com:8080`.

This keeps the validation deterministic across Netty, Jakarta Servlet, and Spring MVC adapters.

## Safe Error Responses and Logging
Invalid CSRF requests are handled securely:
- Tokens are not exposed in error messages.
- Stack traces are omitted.
- No sensitive request data (cookies, bodies, authorization headers) is leaked.
- Failures are logged with a structured format including `event=ujfe.live_csrf_rejected` and metadata such as the failure category, origin validation result, and correlation ID.
- The log entry never includes the raw request body, cookies, authorization headers, or CSRF token value.

## Framework Integrations

### Spring Security

When running UJFE alongside Spring Security, it is recommended to either:
1. Ignore `/_ujfe/event` and `/_ujfe/state` in Spring Security's CSRF configuration and rely solely on UJFE's native protection (which handles the token lifecycle automatically).
2. Configure Spring Security to inject its token into UJFE's meta tag or headers manually (not recommended for most setups).

UJFE routes and `/_ujfe/*` endpoints still run through the normal Spring Security filter chain. UJFE does not bypass Spring Security authorization, authentication, or static-resource rules. See [Spring MVC integration](../integrations/spring.md) for route ownership and filter-chain behavior.

### Standalone Runtime (Servlet / Netty)
When using the standalone Netty or Servlet runtimes, the built-in CSRF validation works natively without any extra dependencies. 

## Development Override
For testing or development environments where CSRF tokens or origin headers are impractical, you can disable the protection explicitly via configuration.

**`application.properties` / `application.yml`**:
```properties
ujfe.live.disable-csrf-protection-for-development-unsafe=true
```

Or via code:
```java
LiveSessionConfig config = LiveSessionConfig.builder()
    .disableCsrfProtectionForDevelopmentUnsafe()
    .build();
```

> **WARNING**: This override is strictly for development. Disabling CSRF in production leaves your mutating endpoints vulnerable to attacks. Production configuration must not silently disable CSRF validation.
