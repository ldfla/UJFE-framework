# Spring MVC Integration

`ujfe-spring` integrates UJFE with Spring Boot and Spring MVC through normal Spring handler mappings. UJFE does not install a catch-all controller and does not take ownership of routes that belong to the host application.

## Route Ownership

`UjfeSpringHandlerMapping` claims only:

- registered UJFE page routes for `GET` requests;
- internal UJFE runtime paths under `/_ujfe/*`.

It does not claim:

- `/api/*` unless the application explicitly registers that exact path as a UJFE route and it is not a static asset path;
- `/actuator/*`;
- Spring MVC controller routes;
- Spring REST controller routes;
- Spring static resources;
- unknown non-UJFE routes;
- missing routes that are not registered in the UJFE `Router`.

Registered page routes are resolved through `Router.resolve(path)`. If a path is not in the UJFE router, the Spring handler mapping returns `null` so the rest of Spring MVC can continue resolving the request.

## Internal Endpoints

The Spring adapter handles UJFE runtime endpoints on the same Spring Boot port:

| Path | Method | Purpose |
| --- | --- | --- |
| `/_ujfe/client.js` | `GET` | Browser live-event bridge |
| `/_ujfe/dev.js` | `GET` | Optional dev tooling |
| `/_ujfe/css` | `GET` | Runtime CSS endpoint |
| `/_ujfe/event` | `POST` | Live event dispatch |
| `/_ujfe/state` | `POST` | Client-state synchronization |

Internal endpoints keep the same UJFE controls as the other adapters: safe error responses, CSRF validation for mutating endpoints, JSON payload limits, internal endpoint rate limiting, and runtime error reporting.

## Handler Mapping Order

`UjfeSpringHandlerMapping.DEFAULT_ORDER` is `100`.

This keeps UJFE behind Spring MVC controller mappings, which normally use lower order values, and ahead only when the UJFE mapping explicitly owns the path. Because UJFE returns no handler for unregistered paths, Spring resource handling, actuator mappings, custom handler mappings, and Boot error handling can continue normally.

If an application intentionally registers another `HandlerMapping` with a higher priority for the same path, Spring's normal ordering rules apply. UJFE does not bypass that order.

## Spring MVC Controllers

Regular controllers and REST controllers continue to work:

```java
@RestController
final class UserController {
    @GetMapping("/api/users")
    List<UserDto> users() {
        return service.findUsers();
    }
}
```

If `/api/users` is not registered in the UJFE `Router`, `UjfeSpringHandlerMapping` does not claim it. Spring MVC preserves the controller status, body, content type, interceptors, filters, and exception handling.

## Static Resources

Spring static resource handling remains responsible for common static paths such as:

- `/favicon.ico`
- `/assets/*`
- `/static/*`
- `/public/*`
- `/webjars/*`
- `/css/*`
- `/js/*`
- `/images/*`

The UJFE Spring handler mapping explicitly returns no handler for static asset-like paths. That allows Spring's resource chain, WebJars handling, cache headers, versioned resource resolvers, and security rules to behave normally.

If a static asset request is routed directly to `UjfeSpringHandler` by custom application wiring, the handler still returns the shared safe static asset response instead of rendering a UJFE page.

## Actuator

Spring Boot Actuator endpoints remain owned by Spring Boot and Spring Security. UJFE does not claim `/actuator/health`, `/actuator/info`, or other actuator paths unless an application deliberately changes Spring routing and registers the same path elsewhere.

If the actuator base path is customized, the same rule applies: UJFE only claims paths in the UJFE `Router` plus `/_ujfe/*`. Custom actuator base paths should not be registered as UJFE page routes.

## Missing Routes

Unknown non-UJFE paths follow normal Spring not-found behavior:

```text
GET /missing
```

If `/missing` is not registered in the UJFE router and is not under `/_ujfe/*`, UJFE returns no handler. The request can then be handled by another Spring mapping or by Spring's normal 404 flow.

## Spring Security

UJFE does not bypass Spring Security. UJFE pages and internal endpoints are served through the same Spring Boot application and the same servlet filter chain as the rest of the app.

Applications can secure UJFE routes with normal Spring Security matchers:

```java
http.authorizeHttpRequests(authorize -> authorize
        .requestMatchers("/dashboard").authenticated()
        .requestMatchers("/_ujfe/**").authenticated()
        .requestMatchers("/api/**").hasRole("API")
        .anyRequest().permitAll());
```

For mutating UJFE endpoints, keep UJFE's native CSRF protection enabled. If Spring Security CSRF is also enabled, configure it deliberately for `/_ujfe/event` and `/_ujfe/state`. The common setup is to ignore those two paths in Spring Security CSRF and rely on UJFE's session token, `X-UJFE-CSRF` header, and Origin/Referer validation.

Do not disable Spring Security for UJFE. Static resources, actuator endpoints, API routes, and UJFE routes should keep the same filter-chain behavior as any other Spring MVC endpoint.

## Example Wiring

```java
@Configuration
class UjfeConfig {
    @Bean
    Router ujfeRouter() {
        return new Router()
                .register("/dashboard", DashboardPage::new)
                .register("/docs", DocsPage::new);
    }

    @Bean
    LiveSessionConfig ujfeLiveSessionConfig() {
        return LiveSessionConfig.builder()
                .title("UJFE Spring App")
                .build();
    }
}
```

With this setup:

| Request | Owner |
| --- | --- |
| `GET /dashboard` | UJFE |
| `GET /docs` | UJFE |
| `GET /_ujfe/client.js` | UJFE internal endpoint |
| `POST /_ujfe/event` | UJFE internal endpoint |
| `GET /api/users` | Spring MVC, if a controller defines it |
| `GET /actuator/health` | Spring Boot Actuator, if enabled |
| `GET /assets/app.css` | Spring resource handling, if configured |
| `GET /missing` | Normal Spring not-found behavior |
