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

## Recommended Paths

Prefer explicit asset locations:

```html
<img src="/assets/poster.png" alt="Poster">
<video src="/assets/demo.mp4" controls></video>
<audio src="/assets/audio.mp3" controls></audio>
<link rel="stylesheet" href="/assets/app.css">
```

The standalone Netty adapter currently treats common root-level asset paths as assets too, including `/poster.png`, `/demo.mp4`, and `/audio.mp3`, so documentation examples and browser media probes do not become page routing errors.

## Routing Precedence

Runtime adapters follow this order:

1. UJFE internal endpoints under `/_ujfe/*`.
2. Static asset-like requests.
3. Registered UJFE page routes.
4. Safe route `404`.

Static assets never shadow internal endpoints such as `/_ujfe/event`, `/_ujfe/state`, `/_ujfe/client.js`, or `/_ujfe/css`.

## Adapter Behavior

Standalone Netty returns safe static asset responses directly. Missing assets produce text `404` responses instead of JSON route errors.

Servlet deployments can delegate static assets to the container by not mapping UJFE over those paths. If a static asset request reaches `UjfeServlet`, it still returns the same safe static asset response instead of invoking page rendering.

Spring MVC delegates unclaimed static assets through normal Spring resource handling. If a static asset request reaches `UjfeSpringHandler` directly, it also returns the shared safe static asset response.

## Security

Static asset paths are validated before handling. Requests containing path traversal markers, encoded traversal sequences, backslashes, or null characters are rejected safely.

UJFE does not serve arbitrary files from the filesystem in this issue. That avoids exposing source files, build files, environment files, keys, or other sensitive local paths.
