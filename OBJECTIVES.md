# UJFE Project Objectives

UJFE is a **Modern Reactive UI Framework for the JVM**. The project objective is to provide a Java-first reactive web framework for generating standards-compliant web pages using Java as the primary application language, without forcing a JavaScript build pipeline, a proprietary CSS system, or a closed rendering model.

UJFE should let Java developers build real web interfaces using real HTML, standard CSS integration points, server-side state, live events, and modern Java language features. It should be useful as a standalone web framework and as an integration layer inside existing Spring Boot applications.

## Core Vision

UJFE exists to make Java a productive language for web UI construction while preserving the long-term strengths of the Web Platform.

The framework should:

- Render real HTML, not a custom component tree that only resembles HTML.
- Support valid W3C/WHATWG HTML by architecture, not only by a fixed list of hardcoded helper methods.
- Keep the authoring model declarative and compact.
- Provide a reactive server-side programming model through signals, events, and render updates.
- Allow teams to choose their CSS strategy, including internal utility CSS, Tailwind, Bootstrap, plain CSS, CSS Modules, enterprise design systems, and token-based corporate styling.
- Integrate naturally with Spring Boot, Spring MVC, and Spring Modulith.
- Run outside Spring Boot using standalone server runtimes such as Netty or Tomcat.
- Avoid Node, npm, TypeScript, Babel, bundlers, and client-side framework lock-in as requirements.

UJFE should feel like a modern Java web UI layer: simple enough for direct page rendering, structured enough for enterprise modular applications, and standards-aligned enough to remain useful as HTML evolves.

## HTML Objectives

HTML support is the most important architectural pillar of UJFE.

UJFE must support all valid HTML elements by principle, including current HTML elements, future HTML elements, custom elements, and Web Components tags. This must not depend on a framework release for each new tag added by the HTML Living Standard.

The architectural source of HTML support is the generic element core:

```java
Element.of("dialog")
        .attr("open", true)
        .child(p("Native dialog content"));
```

Helper methods are convenience APIs:

```java
dialog()
        .attr("open", true)
        .child(p("Native dialog content"));
```

The helper layer should cover the complete common HTML surface:

- Document structure: `html`, `head`, `body`, `title`, `meta`, `link`, `style`, `script`, `base`.
- Semantic layout: `main`, `section`, `article`, `aside`, `header`, `footer`, `nav`, `address`.
- Text content: `h1` through `h6`, `p`, `span`, `strong`, `em`, `small`, `mark`, `abbr`, `cite`, `code`, `pre`, `blockquote`, `q`, `br`, `hr`.
- Grouping: `div`, `figure`, `figcaption`, `details`, `summary`, `dialog`.
- Lists: `ul`, `ol`, `li`, `dl`, `dt`, `dd`.
- Navigation: `a`.
- Media and embedded content: `img`, `picture`, `source`, `audio`, `video`, `track`, `canvas`, `svg`, `map`, `area`, `iframe`, `embed`, `object`, `param`.
- Tables: `table`, `thead`, `tbody`, `tfoot`, `tr`, `td`, `th`, `caption`, `colgroup`, `col`.
- Forms: `form`, `input`, `textarea`, `button`, `select`, `option`, `optgroup`, `label`, `fieldset`, `legend`, `datalist`, `output`, `progress`, `meter`.
- Modern interactivity: `details`, `summary`, `dialog`, `popover`.
- Templates and Web Components: `template`, `slot`.

No helper may become a limitation. If a helper does not exist, the developer must still be able to render the tag:

```java
Element.of("future-html-element")
        .attr("data-ready", true);
```

## Attributes-First Objective

UJFE should use attributes-first APIs as the default HTML model.

This means every HTML attribute should be expressible through a generic API:

```java
input()
        .attr("type", "text")
        .attr("placeholder", "Name")
        .attr("required", true);
```

The framework should not attempt to hardcode fluent methods for every possible HTML attribute. That approach creates a maintenance burden and delays support for new platform features.

Convenience methods are allowed when they improve readability for frequently used attributes:

```java
inputText()
        .placeholder("Name")
        .required(true);
```

But these methods must remain optional sugar over the generic attribute model. The generic `attr(...)` API is the compatibility contract.

## Pure HTML Escape Hatch

UJFE should provide an explicit unsafe HTML escape hatch for cases where developers need to render trusted raw HTML.

The intended shape is an API such as:

```java
UnsafeHTML("<cms-fragment><p>Trusted HTML</p></cms-fragment>")
```

or:

```java
unsafeHtml(trustedHtmlFromCms)
```

This API must be intentionally named unsafe. It must never be confused with normal text rendering, because normal text rendering must escape content by default.

The unsafe HTML helper should be used for:

- Trusted CMS fragments.
- Pre-sanitized documentation content.
- Controlled migration from existing server-rendered HTML.
- Integration with external systems that already produce validated HTML.

It must not be the default mechanism for building pages. The primary model remains structured nodes and safe text/attribute escaping.

## Reactive Rendering Objective

UJFE should provide a reactive server-side rendering model where Java owns the page state and event handlers.

The model should include:

- Signals for mutable state.
- Computed values for derived state.
- Server-side events such as click, input, change, and submit.
- Stable opaque event ids rendered into HTML.
- Re-rendering after event handling.
- Client state synchronization for cookies, local storage, and other explicitly supported browser state.

Example direction:

```java
private final Signal<Integer> count = Signals.signal(0);

public Node render() {
    return section()
            .child(p(() -> "Count: " + count.get()))
            .child(button("Increment")
                    .onClick(() -> count.update(value -> value + 1)));
}
```

The framework should keep the reactive model Java-first. JavaScript can be used internally by the client bridge, but application authors should not be forced to write JavaScript for normal server-side interactions.

## Hooks Objective

UJFE should support hooks as explicit framework extension points. These hooks should be server-side Java hooks, not a copy of React's client-side runtime model.

Hooks should allow developers and integrations to participate in:

- Request context initialization.
- Route resolution.
- Page rendering.
- Head node contribution.
- CSS collection and stylesheet generation.
- Event dispatch.
- Client state merge.
- Error handling.
- Redirects and response customization.
- Observability and tracing.

Examples of desired hook categories:

```java
beforeRender(context -> ...)
afterRender(result -> ...)
onEvent(event -> ...)
onError(error -> ...)
contributeHead(head -> ...)
```

Hooks should be composable, deterministic, and safe for enterprise applications. They should not hide control flow or introduce implicit global behavior that makes rendering difficult to reason about.

## CSS Objectives

UJFE must be CSS agnostic.

The framework should never require:

- A proprietary CSS system.
- Mandatory CSS-in-JS.
- A JavaScript build pipeline.
- A specific design system.
- A specific utility framework.

UJFE should support two major CSS modes.

### Internal CSS Mode

Internal CSS mode is useful for quick applications, demos, prototypes, and teams that want a Java-only experience.

In this mode, UJFE may collect utility-like class names during rendering and generate a stylesheet on the server. The utility syntax may be Tailwind-like for developer familiarity:

```java
section()
        .css("flex flex-col gap-4 p-6 rounded border bg-white");
```

The internal renderer should remain optional and predictable. It should not prevent teams from moving to external CSS.

### External CSS Mode

External CSS mode disables UJFE's internal stylesheet generation and lets the application use standard CSS integration:

```java
LiveSessionConfig.builder()
        .cssMode(CssMode.EXTERNAL)
        .externalStylesheet("/app.css")
        .build();
```

External CSS should work with:

- Tailwind.
- Bootstrap.
- Plain CSS.
- CSS Modules.
- Corporate design systems.
- Design tokens.
- Existing enterprise style pipelines.

The generated HTML should remain normal HTML with normal `class` attributes so any CSS tool can target it.

## Runtime Objectives

UJFE should support multiple deployment models without changing the page authoring model.

The same page code should be usable in:

- A standalone pure Java application.
- A standalone Netty runtime.
- A standalone Servlet/Tomcat runtime.
- A Spring Boot application.
- A Spring Boot application using Spring MVC routes.
- A Spring Boot application organized as a Spring Modulith module.

## Standalone Runtime Objective

In pure UJFE mode, the developer should be able to run UJFE without Spring Boot.

Standalone runtimes should include:

- Netty for a compact event-loop based server.
- Tomcat or a Servlet-based runtime for teams that prefer Servlet infrastructure outside Spring Boot.

The standalone runtime should own:

- HTTP route dispatch for UJFE pages.
- Internal UJFE endpoints such as live events, client script, dev tools script, and CSS.
- Security headers.
- Error responses.
- Lifecycle start and stop.

Example direction:

```java
var router = new Router()
        .register(new HomePage());

var liveSession = new LiveSession(router);

var server = new UjfeServer(
        UjfeServerConfig.builder()
                .host("0.0.0.0")
                .port(8080)
                .build(),
        liveSession
);

server.start();
server.blockUntilShutdown();
```

## Spring Boot Integration Objective

When UJFE runs inside Spring Boot, it must use Spring Boot's web infrastructure instead of starting a competing server.

This is a must-have architectural rule:

- UJFE must not start Netty inside a Spring Boot MVC application unless explicitly requested as a separate server.
- UJFE must serve pages through the same `DispatcherServlet`.
- UJFE must use the same embedded Tomcat port, or whatever servlet container Spring Boot is configured to use.
- UJFE routes must coexist with Spring MVC controllers.
- Static resources, Spring Security, filters, interceptors, observability, and application configuration should remain part of the normal Spring infrastructure.

The Spring integration should expose UJFE as an adapter:

```java
@Configuration
class UjfeConfig {
    @Bean
    Router ujfeRouter(HomePage homePage) {
        return new Router().register(homePage);
    }

    @Bean
    LiveSessionConfig ujfeLiveSessionConfig() {
        return LiveSessionConfig.builder()
                .lang("en")
                .title("UJFE Spring App")
                .build();
    }
}
```

With this model, Spring Boot owns the process and UJFE owns only its registered routes and internal endpoints.

## Spring MVC Route Coexistence Objective

UJFE must be able to share one Spring Boot application with regular Spring MVC.

For example:

- `/dashboard` can be a UJFE page.
- `/admin/users` can be a UJFE page.
- `/api/users` can remain a Spring MVC REST controller.
- `/assets/app.css` can remain a Spring static resource.
- `/actuator/health` can remain a Spring Boot actuator endpoint.

The UJFE Spring handler mapping should only claim:

- Paths registered in the UJFE `Router`.
- Internal framework paths such as `/_ujfe/client.js`, `/_ujfe/event`, `/_ujfe/state`, `/_ujfe/css`, and `/_ujfe/dev.js`.

This avoids route hijacking and lets UJFE be introduced incrementally into existing Spring applications.

## Spring Modulith Objective

UJFE should work naturally as one module inside a Spring Modulith application.

In a modular Spring Boot system, UJFE can be used as:

- A UI module for a bounded context.
- A server-rendered admin console.
- A dashboard module.
- A back-office workflow module.
- A documentation or operational UI module.

The UJFE module should depend on application services through normal Spring dependency injection. Domain modules should not depend on UJFE. This keeps the architecture clean:

```text
domain modules -> no UI dependency
application services -> reusable by MVC, REST, jobs, and UJFE
ujfe-web module -> depends on application services and renders pages
```

UJFE should not require the application to abandon Spring MVC, Spring Security, Spring Data, Spring Modulith events, or existing service boundaries.

## Page Authoring Objectives

UJFE pages should be plain Java types.

They may be:

- Simple classes with a `render()` method.
- Components implementing a common rendering contract.
- Spring beans when used inside Spring Boot.
- Route factories registered manually in pure mode.

Example:

```java
@Page("/")
public final class HomePage {
    public Node render() {
        return main()
                .child(h1("Dashboard"))
                .child(section()
                        .child(p("Server-rendered with Java.")));
    }
}
```

The page model should support modern Java idioms:

- `var` for local values.
- Records for immutable view models.
- Streams for declarative collection rendering.
- Switch expressions for compact state-based rendering.
- Single-file source execution for quick experiments on modern Java.

Example table rendering:

```java
record Metric(String name, int value, String status) {}

var rows = metrics.stream()
        .map(metric -> tr()
                .child(td(metric.name()))
                .child(td(String.valueOf(metric.value()))))
        .toList();

return table()
        .child(caption("Metrics"))
        .child(thead().child(tr().child(th("Name")).child(th("Value"))))
        .child(tbody().children(rows));
```

## Security Objectives

UJFE must be safe by default.

The framework should:

- Escape text nodes.
- Escape attribute values.
- Validate element and attribute names.
- Sanitize URL attributes such as `href`, `src`, `action`, and `poster`.
- Use opaque event ids instead of exposing Java method names.
- Emit secure default headers in standalone runtimes.
- Avoid inline application JavaScript as a normal authoring requirement.
- Clearly mark unsafe APIs, such as `UnsafeHTML(...)`, as unsafe.

Security should be explicit and understandable. Developers should be able to inspect generated HTML and reason about what is sent to the browser.

## Accessibility and SEO Objectives

Because UJFE renders standard HTML on the server, it should support accessibility and SEO as first-class outcomes.

The framework should encourage:

- Semantic HTML.
- Proper heading order.
- Native form elements.
- Native links and buttons.
- Real tables for tabular data.
- `aria-*` attributes through generic `attr(...)` and convenience helpers.
- Server-rendered content visible to crawlers and non-JavaScript clients.

UJFE should not hide the platform. It should make the correct HTML path easy.

## Developer Experience Objectives

The framework should provide a productive Java developer experience.

Developer experience goals include:

- Minimal setup for pure Java projects.
- Spring Boot auto-configuration for Spring applications.
- Clear module boundaries.
- Small examples that compile.
- Documentation in English for global adoption.
- A CLI for converting existing HTML into the Java DSL.
- Dev Preview tooling for inspecting generated elements and testing CSS classes.
- No required JavaScript build process.
- Helpful errors for route resolution, rendering, invalid tags, invalid attributes, and unsafe URLs.

## Module Objectives

The project should remain modular.

Expected module responsibilities:

- `ujfe-core`: shared rendering contracts, escaping, context, client state, and REST client primitives.
- `ujfe-core`: HTML element model, helpers, safe rendering, CSS utility rendering, themes, and raw HTML escape hatch.
- `ujfe-signals`: signals and computed values.
- `ujfe-router`: route annotations, route definitions, page registration, and page rendering.
- `ujfe-live`: live event registry, re-rendering, client bridge, document rendering, CSS mode configuration, hooks, and lifecycle integration.
- `ujfe-http`: standalone Netty runtime.
- `ujfe-servlet` or equivalent: standalone Servlet/Tomcat runtime outside Spring Boot.
- `ujfe-spring`: Spring Boot and Spring MVC adapter using the application server and `DispatcherServlet`.
- `ujfe-cli`: command-line tooling and HTML-to-UJFE conversion.
- `examples`: executable examples and documentation pages.

Each module should be independently understandable and avoid unnecessary coupling.

## Enterprise Objectives

UJFE should be suitable for enterprise environments.

This means:

- Interoperability with existing Java applications.
- Compatibility with Spring Boot and Spring MVC.
- Compatibility with Spring Modulith architecture.
- Low lock-in through standard HTML and standard CSS.
- A clear security model.
- Server-side rendering for SEO and performance.
- A path for progressive adoption.
- No forced replacement of existing REST controllers, MVC controllers, static assets, or security infrastructure.
- A maintainable API surface based on generic primitives plus optional helpers.

## Non-Goals

UJFE should not become:

- A browser-side JavaScript framework clone.
- A proprietary HTML dialect.
- A mandatory CSS framework.
- A mandatory CSS-in-JS solution.
- A replacement for Spring Boot.
- A replacement for Spring MVC.
- A replacement for enterprise design systems.
- A framework that requires a release for every new HTML tag.

UJFE should complement the Web Platform and the Java ecosystem rather than hide or replace them.

## Long-Term Direction

The long-term goal is a Java-native reactive SSR framework that can serve as:

- A standalone Java web UI framework.
- A Spring Boot UI plugin.
- A Spring MVC route participant.
- A Spring Modulith UI module.
- A standards-first alternative to JavaScript-heavy internal applications.
- A low-lock-in rendering layer for enterprise applications.

The strongest architectural commitment is this:

**UJFE should render real HTML, support the full Web Platform by design, and let Java teams build reactive web pages without surrendering control of their server architecture, CSS strategy, or application modularity.**
