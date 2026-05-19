# Changelog
## Unreleased

## Version 0.24.0-SNAPSHOT - 18/05/2026

- Updated the Maven project version to `0.24.0-SNAPSHOT`.
- Adjusted the CHANGELOG.
- Added JaCoCo module coverage reports, module-level coverage gates, aggregate coverage output, and local report-path logging in the Maven `verify` lifecycle.
- Established initial coverage thresholds for `ujfe-core`, `ujfe-signals`, `ujfe-router`, `ujfe-live`, and `ujfe-http`.
- Added focused coverage tests for core REST/theme behavior, router rendering/scanner error paths, example page rendering, dark-mode regressions, and BrasilAPI bank parsing without network calls.
- Improved Documentation, Signals, Lifecycle, and lifecycle resource examples with clearer developer-oriented copy and dark-mode-compatible panels, cards, code blocks, and shared surfaces.
- Documented local coverage workflows, CI enforcement, contributor guidance, example page expectations, and example dark-mode styling.

## Version 0.23.0-SNAPSHOT - 18/05/2026

- Updated the Maven project version to `0.23.0-SNAPSHOT`.
- Improved `ujfe-cli convert` with stricter HTML parsing, doctype handling, entity decoding, comment policies, explicit unsafe fallback, and actionable file/encoding errors.
- Preserved classes, generic attributes, boolean attributes, forms, tables, media tags, and custom elements in generated UJFE DSL output.
- Added converter options for `--output`, `--class-name`, `--package`, `--comments`, `--unsafe-fallback`, `--css extract`, `--css-class-name`, `--componentize`, `--encoding`, `--safe-class-name`, and `--debug`.
- Added generated CSS class output for inline and readable local stylesheet extraction, deterministic componentization for larger pages, and compile coverage for generated Java.

## Version 0.22.0-SNAPSHOT - 18/05/2026

- Updated the Maven project version to `0.22.0-SNAPSHOT`.
- - Added adapter-independent accessibility and SEO validation helpers in `ujfe-core`.
- Added `ValidationMode` support for `OFF`, `WARN`, and `STRICT`, with runtime validation off by default.
- Added opt-in live runtime validation through `LiveSessionConfig`, Servlet properties, and Spring Boot validation properties.
- Added stable validation findings with rule ids, severities, categories, messages, suggestions, element names, attributes, and selector-like locations.
- Added documentation for accessibility validation, SEO validation, shared validation helpers, and strict validation in tests/CI.

## Version 0.21.0-SNAPSHOT - 18/05/2026

- Updated the Maven project version to `0.21.1-SNAPSHOT`.
- Updated Netty to version 4.1.133-Final due DNS Cache Poisoning / Domain Validation Bypass / Denial of Service / Malformed DNS Packets
- Vulnerability Type: CWE-20: Improper Input Validation / CWE-626: Null Byte Interaction Error / CWE-400: Uncontrolled Resource Consumption

## Version 0.21.0-SNAPSHOT - 18/05/2026

- Updated the Maven project version to `0.21.0-SNAPSHOT`.
- Consolidated the HTML DSL, element model, safe URL handling, utility CSS renderer, and HTML tests into `ujfe-core`; removed the separate HTML Maven module from the reactor.
- Added `CssMode.NONE` and completed deterministic behavior for `INTERNAL`, `EXTERNAL`, and `NONE` CSS modes while preserving HTML `class` attributes in every mode.
- Changed `externalStylesheet(...)` to render configured stylesheet links only in `CssMode.EXTERNAL`; `NONE` renders no internal or external framework CSS.
- Added shared static asset routing detection for common images, video, audio, fonts, CSS, JavaScript, source maps, and data/text assets.
- Updated Netty, Servlet, and Spring adapters so `/poster.png`, `/demo.mp4`, `/audio.mp3`, and other asset-like requests bypass page rendering and return safe static asset responses.
- Added static asset path traversal rejection for unsafe or encoded traversal paths.
- Added tests for CSS mode behavior, static asset routing, adapter behavior, and the HTML/core consolidation.
- Documented CSS modes, static asset routing, runtime routing order, and the canonical core HTML APIs.

## Version 0.20.0-SNAPSHOT - 18/05/2026

- Updated the Maven project version to `0.20.0-SNAPSHOT`.
- Added explicit `ClientStatePolicy` allowlists for cookies, local storage keys, and session storage keys.
- Extended `ClientState` and `Ujfe` with session storage access while preserving cookie and local storage APIs.
- Updated the live browser bridge to collect only policy-allowed client state keys and added server-side filtering before state reaches render or event handlers.
- Added Servlet and Spring configuration support for client state allowlists.
- Updated the example app to allow and display `ujfe_demo`, `ujfe.theme`, and `ujfe.tab`.
- Added direct tests for allowed/disallowed client state and documented merge semantics for `ClientState`.
- Documented client state synchronization and security guidance in `docs/client-state.md` and `docs/security/client-state.md`.

## Version 0.19.0-SNAPSHOT - 17/05/2026

- Updated the Maven project version to `0.19.0-SNAPSHOT`.
- Added typed `onInput(Consumer<String>)` and `onChange(Consumer<String>)` live event handlers while preserving existing runnable handlers.
- Extended live event payloads with a safe string `value` field and wired Netty, Servlet, and Spring adapters through the shared codec.
- Updated the browser bridge to capture input, change, checkbox, radio, select multiple, textarea, and form submit values.
- Preserved opaque event ids and native submit prevention for live submit handlers.
- Added live event tests for click, input, change, submit, value payloads, missing ids, and handler failures.
- Added a runnable forms example under `/forms` and documented live form event semantics in `docs/live-events.md`.

## Version 0.18.0-SNAPSHOT - 17/05/2026

- Updated the Maven project version to `0.18.0-SNAPSHOT`.
- Replaced `LiveSession` method-level `synchronized` locking with an explicit per-session `ReentrantReadWriteLock`.
- Serialized same-session render, live event, client state, and lifecycle mutations through the session write lock while keeping independent sessions isolated.
- Added render-scoped live event reconciliation so concurrent events from the same rendered page remain resolvable after a re-render.
- Added stress-style concurrency tests for same-session events, independent sessions, signal update consistency, and coherent HTML output under contention.
- Documented the live runtime concurrency model in `docs/runtime/concurrency.md`.

## Version 0.17.0-SNAPSHOT - 17/05/2026

- Updated the Maven project version to `0.17.0-SNAPSHOT`.
- Added `SecurityHeadersConfig` for configurable secure HTTP headers across UJFE runtime adapters.
- Enabled secure default headers for standalone responses: `X-Content-Type-Options`, `Referrer-Policy`, `X-Frame-Options`, `Permissions-Policy`, and `Content-Security-Policy`.
- Added explicit Java, Servlet, and Spring configuration paths for overriding or disabling framework-managed headers.
- Kept the default CSP compatible with the same-origin `/_ujfe/client.js` runtime script without requiring inline application JavaScript.
- Updated the Spring adapter to avoid overwriting headers already set by Spring Security.
- Documented secure HTTP header behavior in `docs/security/headers.md`.

## Version 0.16.0-SNAPSHOT - 17/05/2026

- Updated the Maven project version to `0.16.0-SNAPSHOT`.
- Added shared safe error response rendering through `ErrorResponseRenderer`, `UjfeErrorCode`, and JSON error response bodies.
- Updated Netty, Jakarta Servlet, and Spring MVC adapters to stop returning raw exception messages and to use stable UJFE error codes.
- Added production-safe mappings for route misses, render failures, live event failures, payload validation, CSRF failures, and rate limit rejections.
- Added explicit development diagnostics with `LiveSessionConfig.builder().enableDevelopmentErrorDetailsUnsafe()` and Servlet property/YAML support.
- Routed safe error metadata through runtime `onError(...)` hooks without leaking details to clients.
- Documented the shared safe error response policy in `docs/security/error-handling.md`.

## Version 0.15.0-SNAPSHOT - 17/05/2026

- Added application-level token bucket rate limiting for `/_ujfe/event` and `/_ujfe/state`.
- Added session-based rate limit keys with safe IP fallback and explicit trusted proxy handling.
- Added `429 Too Many Requests` responses with `Retry-After` for internal endpoint rate limit violations.
- Added safe structured logging and runtime-neutral counters for allowed and rejected internal endpoint requests.
- Documented internal endpoint rate limiting in `docs/security/rate-limiting.md`.

## Version 0.14.0-SNAPSHOT - 17/05/2026

- Standardized live HTTP JSON parsing, validation, serialization, payload limits, safe errors, and rejected-payload logging in `LiveHttpCodec`.
- Updated Netty, Jakarta Servlet, and Spring MVC runtime adapters to reuse the shared live codec for `/_ujfe/event` and `/_ujfe/state`.
- Added a default live JSON payload limit of `1,048,576` bytes with runtime-specific configuration hooks.
- Added safe validation errors for empty bodies, empty JSON objects, malformed JSON, missing `eventId`, missing `clientState`, and oversized payloads.
- Added security-focused tests for malformed, incomplete, oversized, and valid live payloads across the shared codec and supported HTTP adapters.
- Documented live event payloads, payload limit policy, safe error responses, logging metadata, and runtime reuse expectations in `docs/live-events.md`.
- Added CSRF protection for `/_ujfe/event` and `/_ujfe/state` mutating live endpoints using a session-specific token injected via meta tag.
- Added strict `Origin` and `Referer` header validation against request scheme, host, and effective port as a defense-in-depth CSRF measure.
- Introduced `LiveHttpRequestMetadata` to securely transfer HTTP context from Netty, Servlet, and Spring adapters to `LiveSession`.
- Introduced `LiveCsrfException` and safe structured logging for CSRF failures without leaking tokens.
- Added configuration option `disableCsrfProtectionForDevelopmentUnsafe` for local development.
- Documented CSRF protection architecture in `docs/security/csrf.md`.

## Version 0.13.0-SNAPSHOT - 17/05/2026

- Fixed Live HTTP JSON Codec issues under [UJFE-013].

## Version 0.12.0-SNAPSHOT - 17/05/2026

- Updated the Maven project version to `0.12.0-SNAPSHOT`.
- Added the `ujfe-servlet` module with `UjfeServlet` for standalone Jakarta Servlet deployments without Spring Boot or Netty.
- Implemented support for UJFE pages, `/_ujfe/client.js`, `/_ujfe/dev.js`, `/_ujfe/css`, `/_ujfe/event`, and `/_ujfe/state` in the Servlet runtime.
- Reused `LiveHttpCodec` for live JSON payloads, event ids, client state, cookies, and CSS class parsing.
- Centralized shared UJFE internal endpoint paths and HTTP security headers in `ujfe-live`.
- Updated Netty and Spring adapters to use the shared internal path and security header metadata.
- Added explicit route claiming through `UjfeServlet.handles(method, path)` and route-aware request handling so unrelated routes are not rendered as UJFE pages.
- Added support for servlet init parameters, servlet context parameters, `application.properties`, and `application.yml` configuration.
- Added Servlet unit tests with Jakarta Servlet mocks for page rendering, internal endpoints, event/state handling, route claiming, wrong methods, context attributes, and properties/YAML settings.
- Documented the standalone Servlet runtime in `docs/runtime/servlet.md` with setup, route ownership, configuration, endpoint behavior, security notes, and troubleshooting guidance.
- Added a plain Servlet/Tomcat example under `examples/servlet-tomcat`.

## Version 0.11.0-SNAPSHOT - 17/05/2026

- Updated the Maven project version to `0.11.0-SNAPSHOT`.
- Added the `RouteSource` abstraction for deterministic route metadata sources.
- Added `ManualRouteSource` for explicit, reflection-minimized route registration and future AOT-generated metadata.
- Added `ReflectionPageScanner` for deterministic reflection-based route discovery from candidate classes or packages.
- Added route validation for duplicate paths, invalid route paths, abstract pages, invalid render methods, missing constructors, and invalid page classes.
- Updated `Router` to merge route sources, preserve registration order, and fail explicitly on duplicate routes instead of overwriting silently.
- Removed the legacy scanner adapter in favor of explicit `RouteSource` implementations.
- Added route discovery tests for scanners, manual sources, ordering, duplicates, invalid pages, invalid paths, empty sources, package scanning, and metadata.
- Documented router behavior in `docs/router.md` and the AOT route metadata direction in `docs/aot-roadmap.md`.
- Added router source examples under `examples/router` and updated the example app to wire routes through `ManualRouteSource`.

## Version 0.10.0-SNAPSHOT - 17/05/2026

- Updated the Maven project version to `0.10.0-SNAPSHOT`.
- Defined computed signal semantics for lazy evaluation, caching, dependency tracking, invalidation, subscribers, exceptions, circular dependencies, and concurrency.
- Made `Computed<T>` a read-only `Signal<T>` with subscriber support and explicit `UnsupportedOperationException` for mutation attempts.
- Added automatic dependency tracking between mutable signals and computed signals, including nested computed invalidation propagation.
- Added successful-result caching with lazy recomputation after dependency changes.
- Added `ComputedCycleException` for predictable circular computed dependency failures.
- Added tests for computed laziness, cache reuse, invalidation collapse, subscriber behavior, nested dependencies, dynamic dependency cleanup, exception handling, cycle detection, and concurrent access.
- Documented the signal model in `docs/signals.md`.
- Added a runnable signals example route at `/signals` and documented it under `examples/signals`.

## Version 0.9.0-SNAPSHOT - 16/05/2026

- Updated the Maven project version to `0.9.0-SNAPSHOT`.
- Added the `ujfe.runtime.lifecycle` package with `LifecycleRuntime`, `LifecycleRegistry`, `LifecycleTracker`, `MountedComponent`, lifecycle state, context, and event classes.
- Integrated `Lifecycle` mount/unmount callbacks into `LiveSession` rendering, route transitions, and session cleanup.
- Added `ComponentNode` and `component(...)` helper support so nested components can participate in lifecycle tracking explicitly.
- Preserved current route page instances across stable live re-renders to avoid duplicate mounts and state loss.
- Added session cleanup through `LiveSession.close()` and wired HTTP server shutdown to close the live session.
- Routed lifecycle failures through runtime `onError(...)` actions using `RuntimePhase.LIFECYCLE`.
- Added lifecycle tests for stable re-rendering, route transitions, nested components, deterministic cleanup, concurrency, callback failures, and empty registries.
- Added a runnable lifecycle example route at `/lifecycle` and documented it under `examples/lifecycle`.
- Documented the server-side lifecycle model in `docs/lifecycle.md`.

## Version 0.8.0-SNAPSHOT - 16/05/2026

- Updated the Maven project version to `0.8.0-SNAPSHOT`.
- Introduced server-side runtime extension points in the new `ujfe.runtime.action` package.
- Added `RuntimeActionRegistry` and `RuntimeActionRegistryBuilder` with builder API for registering actions across six extension points.
- Added `BeforeRenderAction` and `AfterRenderAction` for rendering lifecycle participation.
- Added `BeforeEventAction` and `AfterEventAction` for live event lifecycle participation.
- Added `ErrorAction` for centralized runtime error handling.
- Added `HeadContributionAction` for dynamic document `<head>` contributions.
- Added `ActionOrder` with `FIRST`, `EARLY`, `NORMAL`, `LATE`, and `LAST` priority constants.
- Added `ActionChain` for deterministic, ordered action execution.
- Added `RenderContext`, `RenderResult`, `LiveEventContext`, `LiveEventResult`, and `RuntimeErrorContext` context objects with defensive metadata copies.
- Added `HeadContributionContext` as a mutable collector for head node contributions.
- Added `RuntimePhase` enum: `RENDER`, `EVENT`, `HEAD_CONTRIBUTION`, `INTERNAL`.
- Integrated `RuntimeActionRegistry` into `LiveSessionConfig` via `runtimeActions(registry)` builder method.
- Wired hooks into `LiveSession`: `beforeRender`/`afterRender` in `renderPath()`, `beforeEvent`/`afterEvent` in `handleEvent()`, head contributions in `renderDocument()`.
- Empty registry is the default; all existing constructors and behavior remain backward-compatible.
- Exceptions in actions are routed to `onError` and re-thrown; exceptions in `onError` are logged to stderr without recursion.
- Added trace IDs (UUID) and timestamps to all runtime operations.
- Documented extension points in `docs/runtime-extension-points.md`.
- Added `RuntimeActionsPage` example page at `/runtime-actions`.


## Version 0.7.0-SNAPSHOT - 16/05/2026

- Updated the Maven project version to `0.7.0-SNAPSHOT`.
- Introduced `UrlPolicy` for configurable URL scheme handling with secure defaults.
- Refactored `SafeUrl` to delegate scheme decisions to the active `UrlPolicy`.
- Blocked `javascript:` and `vbscript:` schemes unconditionally.
- Made `http:`, `mailto:`, and `tel:` schemes configurable through `UrlPolicy.builder()`.
- Added `background` to the set of URL-bearing attributes sanitized by `SafeUrl`.
- Allowed `data:image/svg+xml` and `data:image/avif` MIME types for data URLs.
- Documented the `data:image/*` MIME prefix validation limitation.
- Documented the SafeUrl policy in `docs/security/safe-url.md`.

## Version 0.6.0-SNAPSHOT - 16/05/2026

- Updated the Maven project version to `0.6.0-SNAPSHOT`.
- Hardened the generic attribute model with inline event handler blocking.
- Blocked `onclick`, `onload`, `onerror`, and all `on*` inline event handler attributes by default.
- Validated attribute names to reject whitespace, `<`, `=`, and other dangerous characters before rendering.
- Allowed `aria-*`, `data-*`, and `hx-*` attributes through the generic `attr(...)` API.
- Documented the attribute validation rules in `docs/html/attributes.md`.
- Documented the security rationale for inline event handler blocking in `docs/security/attribute-validation.md`.

## Version 0.5.0-SNAPSHOT - 16/05/2026

- Updated the Maven project version to `0.5.0-SNAPSHOT`.
- Added the explicit unsafe raw HTML APIs `unsafeHtml(...)` and `UnsafeHtml.of(...)`.
- Documented the XSS risk and acceptable use cases for trusted raw HTML rendering.
- Added tests proving safe text remains escaped and unsafe HTML renders raw content only through unsafe-named APIs.
- Refined the example documentation page with stronger security guidance and a cleaner visual structure.

## Version 0.4.0-SNAPSHOT - 16/05/2026

- Updated the Maven project version to `0.4.0-SNAPSHOT`.
- Added reusable `HtmlElementMetadata` for standard HTML void element behavior.
- Rendered HTML void elements without closing tags and rejected children with an explicit error.
- Reused centralized void element metadata in the CLI HTML parser.
- Documented HTML void element rendering rules in `docs/html/void-elements.md`.

## Version 0.3.0-SNAPSHOT - 16/05/2024

- Updated the Maven project version to `0.3.0-SNAPSHOT`.
- Added standard HTML helper coverage tests for required tags, void rendering, normal closing tags, text escaping, and nested children.
- Documented the standard helper API in `docs/html/HELPERS.md` and the README helper table.
- Updated Netty to `4.1.132.Final` to keep the HTTP runtime on the current non-vulnerable Netty line.
- Removed empty-string snippet concatenation patterns from example pages.

## Version 0.2.0-SNAPSHOT - 16/05/2024

- Updated the Maven project version to `0.2.0-SNAPSHOT`.
- Added centralized HTML tag-name validation for `Element.of(...)`.
- Added generic SVG and MathML namespace factories through `Element.svg(...)` and `Element.mathMl(...)`.
- Documented generic element compatibility for current HTML, future HTML, custom elements, Web Components, SVG, and MathML.

## Version 0.1.0-SNAPSHOT - 16/05/2024

- Consolidated the UJFE project identity around the tagline "Modern Reactive UI Framework for the JVM".
- Documented UJFE's core principles: HTML-first, standards-first, Java-first, server-first, safe by default, CSS agnostic, and no required Node.js toolchain.
- Documented UJFE's non-goals in the README, vision, and principles documentation.
- Aligned root project objectives and Maven metadata with the UJFE technical name and tagline.
