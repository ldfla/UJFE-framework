# Router Source Examples

UJFE supports reflection-based route discovery and explicit manual route
sources.

## Manual Route Source

Manual route sources are preferred for AOT-friendly applications:

```java
ManualRouteSource routes = new ManualRouteSource()
        .register("/", HomePage::new)
        .register("/dashboard", DashboardPage::new)
        .register("/settings", SettingsPage::new);

Router router = new Router()
        .register(routes);
```

Manual routes preserve registration order and avoid runtime package scanning.

## Reflection Scanner

Reflection scanning remains available for development:

```java
Router router = new Router()
        .register(ReflectionPageScanner.forPackages("app.pages"));
```

The scanner sorts discovered classes deterministically by class name, validates
annotated page classes, ignores non-page classes, and fails on duplicate routes.

## Multiple Sources

Route sources can be merged explicitly:

```java
Router router = new Router()
        .register(publicRoutes, adminRoutes);
```

If two sources define the same path, startup fails with a deterministic duplicate
route error.

## Current Example App

The runnable examples application wires routes through `ManualRouteSource` in
`app.Main`, including:

- `/`
- `/docs`
- `/signals`
- `/lifecycle`
- `/runtime-actions`

This mirrors the AOT metadata shape planned for future generated route sources.
