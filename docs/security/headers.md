# Secure HTTP Headers

UJFE standalone runtimes emit secure HTTP response headers by default. The headers are a framework-level baseline for browser hardening, not a replacement for reverse proxy, CDN, application firewall, or Spring Security policy.

## Default Headers

The default runtime header set is:

| Header | Default value |
| --- | --- |
| `X-Content-Type-Options` | `nosniff` |
| `Referrer-Policy` | `strict-origin-when-cross-origin` |
| `X-Frame-Options` | `DENY` |
| `Permissions-Policy` | `geolocation=(), microphone=(), camera=()` |
| `Content-Security-Policy` | UJFE-compatible same-origin policy |

The default Content Security Policy starts from `default-src 'self'` and keeps the UJFE client compatible with an external framework script:

```http
Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; media-src 'self' data: https:; object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'
```

`script-src 'self'` is sufficient for the built-in `/_ujfe/client.js` endpoint because UJFE does not require inline application JavaScript. The default `style-src` allows inline style blocks because the current internal CSS mode can emit generated framework CSS directly in the document head. Applications that use external stylesheets can tighten the policy.

## Java Configuration

Configure headers through `LiveSessionConfig`:

```java
import ujfe.live.LiveSessionConfig;
import ujfe.live.SecurityHeadersConfig;

LiveSessionConfig config = LiveSessionConfig.builder()
        .securityHeaders(SecurityHeadersConfig.builder()
                .header(SecurityHeadersConfig.REFERRER_POLICY, "same-origin")
                .header(SecurityHeadersConfig.CONTENT_SECURITY_POLICY, "default-src 'self'")
                .build())
        .build();
```

For a small override, use `securityHeader(...)`:

```java
LiveSessionConfig config = LiveSessionConfig.builder()
        .securityHeader(SecurityHeadersConfig.X_FRAME_OPTIONS, "SAMEORIGIN")
        .build();
```

Advanced deployments can explicitly disable framework-managed headers:

```java
LiveSessionConfig config = LiveSessionConfig.builder()
        .disableSecurityHeaders()
        .build();
```

Disable the headers only when another trusted layer owns the full policy.

## Servlet Configuration

The standalone Jakarta Servlet runtime reads the same policy from servlet init parameters, servlet context parameters, `application.properties`, `application.yml`, or `application.yaml`.

```properties
ujfe.security.headers.enabled=true
ujfe.security.headers.referrer-policy=same-origin
ujfe.security.headers.content-security-policy=default-src 'self'; script-src 'self'
ujfe.security.headers.x-frame-options=SAMEORIGIN
ujfe.security.headers.permissions-policy=geolocation=()
```

Disable UJFE-managed headers explicitly:

```properties
ujfe.security.headers.enabled=false
```

YAML example:

```yaml
ujfe:
  security:
    headers:
      enabled: true
      referrer-policy: same-origin
      content-security-policy: "default-src 'self'; script-src 'self'"
      x-frame-options: SAMEORIGIN
```

## Spring Integration

`ujfe-spring` also accepts Spring Boot properties:

```properties
ujfe.security.headers.referrer-policy=same-origin
ujfe.security.headers.content-security-policy=default-src 'self'; script-src 'self'
```

When Spring Security has already set a response header, UJFE does not overwrite it. This keeps UJFE compatible with applications where Spring Security owns the HTTP header policy.

## Operational Notes

- Public UJFE pages and internal `/_ujfe/*` responses receive the configured headers.
- The default CSP supports `/_ujfe/client.js` as a same-origin external script.
- UJFE does not require inline application JavaScript.
- If an application serves assets from a CDN, extend `script-src`, `style-src`, `img-src`, or `font-src` deliberately.
- If an application embeds UJFE pages in trusted frames, replace `X-Frame-Options: DENY` and update `frame-ancestors` consistently.
- Header values reject blank strings and line breaks to avoid malformed HTTP headers.
