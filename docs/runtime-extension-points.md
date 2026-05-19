# Runtime Extension Points

## Overview

UJFE provides server-side runtime extension points that allow
applications and integrations to participate in the rendering lifecycle,
live event processing, error handling, and document head assembly.

These are **server-side Java runtime extension points**, not browser-side
JavaScript hooks. All actions execute on the JVM as part of the UJFE
rendering and event pipeline.

## Why server-side

UJFE is a server-first framework. Rendering, state management, and event
handling happen on the JVM. Runtime extension points follow the same
principle: they execute on the server, have access to Java APIs, and
never require client-side JavaScript.

## Difference from React hooks

| Aspect | React hooks | UJFE runtime actions |
|--------|-------------|---------------------|
| Execution environment | Browser | Server (JVM) |
| State management | Component-local state | Server-side signals and session state |
| Lifecycle | Component mount/unmount | Render and event pipeline |
| Registration | Inside component functions | Application startup via builder |
| Threading | Single-threaded UI | Thread-safe, synchronized |

UJFE runtime actions are not component-scoped lifecycle callbacks.
They are application-level extension points that participate in the
server rendering pipeline.

## Registration model

Actions are registered through `RuntimeActionRegistry.builder()`:

```java
RuntimeActionRegistry registry = RuntimeActionRegistry.builder()
        .beforeRender(context -> {
            // rendering preparation
        })
        .afterRender(result -> {
            // inspect rendered result
        })
        .beforeEvent(event -> {
            // authorization, tracing, validation
        })
        .afterEvent(result -> {
            // metrics, logging, auditing
        })
        .onError(error -> {
            // centralized runtime error handling
        })
        .contributeHead(head -> {
            head.add(
                    meta()
                            .attr("name", "robots")
                            .attr("content", "index,follow")
            );
        })
        .build();
```

The registry is immutable and thread-safe after construction.
`RuntimeActionRegistryBuilder` is the public builder type returned by
`RuntimeActionRegistry.builder()`.

## Wiring into the runtime

The registry is configured through `LiveSessionConfig`:

```java
LiveSessionConfig config = LiveSessionConfig.builder()
        .runtimeActions(registry)
        .lang("en")
        .title("My App")
        .build();

LiveSession session = new LiveSession(router, config);
```

Because all runtimes (pure UJFE, Netty, Servlet, Spring) use
`LiveSession`, the actions work automatically in every deployment
model.

## Ordering model

Actions execute in a deterministic order controlled by `ActionOrder`:

| Constant | Priority |
|----------|----------|
| `FIRST`  | 0        |
| `EARLY`  | 250      |
| `NORMAL` | 500      |
| `LATE`   | 750      |
| `LAST`   | 1000     |

Lower values execute first. Actions with equal priority preserve
their registration order.

```java
RuntimeActionRegistry.builder()
        .beforeRender(ActionOrder.FIRST, ctx -> log("security"))
        .beforeRender(ActionOrder.NORMAL, ctx -> log("business"))
        .beforeRender(ActionOrder.LAST, ctx -> log("metrics"))
        .build();
```

Custom priorities are available through `ActionOrder.of(int)`.

## Extension points

### Rendering

| Action | When | Context |
|--------|------|---------|
| `beforeRender` | Before page rendering | `RenderContext`: path, page, session, requestMetadata, clientState, timestamp, traceId, runtimeMetadata |
| `afterRender` | After page rendering | `RenderResult`: html, css, path, duration, traceId, headNodes, routeMetadata, cssMetadata, runtimeMetadata |

### Live events

| Action | When | Context |
|--------|------|---------|
| `beforeEvent` | Before event dispatch | `LiveEventContext`: eventId, eventType, session, requestMetadata, clientState, target, submittedValues, traceId, runtimeMetadata |
| `afterEvent` | After event dispatch | `LiveEventResult`: html, eventId, duration, traceId, eventMetadata, reRenderMetadata, clientStateMetadata, runtimeMetadata |

### Errors

| Action | When | Context |
|--------|------|---------|
| `onError` | On any runtime error | `RuntimeErrorContext`: exception, phase, path, eventId, traceId, routeMetadata, eventMetadata, requestMetadata, sessionMetadata, runtimeMetadata |

The `RuntimePhase` enum identifies where the error occurred:
`RENDER`, `EVENT`, `HEAD_CONTRIBUTION`, `INTERNAL`.

### Head contribution

| Action | When | Context |
|--------|------|---------|
| `contributeHead` | During document rendering | `HeadContributionContext`: mutable collector |

```java
.contributeHead(ctx -> {
    ctx.add(meta().attr("name", "viewport")
                  .attr("content", "width=device-width, initial-scale=1"));
    ctx.add(link().attr("rel", "icon").attr("href", "/favicon.ico"));
})
```

Supported nodes: `meta`, `link`, `style`, `script`, `base`, and any
custom tags. Insertion order is preserved.

### Observability traces

| Action | When | Context |
|--------|------|---------|
| `renderTrace` | After a render attempt completes, fails, or produces a safe route-not-found response | `RenderTrace`: safe route, method, status, duration, response size, adapter, request id, and error metadata |
| `eventTrace` | After a live event completes, fails, is rejected, or references a missing handler | `EventTrace`: safe route, opaque event id, method, status, duration, response size, adapter, request id, and handler outcome |

Trace actions are completion notifications. They are isolated from the user
request path: if a trace action throws, rendering or event handling continues.
Trace objects intentionally omit request bodies, cookies, authorization
headers, CSRF tokens, session secrets, browser state, submitted values, full
HTML, raw exception messages, and stack traces. See
[Observability](./observability.md) for the field and privacy policy.

## Error handling

If an action throws an exception:

1. The exception is caught and routed to `onError(...)`.
2. The original action or pipeline exception is re-thrown to the caller.
3. If `onError(...)` itself throws, the nested exception is printed to
   stderr and execution continues.
4. The runtime never enters infinite recursion.
5. Runtime integrity is preserved.

The original exception from the render or event pipeline is always
re-thrown to the caller after error actions execute.

## Context objects

All context objects are immutable (except `HeadContributionContext`
which is a mutable collector). They include a `traceId` (UUID) for
observability correlation and metadata groups for request, route,
event, session, CSS, client-state, and runtime-specific data. Metadata
maps are defensively copied when the context is created.

The `RuntimeErrorContext` does not expose sensitive server information
to clients automatically. Exception details are available for
server-side logging only.

## Enterprise usage patterns

### Observability

```java
.beforeRender(ctx -> MDC.put("traceId", ctx.traceId()))
.afterRender(result -> {
    metrics.record("render.duration", result.renderDuration());
    MDC.remove("traceId");
})
```

### Authorization

```java
.beforeEvent(ActionOrder.FIRST, ctx -> {
    if (!authorized(ctx.clientState())) {
        throw new SecurityException("Unauthorized");
    }
})
```

### SEO

```java
.contributeHead(ctx -> {
    ctx.add(meta().attr("name", "description")
                  .attr("content", "Server-rendered UJFE application"));
    ctx.add(meta().attr("name", "robots")
                  .attr("content", "index,follow"));
})
```

### Centralized error logging

```java
.onError(ctx -> {
    logger.error("UJFE error in {} phase: {}",
            ctx.phase(), ctx.exception().getMessage(), ctx.exception());
})
```

## Future direction

The runtime action system provides the foundation for:

- Micrometer metrics integration
- OpenTelemetry tracing integration
- Custom security middleware
- Request-scoped dependency injection
- Audit logging

These integrations can be implemented as `RuntimeActionRegistry`
configurations without modifying the core framework.

## Runtime rules

The extension system:

- Works without Spring.
- Works without Servlet APIs.
- Works without Netty APIs.
- Avoids global mutable state.
- Avoids static mutable registries.
- Avoids hidden implicit behavior.
- Is thread-safe.
- Is deterministic.
- Is enterprise-safe.
