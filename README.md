# UJFE

**Modern Reactive UI Framework for the JVM**

UJFE is a standards-first, Java-first, HTML-first, server-first UI framework for JVM applications. It renders real HTML on the server, connects browser events to Java handlers, and lets teams use normal CSS strategies without requiring a Node.js toolchain.

UJFE is built for Java teams that want reactive web interfaces while preserving the Web Platform, server-side architecture, and existing framework investments.

## What UJFE Is

- A Java-first framework for authoring web UI with Java types and APIs.
- An HTML-first renderer that emits normal standards-based HTML.
- A server-first runtime where application state and event handlers stay on the JVM by default.
- A CSS agnostic UI layer that works with plain CSS, utility CSS, Tailwind, Bootstrap, CSS Modules, or enterprise design systems.
- A safe-by-default renderer that escapes text and attributes, sanitizes URL attributes, and uses opaque event ids.
- A framework that can run standalone or integrate with Spring Boot and Spring MVC.

## What UJFE Is Not

- UJFE is not a browser-side JavaScript framework clone.
- UJFE is not a proprietary HTML dialect.
- UJFE is not a mandatory CSS framework.
- UJFE is not a replacement for Spring Boot or Spring MVC.

## Core Principles

- HTML-first: UJFE starts from real HTML elements, attributes, and document semantics.
- Standards-first: UJFE follows the Web Platform instead of hiding it behind a closed component model.
- Java-first: UI code, state, handlers, and integration points are authored in Java.
- Server-first: rendering and interaction handling are owned by the JVM unless an application explicitly chooses otherwise.
- Safe by default: normal rendering escapes untrusted content and makes unsafe operations explicit.
- CSS agnostic: generated markup uses standard `class` attributes and does not force a styling system.
- No required Node.js toolchain: Node.js, npm, TypeScript, Babel, and bundlers are not required for normal UJFE applications.

## Documentation

- [Vision](docs/vision.md)
- [Principles](docs/principles.md)
- [Generic HTML elements](docs/html/generic-elements.md)
- [Standard HTML helpers](docs/html/HELPERS.md)
- [HTML void elements](docs/html/void-elements.md)
- [Raw HTML](docs/html/raw-html.md)
- [Attributes](docs/html/attributes.md)
- [Unsafe HTML escape hatch](docs/security/unsafe-html.md)
- [Attribute validation](docs/security/attribute-validation.md)
- [Safe URL policy](docs/security/safe-url.md)

## Modules

- `ujfe-core`: shared rendering contracts, context, escaping, client state, and REST client.
- `ujfe-html`: Java HTML DSL, generic element core, HTML helpers, and optional server-side utility CSS renderer.
- `ujfe-signals`: mutable signals and computed values.
- `ujfe-router`: `@Page` routing and page rendering.
- `ujfe-live`: live event registry, page re-rendering, client script, and dev tools script.
- `ujfe-http`: Netty HTTP runtime for UJFE sessions.
- `ujfe-spring`: Spring Boot/MVC integration that serves UJFE routes through the same DispatcherServlet/Tomcat port as the application.
- `ujfe-cli`: CLI entry point and HTML-to-UJFE conversion command.
- `examples`: runnable example application.

## Requirements

- JDK compatible with the Maven compiler release in `pom.xml`.
- Full reactor builds that include `ujfe-spring` require Java 17 or newer because Spring Boot 4 / Spring Framework 7 are compiled for Java 17+. Java 25 is the recommended runtime for the Spring Boot 4 setup below.
- Maven Wrapper from this repository.

## Build And Test

```bash
./mvnw test
```

```bash
./mvnw verify
```

```bash
./mvnw -DskipTests package
```

## Run The Example Application

```bash
./mvnw -pl examples -am exec:java -Dexec.mainClass=app.Main
```

Open:

```text
http://localhost:8080
```

Available example routes:

- `/`: interactive SSR example with live events, signals, forms, cookies, and local storage reads.
- `/docs`: documentation page showing DSL elements, forms, CSS utilities, security behavior, REST usage, CLI usage, and dev preview.

## Minimal Page Example

```java
package app.pages;

import ujfe.html.Node;
import ujfe.html.Element;
import ujfe.router.Page;

import static ujfe.html.UI.*;

@Page("/")
public final class HomePage {
    public Node render() {
        return main()
                .css("min-h-screen bg-slate-50 text-slate-900 p-6")
                .child(h1("UJFE"))
                .child(p("Server-rendered UI written in Java."))
                .child(Element.of("dialog")
                        .attr("open", true)
                        .child(p("Generic tags work without a framework release.")));
    }
}
```

## HTML DSL Principles

`Element.of("tag-name")` is the architectural source of HTML support. Helpers such as `div()`, `section()`, `dialog()`, `table()`, `template()`, `svg()`, and `math()` are convenience methods only.

```java
section()
        .child(h1("Dashboard"))
        .child(Element.of("dialog")
                .attr("open", true)
                .child(p("Example")))
        .child(Element.of("future-html-element")
                .attr("data-ready", true))
        .child(Element.of("my-card")
                .attr("data-state", "ready")
                .child("Custom element"))
        .child(input()
                .attr("type", "text")
                .attr("placeholder", "Name")
                .attr("required", true));
```

The DSL validates element and attribute names for safe rendering, but it does not maintain a whitelist of supported HTML tags. New HTML tags, custom elements, and Web Components can be rendered through `Element.of(...)` or `element(...)`. Invalid tag names such as `""`, `" "`, `"<script>"`, and `"div onclick=alert(1)"` are rejected before rendering.

HTML void elements such as `img`, `input`, `br`, and `meta` render without closing tags. UJFE rejects children on void elements explicitly instead of silently dropping content.

Raw HTML is intentionally unavailable through neutral names. If trusted pre-rendered HTML must bypass escaping, use the explicit unsafe escape hatch:

```java
unsafeHtml("<p>Trusted HTML</p>");
UnsafeHtml.of("<p>Trusted HTML</p>");
```

`unsafeHtml(...)` renders raw content without escaping. Never pass user input, unsanitized Markdown, request data, or untrusted third-party content to this API.

SVG and MathML have controlled namespace factories for generic tags:

```java
Element.of("svg")
        .attr("viewBox", "0 0 10 10")
        .child(Element.svg("path").attr("d", "M0 0h10v10H0z"));

Element.of("math")
        .child(Element.mathMl("mi").child("x"))
        .child(Element.mathMl("mo").child("="))
        .child(Element.mathMl("mn").child("1"));
```

Current helpers include document tags (`html`, `head`, `body`, `title`, `meta`, `link`, `style`, `script`, `base`), semantic/layout tags, text tags, grouping tags, lists, media and embedded tags (`img`, `picture`, `source`, `track`, `audio`, `video`, `canvas`, `svg`, `map`, `area`, `iframe`, `object`, `embed`, `param`), table tags (`table`, `thead`, `tbody`, `tfoot`, `tr`, `td`, `th`, `caption`, `colgroup`, `col`), form tags, `details`, `summary`, `dialog`, `template`, `slot`, and `math`.

| Group | Helpers |
| --- | --- |
| Document structure | `html()`, `head()`, `body()`, `title()`, `meta()`, `link()`, `style()`, `script()`, `base()` |
| Semantic layout | `main()`, `section()`, `article()`, `aside()`, `header()`, `footer()`, `nav()`, `address()` |
| Text | `h1()`-`h6()`, `p()`, `span()`, `strong()`, `em()`, `small()`, `mark()`, `abbr()`, `cite()`, `code()`, `pre()`, `blockquote()`, `q()`, `br()`, `hr()` |
| Grouping and lists | `div()`, `figure()`, `figcaption()`, `details()`, `summary()`, `dialog()`, `ul()`, `ol()`, `li()`, `dl()`, `dt()`, `dd()` |
| Navigation, media, and embedded content | `a()`, `img()`, `picture()`, `source()`, `audio()`, `video()`, `track()`, `canvas()`, `svg()`, `map()`, `area()`, `iframe()`, `embed()`, `object()`, `param()` |
| Tables | `table()`, `thead()`, `tbody()`, `tfoot()`, `tr()`, `td()`, `th()`, `caption()`, `colgroup()`, `col()` |
| Forms | `form()`, `input()`, `textarea()`, `button()`, `select()`, `option()`, `optgroup()`, `label()`, `fieldset()`, `legend()`, `datalist()`, `output()`, `progress()`, `meter()` |
| Templates and Web Components | `template()`, `slot()` |

## Modern Java Examples

UJFE examples prefer modern Java syntax when it improves readability: `var` for local values, records for immutable view models, streams and lambdas for collection-to-node mapping, and switch expressions for compact branching.

```java
record Metric(String label, int value, String status) {}

var metrics = List.of(
        new Metric("Users", 42, "ok"),
        new Metric("Errors", 2, "warn")
);

var rows = metrics.stream()
        .map(metric -> {
            var tone = switch (metric.status()) {
                case "ok" -> "text-emerald-700";
                case "warn" -> "text-amber-700";
                default -> "text-slate-700";
            };
            return tr()
                    .child(td(metric.label()))
                    .child(td(String.valueOf(metric.value())).css(tone));
        })
        .toList();

table()
        .child(caption("Metrics"))
        .child(colgroup().child(col()).child(col()))
        .child(thead().child(tr().child(th("Label")).child(th("Value"))))
        .child(tbody().children(rows))
        .child(tfoot().child(tr().child(td().attr("colspan", "2").child("Generated from Java data"))));
```

For quick experiments with Java 25, source-file execution lets you run a compact demo without creating a Maven project or declaring an explicit class:

```java
// Demo.java - run with: java Demo.java
import java.util.List;
import ujfe.core.ClientState;
import ujfe.core.Node;
import ujfe.live.LiveSession;
import ujfe.router.Router;

import static ujfe.html.UI.*;

record Metric(String label, int value) {}

record MetricsPage(List<Metric> metrics) {
    public Node render() {
        var rows = metrics.stream()
                .map(metric -> tr()
                        .child(td(metric.label()))
                        .child(td(String.valueOf(metric.value()))))
                .toList();

        return table()
                .child(caption("Metrics"))
                .child(tbody().children(rows));
    }
}

void main() {
    var metrics = List.of(new Metric("Users", 42));
    var router = new Router().register("/", () -> new MetricsPage(metrics));
    var liveSession = new LiveSession(router);
    System.out.println(liveSession.renderDocument("/", ClientState.empty()));
}
```

## Router And Server Initialization

```java
package app;

import app.pages.HomePage;
import ujfe.http.UjfeServer;
import ujfe.http.UjfeServerConfig;
import ujfe.live.LiveSession;
import ujfe.live.LiveSessionConfig;
import ujfe.router.Router;

public final class Main {
    public static void main(String[] args) throws InterruptedException {
        var router = new Router()
                .register(new HomePage());

        var config = LiveSessionConfig.builder()
                .lang("en")
                .title("UJFE")
                .build();
        var liveSession = new LiveSession(router, config);

        var server = new UjfeServer(
                UjfeServerConfig.builder()
                        .host("0.0.0.0")
                        .port(8080)
                        .build(),
                liveSession
        );

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
        server.blockUntilShutdown();
    }
}
```

## Live State Example

```java
package app.components;

import ujfe.html.Node;
import ujfe.signals.Signal;
import ujfe.signals.Signals;

import static ujfe.html.UI.*;

public final class CounterComponent {
    private final Signal<Integer> count = Signals.signal(0);

    public Node render() {
        return section()
                .css("rounded border border-slate-200 bg-white p-4")
                .child(h2("Counter"))
                .child(p(() -> "Count: " + count.get()))
                .child(button("Increment").onClick(() -> count.update(value -> value + 1)));
    }
}
```

## Form Example

```java
import ujfe.html.Node;

import static ujfe.html.UI.*;

public Node renderForm() {
    return form()
            .onSubmit(this::submit)
            .child(label("Name").forId("name"))
            .child(input()
                    .attr("type", "text")
                    .attr("id", "name")
                    .attr("name", "name")
                    .attr("required", true))
            .child(label("Role").forId("role"))
            .child(select().id("role").name("role")
                    .child(option("Backend").value("backend"))
                    .child(option("Full stack").value("fullstack").selected(true)))
            .child(button("Submit").type("submit"));
}

private void submit() {
    // Server-side submit handler.
}
```

## Browser State Example

```java
import ujfe.core.Ujfe;

String cookie = Ujfe.cookie("session").orElse("missing");
String theme = Ujfe.localStorage("theme").orElse("system");
```

## REST Client Example

```java
import ujfe.core.RestClient;
import ujfe.core.RestResponse;

var client = RestClient.create();
var response = client.get("https://brasilapi.com.br/api/banks/v1");

if (response.statusCode() == 200) {
    var json = response.body();
}
```

## CLI Example

```bash
java -cp ujfe-cli/target/classes ujfe.cli.UjfeCli convert page.html --out src/main/java/app/pages/Page.java --type html
```

The convert command reads an HTML file and writes a Java page using the UJFE DSL.

The generated code prefers attributes-first output:

```java
section()
        .attr("class", "p-4")
        .child(h1().attr("title", "Hero").child(text("Hello")));
```

## CSS Modes

UJFE can run with its internal server-side utility CSS renderer or with external CSS managed by Tailwind, Bootstrap, CSS files, CSS Modules, or an enterprise design system. With the bundled Netty runtime, keep external stylesheets same-origin so they fit the default CSP. With Spring MVC integration, the same configuration is rendered by Tomcat through the application port.

Internal CSS is the default:

```java
LiveSession liveSession = new LiveSession(router);
```

External CSS disables the UJFE stylesheet and adds regular head nodes:

```java
import ujfe.live.CssMode;
import ujfe.live.LiveSessionConfig;

LiveSessionConfig config = LiveSessionConfig.builder()
        .cssMode(CssMode.EXTERNAL)
        .externalStylesheet("/app.css")
        .title("UJFE App")
        .build();

LiveSession liveSession = new LiveSession(router, config);
```

## Public API Reference

The public API is documented in English so the project can be used globally. The high-level surface is intentionally small:

- `ujfe.html.Element`: generic HTML element core. Use `Element.of(tagName)` or `element(tagName)` for any valid HTML/custom/future tag, `Element.svg(tagName)` for SVG descendants, and `Element.mathMl(tagName)` for MathML descendants. Use `attr(name, value)`, `attr(name, true)`, `boolAttr(...)`, `child(...)`, `children(...)`, `css(...)`, and event methods such as `onClick(...)`. Attribute names are validated at construction time; inline event handler attributes (`on*`) are blocked by default.
- `ujfe.html.UI`: optional helper factories for official HTML tags. Helpers delegate to `Element.of(...)`; they are convenience methods, not the source of HTML support. `unsafeHtml(...)` is the explicit raw HTML escape hatch for trusted content only.
- `ujfe.html.UnsafeHtml`: intentionally unsafe raw HTML node. It bypasses escaping and should only receive trusted, sanitized HTML.
- `ujfe.html.UrlPolicy`: configurable URL scheme policy. The default allows `https` and `data:image/*`. Applications opt into `http`, `mailto`, `tel`, or custom schemes through `UrlPolicy.builder()`. `javascript:` and `vbscript:` are permanently blocked.
- `ujfe.html.SafeUrl`: URL sanitizer for HTML URL-bearing attributes. Delegates scheme decisions to the active `UrlPolicy`. URL attributes (`href`, `src`, `action`, `poster`, `formaction`, `cite`, `data`, `background`) are routed through this sanitizer.
- `ujfe.live.LiveSessionConfig`: document-level runtime configuration for language, title, head nodes, CSS mode, theme supplier, and dev tools.
- `ujfe.live.CssMode`: `INTERNAL` generates UJFE's server-side utility stylesheet; `EXTERNAL` disables it so teams can use Tailwind, Bootstrap, CSS files, CSS Modules, or enterprise design systems.
- `ujfe.router.Router` and `@Page`: register page instances, page classes, or explicit route factories.
- `ujfe.http.UjfeServer`: standalone Netty runtime for UJFE sessions.
- `ujfe.spring`: Spring Boot/MVC adapter that serves UJFE routes through the same DispatcherServlet/Tomcat port as the application.
- `ujfe.signals.Signal` and `Signals`: mutable and computed state primitives for live server-rendered components.
- `ujfe.core.RestClient` and `RestResponse`: small Java HttpClient wrapper for server-side API calls.
- `ujfe.cli.UjfeCli`: command-line entry point, including HTML-to-UJFE conversion.

## Documentation Pattern

Use the example documentation route as the preferred structure for framework docs:

- Show the rendered result.
- Show the equivalent Java DSL code.
- Keep examples small and executable.
- Link examples to source files under `examples/src/main/java/app`.
- Cover security behavior when documenting HTML attributes, URLs, events, or client state.

## Development Notes

- Text nodes and attributes are escaped during server-side rendering.
- Attribute names are validated before storage: whitespace, `<`, `=`, and other dangerous characters are rejected.
- Inline event handler attributes (`onclick`, `onload`, `onerror`, and all `on*`) are blocked by default. Use `Element.on(event, handler)` for server-side live events.
- `aria-*`, `data-*`, and `hx-*` attributes are fully supported through the generic `attr(...)` API.
- URL attributes (`href`, `src`, `action`, `poster`, `formaction`, `cite`, `data`, `background`) are sanitized through a configurable `UrlPolicy`.
- `javascript:` and `vbscript:` URL schemes are unconditionally blocked. `http:`, `mailto:`, and `tel:` are configurable through `UrlPolicy.builder()`.
- `data:image/*` URLs are allowed by default with a documented MIME prefix validation limitation.
- Live event handlers are registered server-side and rendered as opaque event ids.
- Raw HTML rendering is available only through APIs containing `unsafe` in the name, such as `unsafeHtml(...)` and `UnsafeHtml.of(...)`.
- HTML void elements are centralized in `HtmlElementMetadata`, render without closing tags, and reject children explicitly.
- `Element.of(...)` and `element(...)` support any valid current or future HTML tag name without a framework release.
- Custom elements and Web Components can be rendered through `Element.of("my-card")`; `Element.custom(...)` is available when the hyphen rule should be explicit.
- SVG and MathML generic descendants can be created with `Element.svg(...)` and `Element.mathMl(...)`.
- Helpers cover common modern HTML tags and delegate to the generic element core.
- Boolean attributes can be written as `.attr("required", true)` or through optional sugar such as `.required(true)`.
- In internal CSS mode, classes used during rendering are collected and rendered into a minimal stylesheet.
- In external CSS mode, UJFE does not inject its internal stylesheet and regular `<link rel="stylesheet">` nodes can be configured in the document head.
- The example app enables live dev tooling through `LiveSessionConfig`.

## Spring Initializr Project Setup

Use this setup when creating a new Spring Boot application that consumes the UJFE libraries from the local Maven repository.

Spring Initializr web flow:

- Open `https://start.spring.io/`.
- Select `Project: Maven`.
- Select `Language: Java`.
- Select the latest stable `Spring Boot 4.x` version available.
- Select `Packaging: Jar`.
- Select `Java: 25`.
- Set explicit project metadata:
  - `Group`: `com.example`
  - `Artifact`: `ujfe-spring-demo`
  - `Name`: `ujfe-spring-demo`
  - `Package name`: `com.example.ujfespringdemo`
- Add dependencies:
  - `Spring Web`
  - `Spring Modulith`
- Generate, extract, and open the project.

Generator options:

- `Maven` is the build system.
- `Java 25` is the runtime and compiler target.
- `web` is the Initializr dependency id for Spring Web.
- `modulith` is the Initializr dependency id for Spring Modulith.
- UJFE dependencies are added after generation because they are local artifacts until publication to Maven Central.

Equivalent Initializr API request:

```bash
curl "https://start.spring.io/starter.zip?type=maven-project&language=java&bootVersion=<SPRING_BOOT_4_VERSION>&javaVersion=25&dependencies=web,modulith&groupId=com.example&artifactId=ujfe-spring-demo&name=ujfe-spring-demo&packageName=com.example.ujfespringdemo" -o ujfe-spring-demo.zip
```

Replace `<SPRING_BOOT_4_VERSION>` with the exact Spring Boot 4.x version listed by Spring Initializr.

Install UJFE into the local Maven repository from this repository:

```bash
./mvnw install
```

Use the UJFE version installed in `~/.m2`. The current local project version is:

```xml
<ujfe.version>0.7.0-SNAPSHOT</ujfe.version>
```

Add the UJFE Spring dependency to the generated Spring project:

```xml
<properties>
    <java.version>25</java.version>
    <ujfe.version>0.7.0-SNAPSHOT</ujfe.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webmvc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.modulith</groupId>
        <artifactId>spring-modulith-starter-core</artifactId>
    </dependency>
    <dependency>
        <groupId>dev.ujfe</groupId>
        <artifactId>ujfe-spring</artifactId>
        <version>${ujfe.version}</version>
    </dependency>
</dependencies>
```

Do not add `ujfe-http` to a Spring Boot application unless you intentionally want a separate standalone Netty server. The Spring adapter uses Spring MVC request handling, so UJFE pages, live events, dev scripts, and CSS endpoints are served by the same Tomcat port as the rest of the application.

After UJFE is published to Maven Central, keep the same coordinates and use the published version instead of the local snapshot.

Enable virtual threads in `src/main/resources/application.yml`:

```yaml
spring:
  threads:
    virtual:
      enabled: true
  main:
    keep-alive: true
```

Create a UJFE page at `src/main/java/com/example/ujfespringdemo/web/HomePage.java`:

```java
package com.example.ujfespringdemo.web;

import org.springframework.stereotype.Component;
import ujfe.core.Node;
import ujfe.router.Page;

import static ujfe.html.UI.*;

@Page("/")
@Component
public final class HomePage {

    public Node render() {
        return main()
                .css("min-h-screen bg-slate-50 text-slate-900 flex items-center justify-center p-6")
                .child(section()
                        .css("w-full max-w-6xl rounded-lg border border-slate-200 bg-white p-6 shadow-sm flex flex-col gap-4")
                        .child(span("UJFE + Spring Boot").css("text-sm font-bold text-emerald-700"))
                        .child(h1("Server-rendered UI with UJFE").css("text-3xl font-bold text-slate-900"))
                        .child(p("This page is built with the UJFE Java DSL and served by Spring Web.")
                                .css("text-base text-slate-700 leading-relaxed"))
                        .child(div()
                                .css("rounded border border-emerald-200 bg-emerald-50 p-4")
                                .child(p("The HTML nodes, CSS utility collection, escaping, and rendering context come from the local UJFE libraries.")
                                        .css("text-sm text-emerald-700 leading-relaxed"))));
    }
}
```

Register the UJFE router at `src/main/java/com/example/ujfespringdemo/web/UjfeConfig.java`:

```java
package com.example.ujfespringdemo.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ujfe.live.LiveSessionConfig;
import ujfe.router.Router;

@Configuration
public class UjfeConfig {
    @Bean
    Router ujfeRouter(HomePage homePage) {
        return new Router()
                .register(homePage);
    }

    @Bean
    LiveSessionConfig ujfeLiveSessionConfig() {
        return LiveSessionConfig.builder()
                .lang("en")
                .title("UJFE Spring Demo")
                .build();
    }
}
```

When a `Router` bean exists, `ujfe-spring` auto-configures `LiveSession`, `UjfeSpringHandler`, and `UjfeSpringHandlerMapping`. The handler mapping only claims routes registered in the UJFE `Router` plus internal `/_ujfe/*` endpoints, so regular Spring controllers and static resources keep working normally.

Use external CSS the same way when the Spring app owns the stylesheet pipeline:

```java
@Bean
LiveSessionConfig ujfeLiveSessionConfig() {
    return LiveSessionConfig.builder()
            .cssMode(CssMode.EXTERNAL)
            .externalStylesheet("/app.css")
            .lang("en")
            .title("UJFE Spring Demo")
            .build();
}
```

Run and test:

```bash
./mvnw spring-boot:run
```

```bash
curl http://localhost:8080/
```

Open:

```text
http://localhost:8080/
```
