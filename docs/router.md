# UJFE Router

UJFE routing maps server-side Java pages to HTTP paths. The router model is
designed to support both simple development-time discovery and enterprise
deployments that minimize runtime reflection.

UJFE supports two route discovery models:

- reflection-based discovery through `ReflectionPageScanner`;
- explicit registration through `ManualRouteSource`.

Both models produce `RouteDefinition` objects and feed the same `Router`.

## Core Types

```java
public interface RouteSource {
    Collection<RouteDefinition> routes();
}
```

```java
Router router = new Router()
        .register(routeSource);
```

Important router classes:

- `Router`: deterministic route registry and resolver;
- `RouteDefinition`: path plus page factory metadata;
- `PageRenderer`: renders a route page through `Component` or `render()`;
- `ManualRouteSource`: explicit source for AOT-friendly registration;
- `ReflectionPageScanner`: reflection-based scanner for annotated pages.

## Route Definitions

A route definition contains:

- normalized route path;
- page factory;
- optional page class metadata;
- source description used in deterministic error messages.

```java
RouteDefinition route = new RouteDefinition("/", HomePage::new);
```

Routes preserve registration order. `Router.routes()` returns a stable snapshot
in the same order routes were registered.

## Route Path Rules

Route paths are normalized and validated before registration.

Rules:

- blank paths are rejected;
- paths without a leading `/` are normalized by adding `/`;
- whitespace is rejected;
- query and fragment markers are rejected;
- `<`, `>`, single quotes, and double quotes are rejected.

Examples:

```java
new RouteDefinition("dashboard", DashboardPage::new); // becomes /dashboard
new RouteDefinition("/dashboard", DashboardPage::new);
```

Invalid:

```java
new RouteDefinition("", HomePage::new);
new RouteDefinition("/bad path", HomePage::new);
new RouteDefinition("/bad?query", HomePage::new);
new RouteDefinition("/bad#fragment", HomePage::new);
new RouteDefinition("/<bad>", HomePage::new);
```

Invalid paths fail immediately with `IllegalArgumentException`.

## Manual Route Registration

Manual route registration is the preferred model for:

- enterprise applications;
- AOT deployments;
- GraalVM native images;
- Spring Boot applications that want explicit wiring;
- faster startup;
- reduced runtime reflection.

```java
ManualRouteSource routes = new ManualRouteSource()
        .register("/", HomePage::new)
        .register("/dashboard", DashboardPage::new)
        .register("/settings", SettingsPage::new);

Router router = new Router()
        .register(routes);
```

Manual route sources do not require page classes to be annotated with `@Page`.
The path and factory are explicit metadata. This is the same shape future
generated AOT metadata will use.

Multiple route sources can be merged:

```java
Router router = new Router()
        .register(publicRoutes, adminRoutes);
```

Duplicate paths fail explicitly when a source is built or when sources are
merged into the router.

## Reflection-Based Discovery

Reflection scanning is useful for development and smaller applications.

```java
ReflectionPageScanner scanner = ReflectionPageScanner.forPackages("app.pages");

Router router = new Router()
        .register(scanner);
```

You can also provide explicit candidate classes:

```java
ReflectionPageScanner scanner = new ReflectionPageScanner(
        HomePage.class,
        DashboardPage.class
);
```

The scanner:

- discovers classes annotated with `@Page`;
- ignores classes without `@Page`;
- ignores interfaces safely;
- rejects abstract page classes;
- rejects invalid render methods;
- rejects classes that cannot be instantiated through a no-argument constructor;
- detects duplicate route paths;
- sorts discovered classes by class name for deterministic output.

The scanner loads classes without initializing them. It scans file and jar
classpath resources for the requested package names.

## Valid Page Classes

A reflection-discovered page class must:

- be annotated with `@Page`;
- not be an interface;
- not be abstract;
- be instantiable through a no-argument constructor;
- either implement `ujfe.core.Component` or expose a public zero-argument
  `render()` method;
- return `ujfe.core.Node` from `render()` when not implementing `Component`.

Accepted:

```java
@Page("/")
public final class HomePage implements Component {
    public Node render() {
        return main().child(h1("Home"));
    }
}
```

Also accepted:

```java
@Page("/about")
public final class AboutPage {
    public Node render() {
        return main().child(h1("About"));
    }
}
```

Invalid:

```java
@Page("/bad")
public final class BadPage {
    public String render() {
        return "bad";
    }
}
```

Invalid discovered pages fail with `RouteDiscoveryException`.

## Duplicate Routes

Duplicate routes are never ignored silently.

```java
@Page("/")
public final class HomePage {
}

@Page("/")
public final class AlternateHomePage {
}
```

This fails with an error that identifies the duplicated path and the conflicting
classes.

Manual duplicates also fail:

```java
new ManualRouteSource()
        .register("/", HomePage::new)
        .register("/", AlternateHomePage::new);
```

## Rendering Rules

`PageRenderer` supports:

- `Component` pages;
- pages with public `render()` returning `ujfe.core.Node`.

```java
Node node = new PageRenderer().render(route);
```

`PageRenderer` still validates at render time as a defensive runtime boundary,
but route discovery validates earlier whenever possible.

## Deterministic Ordering

Ordering rules:

- manual registration order is preserved;
- multiple route sources are merged in the order passed to `Router.register(...)`;
- reflection-discovered pages are sorted by fully qualified class name;
- duplicate paths fail immediately instead of depending on last-write-wins
  behavior.

This gives predictable startup and predictable route metadata in tests,
generated artifacts, and enterprise deployments.

## Compatibility Notes

Existing registration styles remain supported:

```java
new Router().register(new HomePage());
new Router().register(HomePage.class);
new Router().register("/", HomePage::new);
```

Registering a page instance can use constructor arguments because the instance
already exists:

```java
new Router().register(new DocumentationPage(theme));
```

Registering a page class or scanning by reflection requires a no-argument
constructor.

## Runtime Independence

The router module does not depend on Spring, Servlet APIs, or Netty. It provides
route definitions and page factories that can be consumed by:

- pure UJFE runtime;
- Netty runtime;
- Servlet-based integrations;
- Spring adapter runtime;
- future generated metadata.

No global mutable route registry is used.
