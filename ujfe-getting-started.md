# Getting Started with UJFE — Build a Todo App

This guide walks you through building a simple reactive todo list application in two ways:

1. **Standalone** — pure Java, no Spring, runs with Netty.
2. **With Spring Boot** — UJFE as a Spring Boot module.

Both use the same page code. Only the wiring changes.

---

## Prerequisites

- Java 11+ (Java 17+ for the Spring Boot path)
- Maven 3.9+
- UJFE installed in your local Maven repo:

```bash
git clone https://github.com/your-org/ujfe
cd ujfe
./mvnw install -DskipTests
```

Check the installed version:

```xml
<!-- use this in your pom.xml -->
<ujfe.version>0.26.0-SNAPSHOT</ujfe.version>
```

---

## Path A — Standalone (No Spring Boot)

### 1. Create the project

```
todo-app/
├── pom.xml
└── src/main/java/todo/
    ├── Main.java
    ├── Todo.java
    └── TodoPage.java
```

**pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
             https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>com.example</groupId>
  <artifactId>todo-app</artifactId>
  <version>1.0-SNAPSHOT</version>

  <properties>
    <maven.compiler.release>11</maven.compiler.release>
    <ujfe.version>0.26.0-SNAPSHOT</ujfe.version>
  </properties>

  <dependencies>
    <!-- HTML DSL -->
    <dependency>
      <groupId>dev.ujfe</groupId>
      <artifactId>ujfe-core</artifactId>
      <version>${ujfe.version}</version>
    </dependency>
    <!-- Signals -->
    <dependency>
      <groupId>dev.ujfe</groupId>
      <artifactId>ujfe-signals</artifactId>
      <version>${ujfe.version}</version>
    </dependency>
    <!-- Router -->
    <dependency>
      <groupId>dev.ujfe</groupId>
      <artifactId>ujfe-router</artifactId>
      <version>${ujfe.version}</version>
    </dependency>
    <!-- Live session + Netty server -->
    <dependency>
      <groupId>dev.ujfe</groupId>
      <artifactId>ujfe-live</artifactId>
      <version>${ujfe.version}</version>
    </dependency>
    <dependency>
      <groupId>dev.ujfe</groupId>
      <artifactId>ujfe-http</artifactId>
      <version>${ujfe.version}</version>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>exec-maven-plugin</artifactId>
        <version>3.1.0</version>
        <configuration>
          <mainClass>todo.Main</mainClass>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

### 2. Define the Todo model

**src/main/java/todo/Todo.java**

```java
package todo;

import java.util.Objects;
import java.util.UUID;

public final class Todo {
    private final String id;
    private final String text;
    private final boolean done;

    public Todo(String text) {
        this(UUID.randomUUID().toString(), text, false);
    }

    public Todo(String id, String text, boolean done) {
        this.id = Objects.requireNonNull(id);
        this.text = Objects.requireNonNull(text);
        this.done = done;
    }

    public String id()   { return id; }
    public String text() { return text; }
    public boolean done(){ return done; }

    public Todo withDone(boolean done) {
        return new Todo(id, text, done);
    }
}
```

### 3. Build the page

**src/main/java/todo/TodoPage.java**

```java
package todo;

import ujfe.core.Component;
import ujfe.core.Node;
import ujfe.router.Page;
import ujfe.signals.Signal;
import ujfe.signals.Signals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ujfe.core.UI.*;

@Page("/")
public final class TodoPage implements Component {

    // Server-side reactive state — all logic stays on the JVM
    private final Signal<List<Todo>> todos = Signals.signal(new ArrayList<>());
    private final Signal<String>     input = Signals.signal("");
    private final Signal<String>     filter = Signals.signal("all");

    @Override
    public Node render() {
        List<Todo> all      = todos.get();
        String     current  = filter.get();
        List<Todo> visible  = filterTodos(all, current);
        long       pending  = all.stream().filter(t -> !t.done()).count();

        return div()
            .css("min-h-screen bg-slate-100 flex items-start justify-center pt-16 font-sans")
            .child(
                div()
                    .css("w-full max-w-md flex flex-col gap-4")
                    // ── Title ──────────────────────────────────────────────
                    .child(h1("UJFE Todo")
                        .css("text-4xl font-black text-slate-800 tracking-tight"))
                    // ── Input row ──────────────────────────────────────────
                    .child(
                        div()
                            .css("flex gap-2")
                            .child(
                                inputText()
                                    .id("todo-input")
                                    .name("todo")
                                    .placeholder("What needs to be done?")
                                    .value(input::get)
                                    .css("flex-1 h-11 rounded-lg border border-slate-300 " +
                                         "bg-white px-4 text-sm focus:outline-none " +
                                         "focus:ring-2 focus:ring-indigo-500")
                                    .onInput(this::captureInput)
                            )
                            .child(
                                button("Add")
                                    .css("h-11 px-5 rounded-lg bg-indigo-600 hover:bg-indigo-700 " +
                                         "text-white font-semibold text-sm transition-colors")
                                    .onClick(this::addTodo)
                            )
                    )
                    // ── List ───────────────────────────────────────────────
                    .child(
                        div()
                            .css("rounded-lg border border-slate-200 bg-white shadow-sm " +
                                 "divide-y divide-slate-100 overflow-hidden")
                            .children(visible.isEmpty()
                                ? List.of(emptyState())
                                : visible.stream().map(this::todoRow).collect(Collectors.toList())
                            )
                    )
                    // ── Footer ─────────────────────────────────────────────
                    .child(footer(pending, current))
            );
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private Node todoRow(Todo todo) {
        return div()
            .css("flex items-center gap-3 px-4 py-3 " +
                 (todo.done() ? "bg-slate-50" : "bg-white"))
            .child(
                input()
                    .type("checkbox")
                    .css("w-4 h-4 rounded accent-indigo-600 cursor-pointer")
                    .checked(todo.done())
                    .onClick(() -> toggleTodo(todo.id()))
            )
            .child(
                span(todo.text())
                    .css("flex-1 text-sm " +
                         (todo.done() ? "line-through text-slate-400" : "text-slate-800"))
            )
            .child(
                button("✕")
                    .css("text-slate-300 hover:text-red-500 text-xs transition-colors")
                    .onClick(() -> removeTodo(todo.id()))
            );
    }

    private Node emptyState() {
        return div()
            .css("py-10 text-center text-sm text-slate-400")
            .child("No todos yet — add one above!");
    }

    private Node footer(long pending, String current) {
        return div()
            .css("flex items-center justify-between text-xs text-slate-500")
            .child(span(() -> pending + " item" + (pending == 1 ? "" : "s") + " left"))
            .child(
                div()
                    .css("flex gap-1")
                    .child(filterBtn("All",    "all",    current))
                    .child(filterBtn("Active", "active", current))
                    .child(filterBtn("Done",   "done",   current))
            )
            .child(
                button("Clear done")
                    .css("text-slate-400 hover:text-slate-700 transition-colors")
                    .onClick(this::clearDone)
            );
    }

    private Node filterBtn(String label, String value, String current) {
        boolean active = value.equals(current);
        return button(label)
            .css("px-2 py-1 rounded border text-xs transition-colors " +
                 (active ? "border-indigo-500 text-indigo-600 font-semibold"
                         : "border-transparent text-slate-500 hover:text-slate-700"))
            .onClick(() -> filter.set(value));
    }

    // ── Event handlers (run on the JVM) ────────────────────────────────────

    private void captureInput(String value) {
        input.set(value);
    }

    private void addTodo() {
        String text = input.get().trim();
        if (!text.isBlank()) {
            todos.update(list -> {
                List<Todo> next = new ArrayList<>(list);
                next.add(new Todo(text));
                return next;
            });
            input.set("");
        }
    }

    private void toggleTodo(String id) {
        todos.update(list -> {
            List<Todo> next = new ArrayList<>();
            for (Todo t : list) {
                next.add(t.id().equals(id) ? t.withDone(!t.done()) : t);
            }
            return next;
        });
    }

    private void removeTodo(String id) {
        todos.update(list -> {
            List<Todo> next = new ArrayList<>(list);
            next.removeIf(t -> t.id().equals(id));
            return next;
        });
    }

    private void clearDone() {
        todos.update(list -> {
            List<Todo> next = new ArrayList<>(list);
            next.removeIf(Todo::done);
            return next;
        });
    }

    private static List<Todo> filterTodos(List<Todo> todos, String filter) {
        if ("active".equals(filter)) {
            return todos.stream()
                .filter(todo -> !todo.done())
                .collect(Collectors.toList());
        }
        if ("done".equals(filter)) {
            return todos.stream()
                .filter(Todo::done)
                .collect(Collectors.toList());
        }
        return todos;
    }
}
```

> **Note on input capture**: `onInput(Consumer<String>)` receives the current control value on the server. Live form submit also sends URL-encoded successful controls as the event value, while a parsed submit-values API is still a follow-up.

### 4. Wire the server

**src/main/java/todo/Main.java**

```java
package todo;

import ujfe.http.UjfeServer;
import ujfe.http.UjfeServerConfig;
import ujfe.live.LiveSession;
import ujfe.live.LiveSessionConfig;
import ujfe.router.Router;
import ujfe.router.source.ManualRouteSource;

public final class Main {
    public static void main(String[] args) throws InterruptedException {

        // 1. Register routes — ManualRouteSource is AOT-friendly (no reflection)
        var routes = new ManualRouteSource()
            .register("/", TodoPage::new);

        var router = new Router().register(routes);

        // 2. Configure the live session (document metadata, CSS, security)
        var config = LiveSessionConfig.builder()
            .lang("en")
            .title("UJFE Todo")
            .devToolsEnabled(true)   // enables /_ujfe/dev.js in dev mode
            .build();

        var liveSession = new LiveSession(router, config);

        // 3. Start the Netty HTTP server
        var server = new UjfeServer(
            UjfeServerConfig.builder()
                .host("0.0.0.0")
                .port(8080)
                .build(),
            liveSession
        );

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();

        System.out.println("Todo app running → http://localhost:8080");
        server.blockUntilShutdown();
    }
}
```

### 5. Run it

```bash
./mvnw compile exec:java
```

Open [http://localhost:8080](http://localhost:8080). You should see the todo app.

---

## Path B — With Spring Boot

### 1. Generate the project

Go to [start.spring.io](https://start.spring.io) and configure:

| Field | Value |
|---|---|
| Project | Maven |
| Language | Java |
| Spring Boot | 4.x (latest) |
| Java | 17 (minimum for Spring Boot 4) |
| Artifact | todo-spring |
| Dependencies | Spring Web |

Download and extract the ZIP.

### 2. Add UJFE to pom.xml

In the generated `pom.xml`, add:

```xml
<properties>
    <ujfe.version>0.26.0-SNAPSHOT</ujfe.version>
</properties>

<!-- inside <dependencies> -->
<dependency>
    <groupId>dev.ujfe</groupId>
    <artifactId>ujfe-core</artifactId>
    <version>${ujfe.version}</version>
</dependency>
<dependency>
    <groupId>dev.ujfe</groupId>
    <artifactId>ujfe-signals</artifactId>
    <version>${ujfe.version}</version>
</dependency>
<dependency>
    <groupId>dev.ujfe</groupId>
    <artifactId>ujfe-router</artifactId>
    <version>${ujfe.version}</version>
</dependency>
<dependency>
    <groupId>dev.ujfe</groupId>
    <artifactId>ujfe-live</artifactId>
    <version>${ujfe.version}</version>
</dependency>
<!-- Spring MVC adapter — DO NOT add ujfe-http (that's the Netty server) -->
<dependency>
    <groupId>dev.ujfe</groupId>
    <artifactId>ujfe-spring</artifactId>
    <version>${ujfe.version}</version>
</dependency>
```

**Do not add `ujfe-http`** — Spring Boot already has Tomcat. `ujfe-spring` serves UJFE through Spring MVC's `DispatcherServlet` on the same port.

### 3. Copy Todo.java and TodoPage.java

Copy `todo/Todo.java` and `todo/TodoPage.java` from Path A into:

```
src/main/java/com/example/todospringdemo/
```

Change the package declaration at the top of each file:

```java
package com.example.todospringdemo;
```

### 4. Configure UJFE as a Spring bean

**src/main/java/com/example/todospringdemo/UjfeConfig.java**

```java
package com.example.todospringdemo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ujfe.live.LiveSessionConfig;
import ujfe.router.Router;
import ujfe.router.source.ManualRouteSource;

@Configuration
public class UjfeConfig {

    // When a Router bean exists, ujfe-spring auto-configures:
    //   LiveSession, UjfeSpringHandler, UjfeSpringHandlerMapping
    // The handler only claims routes registered here + /_ujfe/* endpoints.

    @Bean
    Router ujfeRouter() {
        return new Router().register(
            new ManualRouteSource().register("/", TodoPage::new)
        );
    }

    @Bean
    LiveSessionConfig ujfeLiveSessionConfig() {
        return LiveSessionConfig.builder()
            .lang("en")
            .title("UJFE Todo — Spring")
            .devToolsEnabled(true)
            .build();
    }
}
```

That is all the Spring wiring. `ujfe-spring` auto-configuration picks up the `Router` and `LiveSessionConfig` beans and registers a `HandlerMapping` that claims only UJFE routes.

### 5. Enable virtual threads (optional but recommended)

**src/main/resources/application.yml**

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

### 6. Run it

```bash
./mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080).

Spring MVC controllers, actuator endpoints, and static resources continue to work on the same port. UJFE only claims registered UJFE routes and `/_ujfe/*`.

---

## How It Works

```
Browser                    JVM (UJFE)
───────                    ──────────

GET /                 →    Router resolves TodoPage
                           LiveSession.renderDocument("/", ...)
                           TodoPage.render() → Node tree
                           Node tree → HTML string
                      ←    Full HTML document

DOMContentLoaded      →    POST /_ujfe/state  (cookies + localStorage)
                           LiveSession merges ClientState
                           TodoPage.render() → updated HTML
                      ←    { "html": "...", "css": "..." }
                           client.js → replaces #ujfe-root

click "Add"           →    POST /_ujfe/event  { eventId: "evt-xxx" }
                           LiveEventRegistry.handle("evt-xxx")
                           addTodo() updates todos signal
                           TodoPage.render() → new HTML
                      ←    { "html": "...", "css": "..." }
                           client.js → replaces #ujfe-root
```

All state (`todos`, `input`, `filter`) lives in the `TodoPage` instance on the server. The browser sends events; the server runs handlers and re-renders.

---

## Key Concepts

### Signals

```java
Signal<List<Todo>> todos = Signals.signal(new ArrayList<>());

// Read (inside render — tracked for re-render)
List<Todo> current = todos.get();

// Write (inside event handler — triggers re-render on next request)
todos.update(list -> {
    List<Todo> next = new ArrayList<>(list);
    next.add(new Todo(text));
    return next;
});
```

### Event handlers

```java
button("Add").onClick(this::addTodo)  // server-side Java method
button("✕").onClick(() -> removeTodo(todo.id()))
```

Event ids are opaque UUIDs. No JavaScript method names are exposed.

### CSS

```java
div().css("flex gap-2 p-4")  // server-side utility CSS collection
```

UJFE collects classes used during render and generates a minimal stylesheet. You can switch to Tailwind or any external CSS by setting `CssMode.EXTERNAL`.

### Route registration

```java
// Manual (preferred — no reflection, AOT-friendly)
new ManualRouteSource().register("/", TodoPage::new)

// Reflection-based (development convenience)
ReflectionPageScanner.forPackages("todo")
```

---

## Adding More Routes

```java
// In your router configuration
new ManualRouteSource()
    .register("/",      TodoPage::new)
    .register("/about", AboutPage::new)
```

Each route gets a page instance inside the active `LiveSession`. Stable re-renders within that session reuse the existing instance.

---

## Next Steps

- Read `docs/signals.md` for the full reactive model including computed values.
- Read `docs/lifecycle.md` to open/close server resources with `onMount`/`onUnmount`.
- Read `docs/runtime-extension-points.md` to add observability, auth, and SEO head nodes.
- Read `docs/security/` for the full security model.
- Use `ujfe convert page.html --out Page.java --type html` to convert existing HTML prototypes into the Java DSL.

---

## Troubleshooting

**Nothing happens when I click a button**
→ Check that `/_ujfe/client.js` loads (Network tab). Make sure `/_ujfe/*` is mapped in your servlet config.

**Re-renders show old state**
→ State lives in the page instance. In Spring, if multiple users share one `LiveSession` bean they share state. Create a `LiveSession` per user/session for real applications.

**CSS classes are not applied**
→ In `CssMode.INTERNAL` (default), UJFE generates only classes it sees during render. Add them to a `div().css(...)` call to register them.

**Spring route conflicts**
→ `UjfeSpringHandlerMapping` only claims registered UJFE `GET` routes and `/_ujfe/*`. Spring MVC controllers, REST APIs, actuator endpoints, static resources, and missing non-UJFE routes keep normal Spring ownership.
