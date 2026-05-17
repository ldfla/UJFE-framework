# UJFE AOT Route Metadata Roadmap

UJFE routing now has an explicit route source model. That model is the
foundation for future compile-time route metadata generation and
reflection-minimized startup.

The current AOT direction is:

```text
@Page annotations
annotation processor
generated route metadata
ManualRouteSource
Router
reflection-free runtime startup
```

This issue does not implement the annotation processor. It defines the runtime
shape that future generated metadata will target.

## Why AOT Route Metadata

Reflection-based route scanning is convenient during development, but enterprise
and native-image deployments often need stronger startup guarantees:

- fewer runtime classpath scans;
- less reflection configuration;
- faster startup;
- deterministic metadata;
- GraalVM compatibility;
- Spring Boot compatibility without automatic runtime discovery;
- explicit deployment behavior.

## Runtime Shape

Generated metadata should produce or feed a `RouteSource`:

```java
public final class GeneratedRoutes implements RouteSource {
    public Collection<RouteDefinition> routes() {
        return List.of(
                new RouteDefinition("/", HomePage::new),
                new RouteDefinition("/dashboard", DashboardPage::new)
        );
    }
}
```

Applications can then wire routes without package scanning:

```java
Router router = new Router()
        .register(new GeneratedRoutes());
```

`ManualRouteSource` already models this future shape:

```java
ManualRouteSource routes = new ManualRouteSource()
        .register("/", HomePage::new)
        .register("/dashboard", DashboardPage::new);
```

## Annotation Processor Direction

A future annotation processor should:

- inspect classes annotated with `@Page`;
- validate route paths at compile time where possible;
- validate render method shape where possible;
- detect duplicate routes at compile time;
- emit deterministic generated source;
- avoid classpath scanning at runtime;
- produce metadata compatible with `RouteSource`.

Generated code should preserve route ordering deterministically. A recommended
ordering is fully qualified class name order, matching `ReflectionPageScanner`.

## GraalVM Direction

The runtime route model is designed to reduce reflection requirements:

- manual/generated sources use explicit Java factories;
- no global mutable registry is required;
- `Router` consumes route definitions directly;
- `PageRenderer` can render `Component` instances without reflective method
  invocation.

For fully reflection-minimized native images, prefer pages that implement
`Component` and generated route metadata that calls constructors directly.

## Spring Boot Direction

Spring applications can wire route sources explicitly:

```java
@Bean
RouteSource ujfeRoutes(HomePage homePage, DashboardPage dashboardPage) {
    return new ManualRouteSource()
            .register("/", () -> homePage)
            .register("/dashboard", () -> dashboardPage);
}

@Bean
Router ujfeRouter(RouteSource routes) {
    return new Router().register(routes);
}
```

This avoids runtime package scanning and gives Spring ownership of dependency
injection while UJFE owns route resolution.

## Compatibility

Reflection scanning remains available:

```java
new Router().register(ReflectionPageScanner.forPackages("app.pages"));
```

Use reflection scanning for development convenience. Use manual or generated
route sources for production environments that need predictable startup and
native-image friendliness.

## Out Of Scope

Future work may add:

- annotation processor module;
- generated route source naming conventions;
- build plugin integration;
- GraalVM reflection config generation when needed;
- compile-time diagnostics.

This roadmap only defines the runtime metadata target.
