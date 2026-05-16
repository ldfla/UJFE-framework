# UJFE Vision

**Modern Reactive UI Framework for the JVM**

UJFE is a standards-first, Java-first, HTML-first, server-first UI framework for JVM applications. Its purpose is to let Java teams build reactive web interfaces using real HTML, normal CSS integration points, and server-owned application state.

UJFE treats the browser as the Web Platform, not as a target for a proprietary UI dialect. Pages render as standard HTML, events are routed back to Java handlers, and generated output should remain inspectable by any developer who understands HTML.

## Positioning

UJFE is intended for teams that want productive Java UI development without making a browser-side JavaScript framework the center of the application architecture. It can be used as a standalone JVM web UI framework, as a Spring Boot integration, or as a Spring MVC route participant.

UJFE complements existing server frameworks. In a Spring Boot or Spring MVC application, Spring continues to own the application runtime, filters, security, configuration, static resources, and non-UJFE routes. UJFE owns only the routes and internal endpoints registered for UJFE pages.

## Scope

UJFE focuses on:

- Rendering standard HTML from Java.
- Keeping page state and event handling on the server by default.
- Supporting live interactions without requiring application authors to write browser-side JavaScript for normal workflows.
- Providing safe rendering defaults for text, attributes, URLs, and event identifiers.
- Supporting multiple CSS strategies through standard HTML attributes.
- Avoiding a required Node.js, npm, TypeScript, Babel, or bundler pipeline.

## Non-Goals

- UJFE is not a browser-side JavaScript framework clone.
- UJFE is not a proprietary HTML dialect.
- UJFE is not a mandatory CSS framework.
- UJFE is not a replacement for Spring Boot or Spring MVC.

## Success Criteria

UJFE succeeds when Java developers can build modern server-rendered web interfaces while keeping the result understandable as normal HTML, deployable in normal JVM environments, and compatible with normal CSS and server framework choices.
