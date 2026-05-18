# Standalone Jakarta Servlet Runtime

`ujfe-servlet` runs UJFE in a plain Jakarta Servlet container without Spring Boot, Spring MVC, or Netty.

Use this runtime when an application already deploys as a WAR, when an enterprise platform standardizes on a servlet container, or when the team wants UJFE live rendering without adopting Spring Boot.

## What This Runtime Provides

- `UjfeServlet`, a normal `jakarta.servlet.http.HttpServlet`.
- Server-rendered UJFE pages.
- Live browser event dispatch through `/_ujfe/event`.
- Browser client state synchronization through `/_ujfe/state`.
- Client script delivery through `/_ujfe/client.js`.
- Runtime CSS delivery through `/_ujfe/css`.
- Optional dev preview script through `/_ujfe/dev.js`.
- Shared JSON and state parsing through `LiveHttpCodec`.
- Shared security headers with the Netty and Spring adapters.
- Explicit route ownership so unrelated routes are not captured accidentally.

The servlet runtime does not introduce a Spring, Netty, or container-specific programming model. Application pages, components, signals, lifecycle callbacks, runtime actions, and route sources stay the same Java APIs used by the rest of UJFE.

## Compatibility

This module uses `jakarta.servlet.*`.

Use a Jakarta Servlet container such as Tomcat 10+ or Tomcat 11. Tomcat 9.x is still on the legacy Java EE servlet namespace. Tomcat 9.0.118 can be the latest Tomcat 9 release line, but it is not the matching runtime for this module.

If a deployment must stay on Tomcat 9, it needs either:

- a separate Java EE servlet adapter, or
- a migration/conversion step between Java EE and Jakarta EE artifacts.

UJFE keeps `ujfe-servlet` Jakarta-only to avoid mixing incompatible servlet namespaces inside one runtime artifact.

## Quick Start

Add `ujfe-servlet` to a WAR project:

```xml
<dependency>
    <groupId>dev.ujfe</groupId>
    <artifactId>ujfe-servlet</artifactId>
    <version>0.12.0-SNAPSHOT</version>
</dependency>
```

The servlet API must be provided by the container:

```xml
<dependency>
    <groupId>jakarta.servlet</groupId>
    <artifactId>jakarta.servlet-api</artifactId>
    <scope>provided</scope>
</dependency>
```

Create a page:

```java
package app.pages;

import ujfe.core.Node;
import ujfe.router.Page;
import ujfe.signals.Signal;
import ujfe.signals.Signals;

import static ujfe.core.UI.button;
import static ujfe.core.UI.h1;
import static ujfe.core.UI.main;
import static ujfe.core.UI.p;

@Page("/home")
public final class HomePage {
    private final Signal<Integer> clicks = Signals.signal(0);

    public Node render() {
        return main()
                .css("min-h-screen p-8 flex flex-col gap-4")
                .child(h1("UJFE Servlet"))
                .child(p("Rendered by a plain Jakarta Servlet runtime."))
                .child(p(() -> "Clicks: " + clicks.get()))
                .child(button("Increment").onClick(() -> clicks.update(value -> value + 1)));
    }
}
```

Register the servlet programmatically:

```java
package app;

import app.pages.HomePage;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.annotation.WebListener;
import ujfe.live.LiveSessionConfig;
import ujfe.router.Router;
import ujfe.router.source.ManualRouteSource;
import ujfe.servlet.UjfeServlet;

@WebListener
public final class UjfeBootstrap implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent event) {
        var routes = new ManualRouteSource()
                .register("/home", HomePage::new);

        var router = new Router().register(routes);
        var config = LiveSessionConfig.builder()
                .title("UJFE Servlet App")
                .devToolsEnabled(true)
                .build();

        ServletContext context = event.getServletContext();
        ServletRegistration.Dynamic servlet = context.addServlet("ujfe", new UjfeServlet(router, config));
        servlet.setLoadOnStartup(1);
        servlet.addMapping("/home", "/_ujfe/*");
    }
}
```

Deploy the WAR to a Jakarta Servlet container and open:

```text
http://localhost:8080/<context-path>/home
```

## Runtime Flow

For an initial page request:

```text
GET /home
  -> UjfeServlet checks that /home is a registered route
  -> LiveSession renders the page
  -> UJFE writes the HTML document
  -> the document loads /_ujfe/client.js
```

For a live event:

```text
click button
  -> client.js sends POST /_ujfe/event
  -> LiveHttpCodec extracts eventId and clientState
  -> LiveSession runs the Java event handler
  -> LiveSession re-renders the current route
  -> response JSON contains updated html and css
  -> client.js replaces #ujfe-root
```

For browser state synchronization:

```text
DOMContentLoaded
  -> client.js sends POST /_ujfe/state
  -> policy-allowed cookies, localStorage, and sessionStorage become available through Ujfe
  -> LiveSession re-renders the current route with the filtered ClientState
```

## Choosing A Registration Model

| Model | Best for | Reflection | Route ownership |
| --- | --- | --- | --- |
| Programmatic `ManualRouteSource` | Production, AOT-friendly deployments, enterprise startup predictability | None for route discovery | Explicit servlet mappings |
| `ServletContext` attributes | Applications that already build a `Router` elsewhere | Depends on your router source | Explicit servlet mappings |
| `web.xml` plus `ujfe.routes.packages` | Simple WARs and development | Package scanning | Descriptor mappings |
| Mapping the servlet to `/` | UJFE-only WARs | Depends on configuration | Broad; avoid in mixed apps |

Prefer `ManualRouteSource` for production. It is deterministic, reflection-minimized, and aligns with the AOT route metadata direction.

## Programmatic Registration

Programmatic registration is the safest option because the application controls both:

- the registered UJFE routes, and
- the servlet URL patterns that reach `UjfeServlet`.

Example with multiple pages:

```java
var routes = new ManualRouteSource()
        .register("/home", HomePage::new)
        .register("/dashboard", DashboardPage::new)
        .register("/settings", SettingsPage::new);

var router = new Router().register(routes);
var servlet = context.addServlet("ujfe", new UjfeServlet(router));

servlet.setLoadOnStartup(1);
servlet.addMapping(
        "/home",
        "/dashboard",
        "/settings",
        "/_ujfe/*"
);
```

This mapping does not capture:

- `/assets/app.css`
- `/api/users`
- `/health`
- `/admin`
- any other route not explicitly mapped to the servlet

If the application has many UJFE pages, mapping all exact paths can be verbose. A common compromise is to put UJFE under a path prefix:

```java
var routes = new ManualRouteSource()
        .register("/ui/home", HomePage::new)
        .register("/ui/dashboard", DashboardPage::new);

ServletRegistration.Dynamic servlet = context.addServlet("ujfe", new UjfeServlet(new Router().register(routes)));
servlet.addMapping("/ui/*", "/_ujfe/*");
```

In that model, only `/ui/*` is owned by UJFE.

## Descriptor Registration

For traditional WAR deployments, `web.xml` can instantiate the servlet and configure package scanning.

```xml
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_6_1.xsd"
         version="6.1">

    <servlet>
        <servlet-name>ujfe</servlet-name>
        <servlet-class>ujfe.servlet.UjfeServlet</servlet-class>
        <load-on-startup>1</load-on-startup>

        <init-param>
            <param-name>ujfe.routes.packages</param-name>
            <param-value>app.pages</param-value>
        </init-param>
        <init-param>
            <param-name>ujfe.live.title</param-name>
            <param-value>UJFE Servlet App</param-value>
        </init-param>
        <init-param>
            <param-name>ujfe.live.dev-tools-enabled</param-name>
            <param-value>true</param-value>
        </init-param>
    </servlet>

    <servlet-mapping>
        <servlet-name>ujfe</servlet-name>
        <url-pattern>/home</url-pattern>
    </servlet-mapping>
    <servlet-mapping>
        <servlet-name>ujfe</servlet-name>
        <url-pattern>/dashboard</url-pattern>
    </servlet-mapping>
    <servlet-mapping>
        <servlet-name>ujfe</servlet-name>
        <url-pattern>/_ujfe/*</url-pattern>
    </servlet-mapping>
</web-app>
```

`ujfe.routes.packages` tells `UjfeServlet` to create a `Router` using `ReflectionPageScanner.forPackages(...)`.

Package scanning is convenient, but it is not the preferred production model when startup predictability, AOT compilation, or minimized reflection matters.

## Application Properties

`UjfeServlet` reads configuration from:

1. `application.properties`
2. `application.yml`
3. `application.yaml`
4. servlet context parameters
5. servlet init parameters

Later sources override earlier sources.

Example `application.properties`:

```properties
ujfe.routes.packages=app.pages,app.admin
ujfe.live.title=UJFE Servlet App
ujfe.live.lang=en
ujfe.live.dev-tools-enabled=true
ujfe.live.css-mode=internal
ujfe.live.max-json-payload-bytes=262144
ujfe.client-state.cookies=ujfe_demo
ujfe.client-state.local-storage-keys=ujfe.theme
ujfe.client-state.session-storage-keys=ujfe.tab
```

Equivalent `application.yml`:

```yaml
ujfe:
  routes:
    packages: app.pages,app.admin
  live:
    title: UJFE Servlet App
    lang: en
    dev-tools-enabled: true
    css-mode: internal
    max-json-payload-bytes: 262144
  client-state:
    cookies: ujfe_demo
    local-storage-keys: ujfe.theme
    session-storage-keys: ujfe.tab
```

Supported keys:

| Key | Example | Meaning |
| --- | --- | --- |
| `ujfe.routes.packages` | `app.pages,app.admin` | Comma-separated packages scanned for `@Page` classes |
| `ujfe.live.title` | `UJFE Servlet App` | Document `<title>` |
| `ujfe.live.lang` | `en` | Document `<html lang="...">` |
| `ujfe.live.dev-tools-enabled` | `true` | Whether `/_ujfe/dev.js` is added to rendered documents |
| `ujfe.live.css-mode` | `internal` | CSS delivery mode: `internal`, `external`, or `none` |
| `ujfe.live.max-json-payload-bytes` | `262144` | Maximum accepted JSON body size for `/_ujfe/event` and `/_ujfe/state`; defaults to `1048576` |
| `ujfe.client-state.cookies` | `ujfe_demo` | Comma-separated cookies exposed through `Ujfe.cookie(...)` |
| `ujfe.client-state.local-storage-keys` | `ujfe.theme` | Comma-separated `localStorage` keys exposed through `Ujfe.localStorage(...)` |
| `ujfe.client-state.session-storage-keys` | `ujfe.tab` | Comma-separated `sessionStorage` keys exposed through `Ujfe.sessionStorage(...)` |

## ServletContext Attributes

Applications that already construct UJFE runtime objects can provide them through attributes:

```java
context.setAttribute(UjfeServlet.ROUTER_ATTRIBUTE, router);
```

With only a router attribute, `UjfeServlet` creates a `LiveSession` using configuration from the servlet settings.

If the application also wants to own `LiveSession`, provide both:

```java
context.setAttribute(UjfeServlet.ROUTER_ATTRIBUTE, router);
context.setAttribute(UjfeServlet.LIVE_SESSION_ATTRIBUTE, liveSession);
```

The router is mandatory when a prebuilt `LiveSession` is provided because `UjfeServlet.handles(method, path)` must be able to answer whether a request belongs to UJFE.

## LiveSession Configuration

Programmatic registration can pass a `LiveSessionConfig`:

```java
var config = LiveSessionConfig.builder()
        .title("Enterprise Portal")
        .lang("en")
        .devToolsEnabled(false)
        .cssMode(CssMode.EXTERNAL)
        .externalStylesheet("/assets/app.css")
        .runtimeActions(runtimeActions)
        .build();

var servlet = new UjfeServlet(router, config);
```

That keeps Servlet concerns separate from UJFE runtime concerns:

- Servlet mapping decides which requests enter UJFE.
- `Router` decides which UJFE route is rendered.
- `LiveSessionConfig` decides document metadata, CSS strategy, dev tools, head nodes, and runtime extension points.

The live JSON payload limit is intentionally configured on the Servlet adapter, not on `LiveSessionConfig`, because it is an HTTP request boundary concern:

```java
var servlet = new UjfeServlet(router, config, 262_144);
```

## Internal Endpoints

UJFE internal endpoints must be mapped to the servlet, usually with `/_ujfe/*`.

| Path | Method | Content type | Purpose |
| --- | --- | --- | --- |
| `/_ujfe/client.js` | `GET` | `application/javascript; charset=utf-8` | Browser event bridge |
| `/_ujfe/dev.js` | `GET` | `application/javascript; charset=utf-8` | Optional dev preview script |
| `/_ujfe/css` | `GET` | `text/css; charset=utf-8` | Runtime CSS for requested utility classes |
| `/_ujfe/event` | `POST` | `application/json; charset=utf-8` | Live event dispatch |
| `/_ujfe/state` | `POST` | `application/json; charset=utf-8` | Policy-allowed client state synchronization |

The live payload contract is shared with the other runtimes:

```json
{
  "eventId": "opaque-event-id",
  "clientState": {
    "cookies": "theme=dark; user=42",
    "localStorage": {
      "ujfe.theme": "dark"
    },
    "sessionStorage": {
      "ujfe.tab": "docs"
    }
  }
}
```

Responses from `/_ujfe/event` and `/_ujfe/state` use:

```json
{
  "html": "<main>...</main>",
  "css": ".generated-css{...}"
}
```

Application code should not construct these endpoint payloads manually during normal use. They are shown here to make the runtime behavior clear and testable.

Live JSON parsing, response serialization, payload size validation, and safe error messages are delegated to the shared `LiveHttpCodec`. See [Live events](../live-events.md) for the cross-runtime contract used by Servlet, Spring, and Netty.

## Route Claiming

`UjfeServlet` claims a request only when:

- the path is one of the UJFE internal endpoints, or
- the method is `GET` and the path resolves to a registered UJFE route.

Unknown paths return `404` instead of rendering an arbitrary page. Non-GET requests to known page routes return `405`.

If an asset-like request reaches `UjfeServlet` because the servlet is mapped broadly, the servlet returns a normal text `404` static asset response instead of treating it as a missing UJFE page route.

Applications can inspect this behavior directly:

```java
boolean page = servlet.handles("GET", "/home");              // true
boolean event = servlet.handles("POST", "/_ujfe/event");     // true
boolean asset = servlet.handles("GET", "/assets/app.css");   // false
boolean postPage = servlet.handles("POST", "/home");         // false
```

This method is useful in tests, custom dispatchers, or servlet containers where an application wants a routing decision before calling `service(...)`.

## Static Assets And Existing Routes

In a mixed WAR, avoid this unless UJFE owns the full application:

```java
servlet.addMapping("/");
```

Prefer exact mappings:

```java
servlet.addMapping("/home", "/dashboard", "/_ujfe/*");
```

Or a dedicated prefix:

```java
servlet.addMapping("/ui/*", "/_ujfe/*");
```

Then put static assets and non-UJFE endpoints outside that prefix:

```text
/assets/app.css       -> static resource servlet
/api/users            -> existing REST servlet
/ui/dashboard         -> UJFE
/_ujfe/event          -> UJFE internal endpoint
```

## Security Behavior

`UjfeServlet` applies the same default HTTP security headers as the other UJFE adapters:

- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Permissions-Policy: geolocation=(), microphone=(), camera=()`
- `Content-Security-Policy` with `default-src 'self'` and same-origin `script-src`

The default CSP is compatible with the external `/_ujfe/client.js` runtime script and does not require inline application JavaScript. If a servlet deployment already owns headers at a reverse proxy, container filter, or gateway layer, disable UJFE-managed headers explicitly:

```properties
ujfe.security.headers.enabled=false
```

Or override individual values:

```properties
ujfe.security.headers.referrer-policy=same-origin
ujfe.security.headers.content-security-policy=default-src 'self'; script-src 'self'
ujfe.security.headers.x-frame-options=SAMEORIGIN
```

See [Secure HTTP headers](../security/headers.md) for the shared policy, Java configuration, Spring behavior, and CSP notes.

Safe-by-default HTML rendering comes from the HTML APIs in `ujfe-core`:

- text content is escaped by default.
- attribute values are escaped.
- dangerous attribute names are rejected.
- URL-bearing attributes are sanitized by `SafeUrl`.
- raw HTML requires an explicit unsafe API.

UJFE also applies its runtime CSRF protection and internal endpoint rate limiting to Servlet requests. In a servlet deployment, still put standard enterprise filters in front of `UjfeServlet` when the application needs authentication, authorization, audit logging, request correlation, or infrastructure-level rate limits.

Example filter mapping:

```xml
<filter-mapping>
    <filter-name>security-filter</filter-name>
    <url-pattern>/_ujfe/*</url-pattern>
</filter-mapping>
```

## Error Handling

The servlet delegates error rendering to the same `ErrorResponseRenderer` used by the Netty and Spring adapters. Error bodies are JSON with stable UJFE error codes and generic production-safe messages. Raw exception messages, stack traces, implementation class names, request bodies, cookies, authorization headers, and CSRF tokens are not returned to the client.

| Condition | HTTP status | Error code |
| --- | --- | --- |
| invalid live payload | `400 Bad Request` | `UJFE_BAD_REQUEST` |
| unknown route | `404 Not Found` | `UJFE_ROUTE_NOT_FOUND` |
| missing static asset | `404 Not Found` | plain text static asset response |
| unsafe static asset path | `400 Bad Request` | plain text static asset response |
| wrong method | `405 Method Not Allowed` | `UJFE_INVALID_REQUEST` |
| CSRF validation failure | `403 Forbidden` | `UJFE_CSRF_VALIDATION_FAILED` |
| rate limit exceeded | `429 Too Many Requests` | `UJFE_RATE_LIMITED` |
| unexpected runtime failure | `500 Internal Server Error` | `UJFE_INTERNAL_ERROR`, `UJFE_RENDER_ERROR`, `UJFE_EVENT_HANDLER_ERROR`, or `UJFE_STATE_ERROR` |

Runtime lifecycle errors and runtime action errors continue to flow through `LiveSession` and the runtime action registry. The servlet is only responsible for converting request/response boundaries into HTTP responses. See [Safe error responses](../security/error-handling.md) for the shared policy and development diagnostics switch.

## Testing A Servlet Deployment

A minimal smoke test should verify:

- `GET /home` returns HTML and includes `/_ujfe/client.js`.
- `GET /_ujfe/client.js` returns JavaScript.
- `GET /_ujfe/css?classes=p-4` returns CSS.
- `POST /_ujfe/state` returns JSON with `html` and `css`.
- `POST /_ujfe/event` dispatches an event id captured from the rendered page.
- `GET /assets/app.css` is not claimed by UJFE when assets are outside the UJFE mapping.
- `GET /poster.png`, `/demo.mp4`, or `/audio.mp3` does not trigger UJFE page rendering when it reaches the servlet.

The module test suite uses servlet mocks for those behaviors. Embedded container tests can be added later if the project decides to include a Tomcat test dependency.

## Troubleshooting

### `ClassNotFoundException: jakarta.servlet...`

The application is probably running on a Java EE servlet container or the Jakarta Servlet API is not available. Use a Jakarta Servlet container such as Tomcat 10+ / 11 and keep `jakarta.servlet-api` as `provided`.

### `ClassNotFoundException: javax.servlet...`

The deployment likely mixed Java EE and Jakarta EE artifacts. `ujfe-servlet` is Jakarta-only. Do not deploy it to a Tomcat 9 application expecting the legacy servlet namespace.

### `No UJFE router configured`

The servlet was constructed by the container, but no router was supplied and no route package was configured.

Fix it with one of these approaches:

```java
context.setAttribute(UjfeServlet.ROUTER_ATTRIBUTE, router);
```

or:

```properties
ujfe.routes.packages=app.pages
```

or:

```xml
<init-param>
    <param-name>ujfe.routes.packages</param-name>
    <param-value>app.pages</param-value>
</init-param>
```

### The page renders, but clicks do nothing

Check that `/_ujfe/*` is mapped to `UjfeServlet`. The initial page can render without it, but live events require the internal endpoints.

### Static files return UJFE 404 responses

The servlet is probably mapped too broadly. Use exact page mappings or a dedicated prefix instead of mapping UJFE to `/`.

### Dev preview script does not load

Enable dev tools in `LiveSessionConfig` or configuration:

```properties
ujfe.live.dev-tools-enabled=true
```

The rendered document must include:

```html
<script src="/_ujfe/dev.js"></script>
```

The endpoint itself must also be mapped through `/_ujfe/*`.

## Complete Minimal File Set

A small WAR usually needs:

```text
pom.xml
src/main/java/app/pages/HomePage.java
src/main/java/app/UjfeBootstrap.java
```

For descriptor-based scanning:

```text
pom.xml
src/main/java/app/pages/HomePage.java
src/main/webapp/WEB-INF/web.xml
src/main/resources/application.properties
```

For production and AOT-friendly deployments, prefer the first model with `ManualRouteSource`.
