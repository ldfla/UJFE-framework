# Runtime Performance

UJFE keeps runtime performance policies generic so applications can decide how
far to cache each route.

## Internal Assets

The built-in live runtime assets use HTTP cache metadata in every adapter:

- `/_ujfe/client.js` returns a short public cache window plus `ETag`.
- `/_ujfe/dev.js` returns `no-cache` plus `ETag`, so development tools can
  revalidate without serving stale code blindly.
- `/_ujfe/css` returns private, revalidated CSS because generated CSS can
  depend on theme/session configuration.

Adapters return `304 Not Modified` when `If-None-Match` matches the current
asset ETag.

These are framework-owned runtime assets. Application-owned files such as
`/assets/app.js`, `/assets/app.css`, images, audio, video, fonts, and PDFs are
still served by the host platform. UJFE renders their URLs but does not read
arbitrary files from disk.

## Route Cache Intent

Routes are dynamic by default and receive:

```text
Cache-Control: no-store
```

Applications can opt into route-level cache headers with `RenderMode`:

```java
router.register(new RouteDefinition("/", HomePage::new)
    .withRenderMode(RenderMode.staticPage(Duration.ofMinutes(5))));
```

Available modes:

```java
RenderMode.dynamic();                                      // no-store
RenderMode.cached(Duration.ofSeconds(30));                 // private
RenderMode.staticShell(Duration.ofSeconds(30));            // private shell
RenderMode.staticPage(Duration.ofMinutes(5));              // public
RenderMode.staleWhileRevalidate(
        Duration.ofSeconds(60),
        Duration.ofMinutes(5));                            // public + stale
```

Useful defaults:

- `RenderMode.dynamic()` for private or highly interactive pages.
- `RenderMode.cached(...)` for private browser-cacheable pages.
- `RenderMode.staticShell(...)` for authenticated shells whose widgets update
  through live events or API calls.
- `RenderMode.staticPage(...)` only for public, non-sensitive content.
- `RenderMode.staleWhileRevalidate(...)` for public content that can tolerate
  brief staleness.

`revalidateOn(...)` emits `X-UJFE-Revalidate-On` as metadata for gateways,
CDNs, or application-level invalidation hooks.

## Deployment Notes

For published applications, keep framework and deployment concerns separate:

- Serve unrelated static files through the container, Spring resource handlers,
  CDN, or reverse proxy.
- Use versioned URLs for application-owned CSS/JS/images when long cache
  windows are needed.
- Keep `RenderMode.staticPage(...)` away from user-specific HTML.
- Let UJFE manage internal endpoints, but let the host platform own compression,
  TLS, HTTP/2 or HTTP/3, and CDN rules.
