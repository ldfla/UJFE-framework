# Administrative Primitives

UJFE includes Java-first primitives for common administrative screens. They are
generic framework building blocks, not product-specific application logic.

These APIs render normal HTML and annotate behavior with `data-ujfe-*`
contracts. Browser behavior that needs JavaScript should be implemented as
external modules loaded with `UI.clientModule(...)`, keeping restrictive CSP
policies viable.

## Routes

Use `UI.route(...)` instead of hand-built strings:

```java
String href = UI.route("/flows/{id}/versions/{version}")
        .with("id", flowId)
        .with("version", version)
        .query("tab", "review")
        .continueUrl("/dashboard")
        .build();
```

Route parameters are required, path segments are encoded, query params are
encoded, and continue URLs must be application-relative paths.

## Actions And Forms

Use `UI.actionButton(...)` for destructive or critical operations:

```java
UI.actionButton("Rollback", "/flows/42/rollback")
        .method("delete")
        .csrf(csrfToken)
        .confirm("Rollback this deployment?")
        .loadingText("Rolling back")
        .refresh("/flows/42");
```

Use `UI.actionForm(...)` for server actions with standard field markup:

```java
UI.actionForm("/users")
        .csrf(csrfToken)
        .statusTarget("#user-status")
        .child(UI.field("email").label("Email").text().required(true))
        .child(UI.field("role").label("Role").select()
                .option("admin", "Admin")
                .option("operator", "Operator"));
```

## Data Grids

`UI.dataGrid(rows)` renders dense server-driven tables with sortable column
metadata, selection, row actions, empty state, and pagination metadata:

```java
UI.dataGrid(users)
        .caption("Users")
        .rowKey("userId", UserView::id)
        .selectable(true)
        .sortableTextColumn("email", "Email", UserView::email)
        .column("Status", user -> UI.badge(user.status(), user.status()))
        .rowAction("Open", user -> UI.route("/users/{id}")
                .with("id", user.id())
                .build())
        .pagination(page, pageSize, total);
```

## Object Editors

Use `ObjectSchema` and `UI.objectEditor(...)` for typed configuration forms
instead of raw JSON textareas:

```java
ObjectSchema schema = UI.objectSchema().title("Node config");
schema.field("prompt", "Prompt", ObjectSchema.FieldType.TEXT).required(true);
schema.field("timeout", "Timeout", ObjectSchema.FieldType.NUMBER).defaultValue(20);
schema.field("enabled", "Enabled", ObjectSchema.FieldType.BOOLEAN).defaultValue(true);

UI.objectEditor(schema)
        .values(currentValues)
        .error("prompt", "Prompt is required")
        .jsonPreview(true);
```

## Realtime, Browser Modules, And Media

Declare realtime subscriptions and external browser modules without inline
scripts:

```java
UI.realtime("/events/dashboard")
        .onEvent(DashboardEvent.class)
        .heartbeat(Duration.ofSeconds(10))
        .pollingFallback(Duration.ofSeconds(15));

UI.clientModule("passkey-login")
        .src("/assets/security/passkey-login.js")
        .action("authenticate", PasskeyRequest.class)
        .target("#login-card")
        .errorTarget("#login-errors");

UI.browserBridge("passkey-login")
        .moduleSrc("/assets/security/passkey-login.js")
        .api("navigator.credentials.get")
        .action("authenticate", PasskeyRequest.class, PasskeyResponse.class)
        .trigger("#passkey-button")
        .errorTarget("#login-errors");

UI.webAuthnLogin("/login/passkey/challenge", "/login/passkey/finish")
        .trigger("#passkey-button")
        .userVerification("required")
        .timeout(Duration.ofSeconds(45));
```

For recordings:

```java
UI.audioPlayer(recordingUrl)
        .title("Call recording")
        .duration("02:18")
        .waveform(samples)
        .metadata("Duration", duration)
        .downloadAllowed(canDownload);
```

## Dashboard Primitives

Use dashboard primitives for consistent operational views:

```java
UI.metricTile("Active calls", "12", "Live", "ok");
UI.statusIndicator("Asterisk", "ok");
UI.sparkline(List.of(4, 8, 6, 12)).label("Calls");

UI.alertList()
        .title("Operational alerts")
        .alert("warn", "High latency", "/alerts/42");

UI.healthPanel()
        .title("Systems")
        .item("Asterisk", "ok", "Connected")
        .item("ARI", "warn", "Retrying");
```

## Graphs And Workflows

`UI.graphBuilder(...)` renders an accessible graph snapshot and enhancement
metadata for future pan/zoom, selection, ordering, minimap, and keyboard
behavior:

```java
UI.graphBuilder("ivr-flow")
        .node("start", "Start", "entry", 80, 80)
        .node("menu", "Menu", "ivr", 240, 80)
        .edge("start", "menu", "next", 1)
        .minimap(true);
```

Use workflow primitives for publish/deploy/rollback states:

```java
UI.workflowTimeline()
        .title("Deploy")
        .currentStatus("running")
        .step("Published", "done")
        .step("Asterisk deploy", "running", "Applying configuration");

UI.workflowBanner("failed", "Rollback required");
UI.diffSummary("Changes", 1, 2, 0);
```

## Render Modes

`RenderMode` stores route-level cache intent:

```java
RouteDefinition route = new RouteDefinition("/dashboard", DashboardPage::new)
        .withRenderMode(RenderMode.staticShell(Duration.ofSeconds(30)));

RenderSnapshot snapshot = RenderSnapshot.of(
        "/dashboard",
        UI.main().child("Dashboard"),
        RenderMode.staticShell(Duration.ofSeconds(30)).revalidateOn("dashboard.updated")
);
```

Security guidance:

- Use `RenderMode.dynamic()` or `CacheScope.NO_STORE` for private user data,
  permissions, realtime call state, recordings, and sensitive operational data.
- Use `RenderMode.staticPage(...)` only for public or non-sensitive content.
- Use `RenderMode.staticShell(...)` for authenticated shells where widgets load
  protected data separately.
- Use `revalidateOn(...)` to record the application event that should invalidate
  cached output.

## Migration Guide

- Replace string-concatenated links with `UI.route(...)`.
- Replace repeated POST button markup with `UI.actionButton(...)`.
- Replace repeated labels, hints, and server error markup with `UI.field(...)`.
- Replace card-based lists with `UI.dataGrid(...)` when rows need scanning,
  sorting, selection, or row actions.
- Add grid filters with `.filter(...)` and responsive columns with
  `.responsiveTextColumn(...)`.
- Replace raw JSON configuration fields with `UI.objectEditor(...)`.
- Replace per-page realtime scripts with `UI.realtime(...)` plus one external
  module responsible for SSE/WebSocket/polling.
- Replace hand-written WebAuthn glue with `UI.webAuthnLogin(...)`,
  `UI.webAuthnRegistration(...)`, `Base64Url`, and an external module.
- Move cohesive pages/components to `UI.uiComponent(...)` with `UI.scopedStyle(...)`,
  `UI.contract(...)`, and `UI.clientModule(...)`.
- Wrap route/component data loading with `UI.loader(...)` and render partial
  failures through `UI.loadBoundary(...)`.
- Use `UI.componentPreview(...)` and `HtmlSnapshot.capture(...)` for
  storybook-like previews, snapshot fingerprints, and CSP checks.
- Keep business rules, authorization, auditing, ARI integration, persistence,
  and domain validation in the consuming application.

## Component Model And Contracts

Components can colocate props metadata, external scoped CSS, contracts, client
modules, and markup:

```java
var style = UI.scopedStyle("login-card")
        .href("/assets/login-card.css")
        .token("gap", "space.3");

UI.uiComponent("login-card")
        .props(new LoginProps(continueUrl))
        .style(style)
        .contract(UI.contract("login-card").payload(LoginProps.class))
        .clientModule(UI.clientModule("login-card").src("/assets/login-card.js"))
        .css(style.className("root"))
        .child(renderLoginForm());
```

`UI.contract(...)` uses reflection metadata from Java classes. It is intended as
runtime/tooling metadata today and can be backed by annotation processors later.

## Data Loaders And Boundaries

Use `UI.loader(...)` to keep blocking Java loaders simple while still supporting
timeouts, fallbacks, parallel tasks, and virtual threads when the running JDK
provides them:

```java
LoadResult<Map<String, Object>> result = UI.loader(Map.class)
        .virtualThreadsWhenAvailable()
        .timeout(Duration.ofSeconds(2))
        .parallel("ari", ariClient::status)
        .parallel("calls", callService::activeCalls)
        .parallel("alerts", alertService::recent)
        .loadMap();

UI.loadBoundary(result)
        .success(data -> renderDashboard(data))
        .timeout(UI.workflowBanner("timeout", "Dashboard data timed out"))
        .error(UI.workflowBanner("failed", "Dashboard data failed"));
```
