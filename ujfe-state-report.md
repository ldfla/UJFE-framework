# UJFE Framework - Current State Report (0.26.0-SNAPSHOT -> v1.0.0)

> Updated 2026-05-21 23:21 -03 - Based on a local source review of the Maven reactor, docs, examples, tests, and roadmap files.

---

## 1. Current Snapshot

UJFE is now a multi-module JVM UI framework at `0.26.0-SNAPSHOT`. The current reactor contains:

- `ujfe-core`
- `ujfe-signals`
- `ujfe-router`
- `ujfe-live`
- `ujfe-servlet`
- `ujfe-spring`
- `ujfe-http`
- `ujfe-cli`
- `examples/servlet-tomcat`
- `examples`

The separate `ujfe-html` module referenced by older docs/report text no longer exists. The HTML DSL and rendering contracts live in `ujfe-core` under `ujfe.core`.

The project has moved beyond the old `0.16.0` baseline. Since then it added typed live form values, explicit live-session locking, client-state allowlists, CSS mode `NONE`, static asset handling, accessibility/SEO validation helpers, stronger CLI conversion, dependency-free observability traces, JaCoCo gates, and Spring/Servlet configuration improvements.

---

## 2. What Is Implemented

### Core HTML / Rendering (`ujfe-core`)

- Generic `Element.of(tagName)` element model for current, future, custom, SVG, and MathML elements.
- Broad HTML helper surface in `UI`, now part of `ujfe-core`.
- Standard `Node`, `Renderable`, `Component`, `TextNode`, `ComponentNode`, `UjfeContext`, and `Ujfe` context access APIs.
- Text and attribute escaping through `HtmlEscaper` and `AttributeEscaper`.
- Attribute validation that rejects unsafe names and inline `on*` attributes.
- URL sanitization through `SafeUrl` and configurable `UrlPolicy`.
- Explicit unsafe escape hatch through `UnsafeHtml` / `unsafeHtml(...)`.
- Void element enforcement through centralized metadata.
- Client state model for cookies, local storage, and session storage.
- Optional utility CSS rendering and `CssTheme`.
- Small `RestClient` / `RestResponse` helper.
- Accessibility and SEO validation helpers through `DocumentValidator`, `ValidationOptions`, `AccessibilityValidator`, and `SeoValidator`.

### Signals (`ujfe-signals`)

- Mutable `Signal<T>` and `MutableSignal<T>` with `set`, `update`, and `subscribe`.
- `Computed<T>` with lazy cached evaluation, dependency tracking, invalidation, subscribers, exception safety, and cycle detection.
- Thread-safe signal behavior covered by tests.

### Router (`ujfe-router`)

- `@Page` annotation.
- `Router`, `RouteDefinition`, `PageRenderer`, and page class validation.
- `ManualRouteSource` for explicit/AOT-friendly registration.
- `ReflectionPageScanner` for package or candidate-class discovery.
- Deterministic route order and duplicate route detection.
- Runtime-independent routing documentation and AOT roadmap.

### Live Runtime (`ujfe-live`)

- `LiveSession` render/event/state lifecycle with explicit per-session `ReentrantReadWriteLock`.
- Opaque event ids through `LiveEventRegistry`, with render-position reconciliation so stale duplicate events become safe synchronization instead of hard failures.
- Live browser bridge for click, input, change, submit, CSS replacement, root replacement, CSRF headers, and client-state synchronization.
- Typed `onInput(Consumer<String>)` and `onChange(Consumer<String>)`.
- Browser-side value capture for inputs, textarea, checkbox, radio, select, select multiple, and form submit.
- Shared `LiveHttpCodec` for JSON payload parsing, response serialization, cookie parsing, CSS class parsing, payload limits, and safe rejected-payload logging.
- CSRF protection with session token, `X-UJFE-CSRF`, and Origin/Referer same-origin validation.
- Token bucket rate limiting for `/_ujfe/event` and `/_ujfe/state`, including trusted proxy support.
- Safe JSON error responses through `ErrorResponseRenderer`, `UjfeErrorCode`, and production/development modes.
- Runtime actions: `beforeRender`, `afterRender`, `beforeEvent`, `afterEvent`, `onError`, and `contributeHead`.
- Server-side lifecycle runtime for mount/unmount tracking and session cleanup.
- CSS modes `INTERNAL`, `EXTERNAL`, and `NONE`.
- Static asset path detection/rejection for common assets and traversal attempts.
- Optional rendered-document validation in WARN or STRICT mode.

### HTTP Runtimes

- Netty runtime (`ujfe-http`) through `UjfeServer`, `UjfeHttpHandler`, and `UjfeServerConfig`.
- Jakarta Servlet runtime (`ujfe-servlet`) through `UjfeServlet`, `UjfeServletSettings`, classpath/config/env-style property support, and a Tomcat WAR example.
- Spring MVC / Boot integration (`ujfe-spring`) through auto-configuration, explicit route-owned handler mapping, security header properties, client-state properties, and validation properties.
- Hardened Spring route coexistence: UJFE claims only registered UJFE `GET` page routes and `/_ujfe/*`, while Spring MVC controllers, REST APIs, actuator-like paths, static resources, security filters, and missing routes keep Spring ownership.
- All adapters reuse shared live codec behavior and safe error rendering.

### Security Baseline

- Secure default headers across runtime adapters:
  - `X-Content-Type-Options`
  - `Referrer-Policy`
  - `X-Frame-Options`
  - `Permissions-Policy`
  - `Content-Security-Policy`
- Spring integration avoids overwriting headers already set by Spring Security.
- CSRF protection and internal endpoint rate limiting are on by default.
- UJFE Spring routes and internal endpoints continue through the normal servlet filter chain, so Spring Security can protect both page and `/_ujfe/*` paths.
- Client state is deny-all by default and exposed only through explicit allowlists.
- Static asset traversal attempts are rejected.
- Safe URL handling blocks dangerous schemes and documents current `data:image/*` limitations.

### CLI

- `ujfe convert page.html --output Page.java --type html`.
- Options include class/package names, encoding, comment policy, unsafe fallback, CSS extraction/external handling, componentization, safe class names, and debug stack traces.
- Converter handles doctype, entities, malformed input failures, attributes, boolean attributes, void elements, forms, tables, media tags, custom elements, CSS extraction, and generated Java compile coverage.

### Testing And CI

- GitHub Actions runs `./mvnw -B verify --file pom.xml` on JDK 25.
- Local validation for UJFE-026: `./mvnw clean verify` passes on the full reactor.
- JaCoCo is configured in the Maven `verify` lifecycle with aggregate reports.
- Current non-zero line coverage gates:
  - `ujfe-core`: 90%
  - `ujfe-signals`: 85%
  - `ujfe-router`: 80%
  - `ujfe-live`: 75%
  - `ujfe-http`: 70%
- The repository currently has 42 test classes across core, signals, router, live, adapters, CLI, and examples.

---

## 3. Current Gaps / Risks Before v1.0.0

### P0 - Deployment Safety

| Area | Current status | Risk |
| --- | --- | --- |
| Per-user session isolation | Not implemented as a framework-level runtime/session store. Netty examples and Spring auto-config still construct one `LiveSession` object. | A shared `LiveSession` shares page instances, signals, event ids, CSRF token, rate-limit session key, and client state across users unless applications manually isolate sessions. |
| Session lifecycle / eviction | `LiveSession.close()` exists, but no built-in session manager, TTL, eviction, or per-user cleanup policy exists. | Long-running servers need explicit lifecycle ownership to avoid retaining sessions indefinitely. |
| Spring default session scope | `UjfeSpringAutoConfiguration` creates a singleton `LiveSession` bean when no user bean exists. | Dangerous default for real multi-user Spring apps. This should not be the production default. |
| Form submit API | Browser submit values are sent as a URL-encoded string, but `onSubmit` remains `Runnable`; `LiveEventContext.submittedValues()` is still populated with `Map.of()`. | Developers cannot directly receive typed/parsed submitted values from `onSubmit` or runtime actions. |
| GET error pages | Missing routes and render failures return safe JSON error bodies, even for browser GET requests. | Browser-facing apps still need custom HTML 404/500 rendering. |

### P1 - API Completeness

| Area | Gap |
| --- | --- |
| Route parameters | Router is exact-match only. No `/users/:id`, wildcards, or typed path params. |
| Query parameters | Adapters parse query strings only for internal needs such as `/_ujfe/css`; pages do not receive query params through context. |
| Redirect / response control | No first-class redirect API for post-submit-redirect-get or runtime action response customization. |
| Per-page head/title | Global `LiveSessionConfig.title()` and global/runtime-action head contributions exist, but there is no simple per-route `title()` / `head()` page API. |
| File uploads | No multipart/form-data support in live events or adapters. |
| Server push | No SSE/WebSocket channel for server-originated re-render when background state changes. The runtime remains request-driven. |
| Partial/streaming rendering | Rendering is full-page/full-fragment replacement, not incremental streaming. |

### P1 - Security Hardening

| Area | Gap |
| --- | --- |
| CSP nonce/hash support | Injected internal `<style data-ujfe-css>` and framework scripts do not support nonces. Default CSP still needs `style-src 'unsafe-inline'` for internal CSS. |
| HSTS | `Strict-Transport-Security` is not part of `SecurityHeadersConfig.defaults()`. It can be added manually, but there is no deployment-aware default/profile. |
| SVG data URLs | `data:image/svg+xml` is currently allowed by default and documented as a hardening follow-up. |
| Session-bound security | CSRF and rate limiting are only as strong as `LiveSession` isolation. Shared sessions weaken both. |

### P2 - Documentation / Release Readiness

| Area | Gap |
| --- | --- |
| Stale versions in docs | Most primary setup docs now use `0.26.0-SNAPSHOT`; older historical changelog entries and roadmap cards still reference prior versions by design. |
| Removed module references | Primary getting-started dependency snippets now use `ujfe-core`; audit any older external docs before release. |
| Roadmap status drift | `docs/roadmap/import/ujfe-roadmap.yml` marks cards as `Todo` even though many are implemented in source and changelog. |
| Maven publication | No Maven Central release metadata is present (`distributionManagement`, signing/release config, SCM/developer/license metadata). |
| Version policy | Project is still on `0.26.0-SNAPSHOT`; no release process or compatibility policy is documented. |
| Javadoc | Public APIs still rely mostly on guides/tests rather than broad API-level Javadoc. |

### P2 - Observability

| Area | Gap |
| --- | --- |
| Metrics bridge | `RateLimitMetrics` exists, but there is no Micrometer bridge. |
| Tracing bridge | Trace IDs exist in runtime contexts, but no OpenTelemetry/Brave integration exists. |
| Render performance export | `RenderResult.renderDuration()` exists, but no collector/exporter is wired. |

---

## 4. Corrections To The Previous Report

The previous report listed several gaps that are now implemented or partially implemented:

- Form input/change values are now captured and delivered to typed handlers.
- Form submit now sends URL-encoded successful controls as the event value, though not as a parsed Java API.
- Session concurrency now uses explicit per-session locking and deterministic concurrency tests.
- `LiveSession.close()` and lifecycle cleanup exist, though eviction/session ownership is still missing.
- Client state now includes session storage and uses explicit allowlists.
- CSS has `INTERNAL`, `EXTERNAL`, and `NONE` modes.
- Static asset handling exists and rejects traversal-like paths.
- Accessibility/SEO validation helpers and runtime validation integration exist.
- CLI conversion is much broader than the old `convert page.html --out Page.java --type html` summary.
- Secure headers are configurable across adapters, not only hardcoded.

---

## 5. Recommended v1.0.0 Priority Order

```text
P0 - Must fix before real multi-user deployment
  1. Add framework-owned per-user/per-browser LiveSession isolation.
  2. Add session manager with TTL, eviction, close hooks, and adapter integration.
  3. Change Spring auto-configuration away from singleton shared LiveSession defaults.
  4. Add parsed form submit API and populate LiveEventContext.submittedValues().
  5. Add browser-facing HTML 404/500 page support.

P1 - Core app-development completeness
  6. Route parameters and typed path param access.
  7. Query parameter access in render/event context.
  8. Redirect/response control API.
  9. Per-page title/head metadata API.
  10. CSP nonce/hash support for injected style/script tags.
  11. HSTS configuration/profile support.

P2 - Adoption and release readiness
  12. Finish stale-doc audit and keep `ujfe-core` dependency guidance consistent.
  13. Synchronize roadmap/import statuses with actual implementation.
  14. Define versioning and release policy.
  15. Add Maven Central publication metadata and release workflow.
  16. Expand Javadoc for primary public APIs.
  17. Add Micrometer metrics bridge.

P3 - Post-1.0.0 candidates
  18. SSE/WebSocket server push.
  19. Multipart/file uploads.
  20. Incremental/streaming rendering.
  21. `ujfe new`, `ujfe dev`, and `ujfe build` CLI commands.
  22. OpenTelemetry integration.
```

---

## 6. Bottom Line

The HTML core, signals, router, live event runtime, adapters, validation helpers, CLI converter, security defaults, tests, and CI are all meaningfully developed.

The main blocker for v1.0.0 is no longer basic framework capability. It is production session architecture: UJFE needs safe per-user session ownership, lifecycle/eviction, and Spring defaults that cannot accidentally share state across users. After that, the highest-value work is route/query APIs, redirect/head APIs, HTML error pages, CSP nonce support, and documentation/release cleanup.
