# UJFE

UJFE means **Using Java For Everything**. It is a Java-first reactive SSR framework that renders HTML, server-side utility CSS, live events, routing, signals, and a small Netty-based HTTP runtime without a JavaScript build pipeline.

## Modules

- `ujfe-core`: shared rendering contracts, context, escaping, client state, and REST client.
- `ujfe-html`: Java HTML DSL and server-side utility CSS renderer.
- `ujfe-signals`: mutable signals and computed values.
- `ujfe-router`: `@Page` routing and page rendering.
- `ujfe-live`: live event registry, page re-rendering, client script, and dev tools script.
- `ujfe-http`: Netty HTTP runtime for UJFE sessions.
- `ujfe-cli`: CLI entry point and HTML-to-UJFE conversion command.
- `examples`: runnable example application.

## Requirements

- JDK compatible with the Maven compiler release in `pom.xml`.
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
import ujfe.router.Page;

import static ujfe.html.UI.*;

@Page("/")
public final class HomePage {
    public Node render() {
        return main()
                .css("min-h-screen bg-slate-50 text-slate-900 p-6")
                .child(h1("UJFE"))
                .child(p("Server-rendered UI written in Java."));
    }
}
```

## Router And Server Initialization

```java
package app;

import app.pages.HomePage;
import ujfe.http.UjfeServer;
import ujfe.http.UjfeServerConfig;
import ujfe.live.LiveSession;
import ujfe.router.Router;

public final class Main {
    public static void main(String[] args) throws InterruptedException {
        Router router = new Router()
                .register(new HomePage());

        LiveSession liveSession = new LiveSession(router);

        UjfeServer server = new UjfeServer(
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
            .child(inputText().id("name").name("name").required(true))
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

RestClient client = RestClient.create();
RestResponse response = client.get("https://brasilapi.com.br/api/banks/v1");

if (response.statusCode() == 200) {
    String json = response.body();
}
```

## CLI Example

```bash
java -cp ujfe-cli/target/classes ujfe.cli.UjfeCli convert page.html --out src/main/java/app/pages/Page.java --type html
```

The convert command reads an HTML file and writes a Java page using the UJFE DSL.

## Documentation Pattern

Use the example documentation route as the preferred structure for framework docs:

- Show the rendered result.
- Show the equivalent Java DSL code.
- Keep examples small and executable.
- Link examples to source files under `examples/src/main/java/app`.
- Cover security behavior when documenting HTML attributes, URLs, events, or client state.

## Development Notes

- Text nodes and attributes are escaped during server-side rendering.
- URL attributes such as `href`, `src`, `action`, and `poster` are sanitized.
- Live event handlers are registered server-side and rendered as opaque event ids.
- CSS classes used during rendering are collected and rendered into a minimal stylesheet.
- The example app enables live dev tooling through the `LiveSession` constructor.

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
<ujfe.version>0.1.0-SNAPSHOT</ujfe.version>
```

Add the UJFE dependency to the generated Spring project:

```xml
<properties>
    <java.version>25</java.version>
    <ujfe.version>0.1.0-SNAPSHOT</ujfe.version>
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
        <artifactId>ujfe-html</artifactId>
        <version>${ujfe.version}</version>
    </dependency>
</dependencies>
```

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

import ujfe.core.Node;

import static ujfe.html.UI.*;

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

Create a Spring Web controller that renders the UJFE page at `src/main/java/com/example/ujfespringdemo/web/UjfePageController.java`:

```java
package com.example.ujfespringdemo.web;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ujfe.core.UjfeContext;
import ujfe.html.UtilityCssRenderer;

@RestController
public final class UjfePageController {
    private final HomePage homePage = new HomePage();

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String home() {
        UjfeContext context = UjfeContext.create();
        String body = UjfeContext.withCurrent(context, () -> homePage.render().render(context));
        String css = UtilityCssRenderer.render(context.cssClasses());

        return """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>UJFE Spring Demo</title>
                    <style>%s</style>
                </head>
                <body>%s</body>
                </html>
                """.formatted(css, body);
    }
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
