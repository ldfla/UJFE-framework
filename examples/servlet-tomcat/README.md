# UJFE Servlet/Tomcat Example

This example shows how to run UJFE in a plain Jakarta Servlet container without Spring Boot.

Use a Jakarta Servlet container such as Tomcat 10+ / 11. Tomcat 9.x uses the legacy Java EE servlet namespace, while `ujfe-servlet` uses `jakarta.servlet.*`.

## What The Example Contains

- `HomePage`: a small UJFE page with a server-side signal and live click handler.
- `UjfeServletBootstrap`: a `ServletContextListener` that creates the router and registers `UjfeServlet`.
- `application.properties`: document/runtime settings used by the servlet runtime.

The properties file also sets `ujfe.live.max-json-payload-bytes`, the shared live JSON body limit used by `/_ujfe/event` and `/_ujfe/state`.

The bootstrap maps only:

- `/home`
- `/_ujfe/*`

That keeps unrelated static assets and application routes outside the UJFE servlet.

## Build

```bash
mvn package
```

Deploy the generated WAR to a Jakarta Servlet container.

Open:

```text
http://localhost:8080/ujfe-servlet-tomcat/home
```

## Bootstrap Pattern

The example uses explicit route registration:

```java
var routes = new ManualRouteSource()
        .register("/home", HomePage::new);

var router = new Router().register(routes);
var config = LiveSessionConfig.builder()
        .title("UJFE Servlet Tomcat Example")
        .devToolsEnabled(true)
        .build();

ServletRegistration.Dynamic servlet = context.addServlet("ujfe", new UjfeServlet(router, config));
servlet.setLoadOnStartup(1);
servlet.addMapping("/home", "/_ujfe/*");
```

This is the recommended production model because it avoids reflection-based route discovery and keeps route ownership explicit.

## Expected Requests

After loading `/home`, the browser will request:

```text
GET /ujfe-servlet-tomcat/home
GET /_ujfe/client.js
GET /_ujfe/dev.js
POST /_ujfe/state
```

When the button is clicked, the browser sends:

```text
POST /_ujfe/event
```

The event endpoint returns JSON with updated HTML and CSS.
