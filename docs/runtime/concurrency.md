# Runtime Concurrency

UJFE live sessions use explicit per-session concurrency control. The runtime does not use a global mutable session lock, so independent live sessions can process internal requests without waiting on each other.

## Locking Model

Each `LiveSession` owns its own `ReentrantReadWriteLock`.

Write-locked operations:

- page rendering through `renderPath(...)`.
- full document rendering through `renderDocument(...)`.
- live event dispatch through `handleEvent(...)`.
- client state synchronization through `updateClientState(...)`.
- lifecycle cleanup through `close()`.

Read-locked operations:

- CSS rendering through `renderCss(...)`.

This keeps all mutable session state coherent:

- current route path.
- current page instance.
- client state snapshot.
- event handler registry.
- lifecycle registry.
- generated HTML and CSS for a render pass.

## Why Not `synchronized`

Earlier versions guarded the main `LiveSession` entry points with Java's intrinsic `synchronized` monitor. That was correct for consistency, but it hid the concurrency policy inside method modifiers and made it harder to reason about read-only work, route transitions, and future runtime integration.

The explicit lock makes the policy visible and testable:

- no static lock is used.
- no global registry serializes independent sessions.
- same-session mutations remain serialized.
- read-only CSS rendering can use a read lock.

## Why Not `StampedLock`

`StampedLock` was evaluated but not used for the live session boundary.

The live runtime can execute user callbacks, lifecycle callbacks, and runtime actions during rendering and event handling. Those callbacks may re-enter session APIs in advanced integrations. `ReentrantReadWriteLock` is reentrant and fits that model. `StampedLock` is not reentrant, so it would make callback-driven re-entry easier to deadlock or misuse.

Correctness is prioritized over lock-free optimization.

## Event Consistency

Live event handlers are scoped to the current render route and registration position. During a render pass, UJFE reconciles the active event handlers instead of dropping the whole registry at the start of rendering.

This matters when multiple browser events are sent from the same rendered page before the browser receives the first re-render:

```text
render page
click A and click B are sent concurrently
event A runs and re-renders
event B still resolves to the matching handler position
```

Removed event positions are cleaned up after a successful render. If rendering fails, the previous event registry remains available so the session does not lose the last coherent state.

## Same Session

Concurrent mutations inside the same session are serialized by the session write lock.

This guarantees:

- no duplicate render mutation at the same time.
- no lifecycle reconciliation overlap.
- no lost signal updates caused by concurrent event dispatch.
- coherent final HTML after contention.

Application state should still use thread-safe primitives or UJFE signals when shared outside one session.

## Different Sessions

Different `LiveSession` instances have different locks. A long-running event in one session does not block event handling in another session.

This is the intended runtime boundary:

```text
session A event -> session A write lock
session B event -> session B write lock
```

No Spring, Servlet, or Netty runtime dependency is required for this behavior.

## Tests

The runtime test suite covers:

- concurrent events in the same session.
- concurrent events in different sessions.
- no lost signal updates under same-session contention.
- coherent HTML output after concurrent live events.
- latch-based overlap checks so independent sessions are verified without relying on fragile timing benchmarks.
