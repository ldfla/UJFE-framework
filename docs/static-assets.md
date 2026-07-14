# Static Assets

UJFE page routing is separate from static asset handling.

Asset-like requests such as:

```text
/poster.png
/demo.mp4
/audio.mp3
/assets/app.css
/static/font.woff2
```

are not sent to `LiveSession.renderDocument(...)` as page routes. Missing assets return a normal safe `404 Not Found` text response and are not logged as severe UJFE route failures.

UJFE renders references to static assets, but it is not a general-purpose static
file server. Application JavaScript, images, fonts, PDFs, videos, audio files,
and generated CSS should be served by the host platform: Spring resource
handlers, the servlet container, a reverse proxy, a CDN, or another static file
pipeline.

## Recommended Paths

Prefer explicit asset locations:

```html
<script src="/assets/app.js" defer></script>
<img src="/assets/poster.png" alt="Poster">
<picture>
  <source media="(min-width: 960px)" src="/assets/hero-wide.webp">
  <img src="/assets/hero.webp" alt="Dashboard">
</picture>
<video src="/assets/demo.mp4" controls></video>
<audio src="/assets/audio.mp3" controls></audio>
<link rel="stylesheet" href="/assets/app.css">
<object data="/assets/report.pdf" type="application/pdf"></object>
```

The standalone Netty adapter currently treats common root-level asset paths as assets too, including `/poster.png`, `/demo.mp4`, and `/audio.mp3`, so documentation examples and browser media probes do not become page routing errors.

The equivalent UJFE DSL is normal HTML:

```java
script().src("/assets/app.js").attr("defer", true);
img().src("/assets/poster.png").alt("Poster");
picture()
        .child(source()
                .attr("media", "(min-width: 960px)")
                .src("/assets/hero-wide.webp"))
        .child(img().src("/assets/hero.webp").alt("Dashboard"));
video().src("/assets/demo.mp4").poster("/assets/poster.webp")
        .controls(true)
        .preload("metadata");
audio().controls(true)
        .child(source().src("/assets/audio.mp3").type("audio/mpeg"))
        .child(track().attr("kind", "captions")
                .attr("srclang", "en")
                .src("/assets/captions.vtt"));
link().attr("rel", "stylesheet").href("/assets/app.css");
object().attr("data", "/assets/report.pdf").type("application/pdf");
embed().src("/assets/preview.pdf").type("application/pdf");
```

For application-owned JavaScript modules, UJFE can render either direct script
tags or declarative metadata primitives:

```java
LiveSessionConfig.builder()
        .head(script().src("/assets/app.js").attr("defer", true))
        .build();

clientModule("charts")
        .src("/assets/charts.js")
        .action("render", ChartRequest.class)
        .target("#chart")
        .errorTarget("#chart-error")
        .defer(true);
```

The JavaScript file itself remains application-owned.

## Routing Precedence

Runtime adapters follow this order:

1. UJFE internal endpoints under `/_ujfe/*`.
2. Static asset-like requests.
3. Registered UJFE page routes.
4. Safe route `404`.

Static assets never shadow internal endpoints such as `/_ujfe/event`, `/_ujfe/state`, `/_ujfe/client.js`, or `/_ujfe/css`.

## Adapter Behavior

Standalone Netty returns safe static asset responses directly. Missing assets produce text `404` responses instead of JSON route errors. It does not read arbitrary files from the project directory.

Servlet deployments can delegate static assets to the container by not mapping UJFE over those paths. If a static asset request reaches `UjfeServlet`, it still returns the same safe static asset response instead of invoking page rendering.

Spring MVC delegates unclaimed static assets through normal Spring resource handling. If a static asset request reaches `UjfeSpringHandler` directly, it also returns the shared safe static asset response.

In Spring MVC, prefer normal resource handling for public assets:

```java
@Override
public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/assets/**")
            .addResourceLocations("classpath:/static/assets/");
}
```

In servlet deployments, map UJFE narrowly and let the container or proxy own
static paths such as `/assets/*`.

## Cache Strategy

Application assets should use versioned URLs when they need long cache windows:

```text
/assets/app.3f7c2d.css
/assets/app.91a0b6.js
/assets/logo.20260714.svg
```

UJFE manages cache metadata only for its internal runtime endpoints under
`/_ujfe/*`. See [Runtime performance](runtime/performance.md).

## Security

Static asset paths are validated before handling. Requests containing path traversal markers, encoded traversal sequences, backslashes, or null characters are rejected safely.

UJFE does not serve arbitrary files from the filesystem. That avoids exposing source files, build files, environment files, keys, or other sensitive local paths.

URL attributes are also sanitized by the core renderer. `javascript:` and
`vbscript:` URLs are blocked. `data:image/*` is allowed by default for image
use cases; do not use data URLs for arbitrary scripts or documents.
