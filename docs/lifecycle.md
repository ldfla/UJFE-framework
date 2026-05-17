# UJFE Lifecycle

UJFE lifecycle is a server-side runtime lifecycle for pages and components that
participate in live rendering. It exists to manage server resources that need a
clear lifetime: subscriptions, handles, observers, instrumentation scopes,
stateful services, and cleanup work tied to a live session.

It is not a browser lifecycle system. It does not run in JavaScript, does not
access the DOM, and is not modeled after React hooks or client-side effects.

## Quick Start

Implement `Lifecycle` on a page or component that needs mount and cleanup
callbacks:

```java
import ujfe.core.Component;
import ujfe.core.Lifecycle;
import ujfe.core.Node;
import ujfe.router.Page;

import static ujfe.html.UI.*;

@Page("/dashboard")
public final class DashboardPage implements Component, Lifecycle {
    private boolean subscriptionOpen;

    @Override
    public void onMount() {
        subscriptionOpen = true;
        // Open server-side resources here.
    }

    @Override
    public void onUnmount() {
        subscriptionOpen = false;
        // Release server-side resources here.
    }

    @Override
    public Node render() {
        return main()
                .child(h1("Dashboard"))
                .child(p(() -> subscriptionOpen ? "Mounted" : "Not mounted"));
    }
}
```

Nested lifecycle components should be rendered with `component(...)`:

```java
import ujfe.core.Component;
import ujfe.core.Lifecycle;
import ujfe.core.Node;

import static ujfe.html.UI.*;

public final class DashboardPage implements Component {
    private final MetricsPanel metricsPanel = new MetricsPanel();

    @Override
    public Node render() {
        return main()
                .child(h1("Dashboard"))
                .child(component(metricsPanel));
    }
}
```

`component(metricsPanel)` tells the renderer that the Java component instance is
part of the live render tree. If the component implements `Lifecycle`, UJFE
tracks it during reconciliation.

## Lifecycle Contract

The public contract is intentionally small:

```java
package ujfe.core;

public interface Lifecycle {
    default void onMount() {
    }

    default void onUnmount() {
    }
}
```

The methods are default no-ops. A page or component can implement only the
callback it needs.

## When `onMount()` Runs

`onMount()` runs when a lifecycle instance enters a live session render tree for
the first time.

It runs for:

- a page entering a live session;
- a component entering the rendered tree;
- a live component becoming active in a session;
- a component returning after it had previously left the tree.

It does not run for:

- every `render()` call;
- a stable live re-render of the same component instance;
- a signal update by itself;
- a browser event by itself, unless the event causes the component tree to
  change.

## When `onUnmount()` Runs

`onUnmount()` runs when a mounted lifecycle instance leaves the live render tree.

It runs for:

- route transitions that remove a page or component;
- conditional rendering that stops rendering a component;
- nested components that are no longer rendered;
- live session shutdown through `LiveSession.close()`;
- HTTP server shutdown through `UjfeServer.stop()`.

UJFE calls `onUnmount()` at most once for a mounted instance. Cleanup continues
for remaining components even if one unmount callback throws.

## Mental Model

A live render pass has three lifecycle phases:

```text
render current page/component tree
observe lifecycle instances during rendering
reconcile observed instances with previously mounted instances
```

During reconciliation:

- previously mounted instances that are no longer observed are unmounted;
- already mounted instances that are still observed remain mounted;
- newly observed lifecycle instances are mounted.

Lifecycle state is stored in the live session lifecycle registry. It is not a
global static registry.

## Stable Identity

Lifecycle identity is based on Java object identity.

The same object instance keeps its lifecycle state across re-renders. A new
object instance is treated as a different lifecycle participant.

Prefer this:

```java
public final class DashboardPage implements Component {
    private final MetricsPanel metricsPanel = new MetricsPanel();

    @Override
    public Node render() {
        return section()
                .child(component(metricsPanel));
    }
}
```

Avoid this when lifecycle stability matters:

```java
public Node render() {
    return section()
            .child(component(new MetricsPanel()));
}
```

The second example creates a fresh component object on every render. UJFE will
see the old instance leave the tree and the new instance enter it, so it will
unmount and mount again.

## Conditional Components

Conditional rendering controls component lifetime:

```java
import ujfe.core.Component;
import ujfe.core.Node;
import ujfe.html.Element;
import ujfe.signals.Signal;
import ujfe.signals.Signals;

import static ujfe.html.UI.*;

public final class SettingsPage implements Component {
    private final Signal<Boolean> showAdvanced = Signals.signal(false);
    private final AdvancedSettingsPanel advanced = new AdvancedSettingsPanel();

    @Override
    public Node render() {
        Element page = main()
                .child(button("Toggle advanced")
                        .onClick(() -> showAdvanced.update(value -> !value)));

        if (showAdvanced.get()) {
            page.child(component(advanced));
        }

        return page;
    }
}
```

When `showAdvanced` changes from `false` to `true`, `advanced.onMount()` runs.
When it changes back to `false`, `advanced.onUnmount()` runs.

## Page Lifecycle

Pages can implement `Lifecycle` directly:

```java
import ujfe.core.Component;
import ujfe.core.Lifecycle;
import ujfe.core.Node;
import ujfe.router.Page;

import static ujfe.html.UI.*;

@Page("/reports")
public final class ReportsPage implements Component, Lifecycle {
    @Override
    public void onMount() {
        // The page entered the live session.
    }

    @Override
    public void onUnmount() {
        // The page left the live session or the session closed.
    }

    @Override
    public Node render() {
        return main().child(h1("Reports"));
    }
}
```

`LiveSession` preserves the current route page instance across stable
re-renders. When the route changes, removed page/component instances are
unmounted and the new route instances are mounted.

## Component Lifecycle

Components can implement `Lifecycle` independently from pages:

```java
import ujfe.core.Component;
import ujfe.core.Lifecycle;
import ujfe.core.Node;

import static ujfe.html.UI.*;

public final class ResourcePanel implements Component, Lifecycle {
    private ServerResource resource;

    @Override
    public void onMount() {
        resource = ServerResource.open();
    }

    @Override
    public void onUnmount() {
        if (resource != null) {
            resource.close();
            resource = null;
        }
    }

    @Override
    public Node render() {
        return div().child("Resource-backed panel");
    }
}
```

Keep long-lived lifecycle components as fields on the parent page or component.
This gives UJFE stable object identity and prevents duplicate initialization.

## Route Transitions

Route transitions are lifecycle boundaries.

```text
render /dashboard
mount DashboardPage
mount nested Dashboard components

navigate to /settings
unmount removed Dashboard components
unmount DashboardPage
mount SettingsPage
mount nested Settings components
```

If the same Java component instance is still present in the new rendered tree,
it remains mounted. In normal page routing, each route owns its page instance,
so switching routes usually unmounts the previous page tree.

## Session Shutdown

`LiveSession.close()` unmounts all mounted lifecycle instances and clears session
state. `UjfeServer.stop()` closes the live session before stopping the HTTP
runtime.

Use session shutdown cleanup for resources that must not survive the user's live
session:

```java
try (LiveSession session = new LiveSession(router)) {
    session.renderPath("/");
}
```

When the session closes, mounted lifecycle components receive `onUnmount()`.

## Error Handling

Lifecycle failures are routed through runtime error handling.

If `onMount()` throws:

- the mount fails safely;
- lifecycle instances mounted earlier in the same reconciliation pass are rolled
  back;
- partial mount state is not left in the registry;
- the failure is routed to `onError(...)` runtime actions with
  `RuntimePhase.LIFECYCLE`.

If `onUnmount()` throws:

- UJFE removes the lifecycle instance from the registry;
- cleanup continues for the remaining mounted instances;
- the failure is routed to runtime `onError(...)` actions;
- session cleanup still completes.

Lifecycle callbacks should still be written defensively. Prefer idempotent
cleanup and null checks around external resources.

## Ordering

Lifecycle ordering is deterministic:

- components are detected in render order;
- duplicate references to the same instance are ignored within one render pass;
- new components mount in render order;
- removed components unmount in reverse mount order;
- session cleanup unmounts everything in reverse mount order.

This makes nested resource cleanup predictable: children that mounted after
their parent are cleaned up before the parent.

## Runtime Integration

Application code usually interacts only with `Lifecycle` and `component(...)`.
The runtime integration is handled by `LiveSession`.

Initial render:

```text
resolve route
render page
detect lifecycle instances
unmount removed instances
mount new instances
persist lifecycle state
```

Stable re-render:

```text
render current page instance
detect same lifecycle instances
skip duplicate mounts
preserve lifecycle state
```

Route transition:

```text
render new route
unmount removed route/page/component instances
mount new route/page/component instances
```

Session shutdown:

```text
LiveSession.close()
unmount all mounted lifecycle instances
clear event and lifecycle state
```

The lower-level runtime classes live in `ujfe.runtime.lifecycle`:

- `LifecycleRuntime`
- `LifecycleRegistry`
- `LifecycleTracker`
- `MountedComponent`
- `LifecycleState`
- `LifecycleContext`
- `MountContext`
- `UnmountContext`
- `MountEvent`
- `UnmountEvent`

They are runtime extension points for UJFE internals and advanced integrations.
They do not depend on Spring, Servlet, or Netty APIs.

## Testing Lifecycle Code

For component-level tests, assert callback counts:

```java
LifecycleRuntime runtime = LifecycleRuntime.create();
LifecycleTracker tracker = runtime.beginRender(
        new LifecycleContext("/", null, "trace", Map.of())
);

tracker.track(component);
tracker.complete();

assertTrue(runtime.isMounted(component));
```

For live runtime tests, render through `LiveSession` and trigger route changes or
session close:

```java
LiveSession session = new LiveSession(router);

session.renderPath("/dashboard");
session.renderPath("/dashboard");
session.renderPath("/settings");
session.close();
```

Expected behavior:

- the first render mounts `/dashboard`;
- the second render preserves stable `/dashboard` instances;
- the route change unmounts removed `/dashboard` instances and mounts
  `/settings`;
- `close()` unmounts remaining lifecycle instances.

## Recommended Uses

Use lifecycle callbacks for:

- opening and closing server-side subscriptions;
- registering and unregistering observers;
- starting and stopping request-independent resources scoped to a live session;
- tracking page or component lifetime for audit, metrics, or tracing;
- connecting stateful server-side components to runtime resources;
- cleanup that must happen on route transition or session shutdown.

Avoid lifecycle callbacks for:

- browser effects;
- client-side DOM access;
- JavaScript lifecycle behavior;
- dependency injection;
- long-running distributed orchestration;
- business operations that should happen on explicit user commands;
- work that should run on every render.

## Example Application

The examples application includes a lifecycle page at `/lifecycle`.

Run it with:

```bash
./mvnw -pl examples -am exec:java -Dexec.mainClass=app.Main
```

Open:

```text
http://localhost:8080/lifecycle
```

The example demonstrates:

- page mount and unmount callbacks;
- nested lifecycle components through `component(...)`;
- stable live re-renders;
- route transition cleanup;
- resource cleanup patterns;
- lifecycle-safe signal usage.
