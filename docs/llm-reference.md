# UJFE LLM Reference

This file is the complete app-facing reference a code-generating LLM should
read before writing UJFE code. It describes what UJFE is, which modules exist,
which public APIs to prefer, how CSS works, how assets are referenced, how
runtime adapters behave, and which framework rules must not be guessed.

It covers framework APIs intended for application code, adapters, integrations,
and tests. Package-private implementation details, private helpers, and
framework-internal algorithms are intentionally excluded.

## Framework Identity

UJFE is a Java-first, HTML-first, server-first frontend framework for JVM
applications.

Use UJFE when the application should:

- render real HTML on the server;
- write UI in Java with `Node`, `Element`, and `UI` helpers;
- connect browser events to Java callbacks through the live runtime;
- keep application state and handlers on the JVM by default;
- use normal CSS, Tailwind, Bootstrap, design systems, or the optional UJFE
  internal utility renderer;
- run standalone through Netty, Jakarta Servlet, or Spring MVC.

Do not describe UJFE as a browser-side JavaScript framework clone, a mandatory
CSS framework, a proprietary HTML dialect, or a Spring replacement.

## Module Map

Use these modules deliberately:

| Module | Use |
| --- | --- |
| `ujfe-core` | HTML DSL, `Node`, `Element`, `UI`, escaping, URL policy, utility CSS, client state, REST client, reusable primitives. |
| `ujfe-signals` | `Signal<T>`, `Computed<T>`, subscriptions, derived state. |
| `ujfe-router` | `@Page`, `Router`, `RouteDefinition`, route sources, page rendering. |
| `ujfe-live` | `LiveSession`, event registry, runtime config, internal endpoints, client script, dev script. |
| `ujfe-http` | Standalone Netty server. Do not use inside Spring Boot unless a separate server is intentional. |
| `ujfe-servlet` | Jakarta Servlet runtime for Tomcat/Jakarta containers without Spring. |
| `ujfe-spring` | Spring Boot/MVC integration through the normal DispatcherServlet pipeline. |
| `ujfe-cli` | HTML-to-UJFE conversion command. |

## Reference Scope And Imports

For normal pages, use these imports:

```java
import ujfe.core.Node;
import ujfe.core.Component;
import ujfe.core.Lifecycle;
import ujfe.live.LiveSession;
import ujfe.live.LiveSessionConfig;
import ujfe.router.Router;
import ujfe.router.Page;

import static ujfe.core.UI.*;
```

Add module-specific imports only when using that feature:

```java
import ujfe.core.RenderMode;
import ujfe.live.CssMode;
import ujfe.signals.Signal;
import ujfe.signals.Signals;
import ujfe.validation.ValidationMode;
import ujfe.runtime.action.RuntimeActionRegistry;
```

LLMs should prefer framework helpers over string-built HTML. Use raw HTML only
through `unsafeHtml(...)`, and only for trusted, pre-sanitized fragments.

## Minimal Standalone Application

```java
import ujfe.core.Node;
import ujfe.http.UjfeServer;
import ujfe.http.UjfeServerConfig;
import ujfe.live.LiveSession;
import ujfe.live.LiveSessionConfig;
import ujfe.router.Router;

import static ujfe.core.UI.*;

public final class Main {
    public static void main(String[] args) throws InterruptedException {
        var router = new Router().register("/", HomePage::new);

        var config = LiveSessionConfig.builder()
                .title("UJFE App")
                .lang("en")
                .build();

        var server = new UjfeServer(
                UjfeServerConfig.builder()
                        .host("0.0.0.0")
                        .port(8080)
                        .build(),
                new LiveSession(router, config)
        );

        server.start();
        server.blockUntilShutdown();
    }

    public static final class HomePage {
        public Node render() {
            return main()
                    .child(h1("Hello UJFE"))
                    .child(p("Server-rendered Java UI."));
        }
    }
}
```

## Page And Routing Rules

Valid page objects expose a public render method returning `Node` or compatible
renderable content.

Use `Router` in one of these forms:

```java
new Router().register("/", HomePage::new);
new Router().register(new HomePage());       // annotated instance
new Router().register(HomePage.class);       // annotated class
new Router().register(routeSource);          // RouteSource
```

Use `@Page` for annotated pages:

```java
@Page("/dashboard")
public final class DashboardPage {
    public Node render() {
        return main().child(h1("Dashboard"));
    }
}
```

Use `ManualRouteSource` for explicit generated or hand-authored route lists:

```java
var routes = new ManualRouteSource()
        .register("/", HomePage::new)
        .register("/settings", SettingsPage::new);

var router = new Router().register(routes);
```

Use `RouteDefinition` when route metadata is needed:

```java
router.register(new RouteDefinition("/", HomePage::new)
        .withRenderMode(RenderMode.staticShell(Duration.ofSeconds(30))));
```

Paths are normalized. Duplicate route paths fail at registration.

## Core HTML DSL

Always prefer:

```java
import static ujfe.core.UI.*;
```

The DSL renders standard HTML. Text is escaped by default.

### Node Creation

```java
text("safe text")
text(() -> dynamicText)
unsafeHtml("<strong>trusted only</strong>")
component(componentInstance)
element("future-html-element")
Element.of("dialog")
Element.custom("my-card")
Element.svg("path")
Element.mathMl("mi")
```

Use `unsafeHtml(...)` only for trusted, pre-sanitized HTML. Never pass user
input, request data, Markdown output, or untrusted CMS content to it.

### Element Methods

All `Element` helpers support the fluent base API:

```java
div()
    .id("panel")
    .css("flex flex-col gap-4")
    .attr("data-kind", "summary")
    .attr("hidden", false)
    .boolAttr("required", true)
    .aria("live", "polite")
    .ariaLabel("Status")
    .data("state", "ready")
    .role("status")
    .child(h2("Title"))
    .child("safe text")
    .children(p("A"), p("B"));
```

Common convenience methods include:

- global: `id`, `title`, `lang`, `dir`, `role`, `tabindex`,
  `accessKey`, `contentEditable`, `draggable`, `hidden`, `open`,
  `spellcheck`, `translate`, `slot`, `part`, `aria`, `ariaLabel`, `data`;
- forms: `type`, `name`, `value`, `placeholder`, `action`, `method`,
  `forId`, `checked`, `selected`, `disabled`, `enabled`, `required`,
  `readonly`, `multiple`, `autofocus`, `min`, `max`, `step`, `rows`,
  `cols`, `size`, `maxlength`, `minlength`, `autocomplete`, `inputMode`,
  `pattern`;
- assets/media: `src`, `href`, `alt`, `width`, `height`, `controls`,
  `autoplay`, `loop`, `muted`, `playsInline`, `preload`, `poster`,
  `crossorigin`, `referrerPolicy`, `loading`;
- modern interaction: `popover(String)`, `popover(boolean)`.

Complete `Element` app-facing API:

```java
Element.of(String tagName)
Element.html(String tagName)
Element.custom(String tagName)
Element.svg(String tagName)
Element.mathMl(String tagName)

css(String classes)
attr(String name, String value)
attr(String name, boolean enabled)
attr(String name, BooleanSupplier enabledSupplier)
attr(String name, Supplier<String> valueSupplier)
boolAttr(String name, boolean enabled)
boolAttr(String name, BooleanSupplier enabledSupplier)
boolAttr(String name, Supplier<Boolean> enabledSupplier)
child(Node child)
child(String text)
child(Supplier<String> textSupplier)
children(Node... nodes)
children(Collection<? extends Node> nodes)

onClick(Runnable handler)
onInput(Runnable handler)
onInput(Consumer<String> handler)
onChange(Runnable handler)
onChange(Consumer<String> handler)
onSubmit(Runnable handler)
on(String eventName, Runnable handler)

id(String), title(String), lang(String), dir(String), role(String)
tabindex(int), accessKey(String), contentEditable(boolean)
draggable(boolean), hidden(boolean), open(boolean)
spellcheck(boolean), translate(boolean), slot(String), part(String)
aria(String name, String value), ariaLabel(String label)
data(String name, String value)

type(String), name(String), value(String), value(Supplier<String>)
placeholder(String), label(String), action(String), method(String)
forId(String), checked(boolean), checked(Supplier<Boolean>)
selected(boolean), selected(Supplier<Boolean>), disabled(boolean)
enabled(boolean), required(boolean), readonly(boolean), multiple(boolean)
autofocus(boolean), min(String), max(String), step(String), rows(int)
cols(int), size(int), maxlength(int), minlength(int)
autocomplete(String), inputMode(String), pattern(String)

src(String), href(String), alt(String), width(int), height(int)
controls(boolean), autoplay(boolean), loop(boolean), muted(boolean)
playsInline(boolean), preload(String), poster(String), crossorigin(String)
referrerPolicy(String), loading(String)

popover(String), popover(boolean)
tagName(), namespace(), render(UjfeContext)
```

Attribute and URL rules:

- `attr(name, null)` removes a normal attribute.
- `attr(name, false)` and `boolAttr(name, false)` remove a boolean attribute.
- Text children are escaped.
- Attribute values are escaped.
- Attribute names are validated and inline event attributes such as `onclick`
  are rejected.
- URL-bearing attributes (`href`, `src`, `action`, `poster`, `formaction`,
  `cite`, `data`, `background`) go through `SafeUrl.sanitize(...)`.
- HTML void elements such as `img`, `input`, `br`, `hr`, `meta`, and `link`
  cannot have children.

### HTML Helper Groups

Document and metadata:

```java
html(), head(), body(), title(), title("Title"), meta(), link(), style(),
script(), base()
```

Layout and semantic structure:

```java
div(), header(), main(), aside(), section(), article(), nav(), footer(),
address(), address("Text"), figure(), figcaption(), details(), summary(),
dialog(), modal()
```

Text:

```java
h1()..h6(), h1("Text")..h6("Text"), p(), p("Text"), p(Supplier<String>),
span(), span("Text"), strong(), strong("Text"), em(), em("Text"), b(), i(),
u(), small(), mark(), abbr(), cite(), pre(), code(), code("Text"),
blockquote(), q(), br(), hr()
```

Links, lists, tables:

```java
a(), a("Text"), ul(), ol(), li(), le(), dl(), dt(), dt("Term"), dd(),
dd("Description"), table(), thead(), tbody(), tfoot(), tr(), td(), td("Text"),
th(), th("Text"), caption(), caption("Text"), colgroup(), col()
```

Media and embedded content:

```java
img(), picture(), source(), track(), audio(), video(), canvas(), svg(), math(),
map(), area(), iframe(), object(), embed(), param()
```

Forms:

```java
form(), form(Node...), label(), label("Text"), input(), input("type"),
inputText(), inputNumber(), inputPassword(), inputEmail(), inputSearch(),
inputTel(), inputUrl(), inputHidden(), inputDate(), inputTime(),
inputDateTimeLocal(), inputMonth(), inputWeek(), inputColor(), inputFile(),
inputRange(), inputButton(), inputImage(), inputSubmit(), inputReset(),
checkbox(), radio(), button(), button("Text"), select(), option(),
option("Text"), optgroup(), textarea(), textarea("Text"), fieldset(),
legend(), legend("Text"), datalist(), output(), output("Text"), progress(),
meter()
```

Templates and Web Components:

```java
template(), slot(), element("my-custom-element"), Element.custom("my-card")
```

Unknown standards tags should be rendered with `Element.of(...)` or
`element(...)`, not by adding one-off framework assumptions.

### UI Factory Catalog

`UI` exposes both HTML factories and higher-level generic primitives. The
complete app-facing factory list is:

```java
text(String)
text(Supplier<String>)
unsafeHtml(String)
component(Component)
route(String)

actionButton(String label, String actionUrl)
actionButton(String label, Runnable handler)
actionForm(String actionUrl)
dataGrid(Collection<T> rows)
field(String name)
objectSchema()
objectEditor(ObjectSchema schema)
audioPlayer(String sourceUrl)
realtime(String endpoint)
clientModule(String name)
browserBridge(String name)
webAuthnLogin(String challengeUrl, String responseUrl)
webAuthnRegistration(String challengeUrl, String responseUrl)
contract(String name)
scopedStyle(String componentId)
uiComponent(String componentId)
loader(Class<T> type)
loadBoundary(LoadResult<T> result)
sparkline(List<Integer> values)
alertList()
healthPanel()
componentPreview(String title)
graphBuilder(String id)
workflowTimeline()

badge(String label)
badge(String label, String tone)
statusIndicator(String label, String status)
metricTile(String label, String value)
metricTile(String label, String value, String hint, String tone)
toolbar(Node... children)
sectionHeader(String title, String description)
splitView(Node primary, Node secondary)
toast(String message, String tone)
modalDialog(String title, Node body)
workflowBanner(String status, String message)
diffSummary(String title, int added, int changed, int removed)
```

High-level primitives render ordinary HTML plus `data-ujfe-*` metadata. They do
not imply a single product domain and do not include hidden browser behavior
unless an external application-owned module enhances them.

## Live Events

Use live event helpers for server-side callbacks:

```java
button("Save").onClick(this::save);
inputText().onInput(value -> name.set(value));
select().onChange(value -> profile.set(value));
form().onSubmit(this::submit);
div().on("click", this::genericHandler);
```

Available event methods:

- `onClick(Runnable)`;
- `onInput(Runnable)` and `onInput(Consumer<String>)`;
- `onChange(Runnable)` and `onChange(Consumer<String>)`;
- `onSubmit(Runnable)`;
- `on(String eventName, Runnable)`.

Live event HTTP endpoints are internal:

| Path | Method | Purpose |
| --- | --- | --- |
| `/_ujfe/event` | `POST` | Dispatch browser events to Java handlers. |
| `/_ujfe/state` | `POST` | Synchronize allowed browser state. |
| `/_ujfe/css` | `GET` | Render requested internal utility CSS. |
| `/_ujfe/client.js` | `GET` | Browser event bridge. |
| `/_ujfe/dev.js` | `GET` | Optional development preview script. |

Application code normally does not call these endpoints manually.

## Client State

Browser cookies, local storage, and session storage are denied by default.
Expose only explicit keys:

```java
var config = LiveSessionConfig.builder()
        .allowClientCookie("ujfe_demo")
        .allowLocalStorageKey("ujfe.theme")
        .allowSessionStorageKey("ujfe.tab")
        .build();
```

Read state from render/event code:

```java
Ujfe.cookie("ujfe_demo").orElse("missing");
Ujfe.localStorage("ujfe.theme").orElse("light");
Ujfe.sessionStorage("ujfe.tab").orElse("home");
```

Never expose broad browser storage by default.

## Signals

Use `ujfe-signals` for mutable and derived live server state:

```java
Signal<Integer> count = Signals.signal(0);
Computed<Integer> doubled = Signals.computed(() -> count.get() * 2);

button("+").onClick(() -> count.update(value -> value + 1));
p(() -> "Count: " + count.get());
p(() -> "Doubled: " + doubled.get());
```

Signals are useful for page-local state, form drafts, toggles, computed totals,
and derived UI state. Use normal Java values for static labels and constants.

## CSS Model

UJFE is CSS agnostic. It always preserves normal `class` attributes.

### CSS Modes

```java
LiveSessionConfig.builder().cssMode(CssMode.INTERNAL).build();
LiveSessionConfig.builder()
        .cssMode(CssMode.EXTERNAL)
        .externalStylesheet("/assets/app.css")
        .build();
LiveSessionConfig.builder().cssMode(CssMode.NONE).build();
```

| Mode | Behavior |
| --- | --- |
| `INTERNAL` | Collect classes during render and emit generated utility CSS. |
| `EXTERNAL` | Emit configured stylesheet links and no UJFE generated CSS. |
| `NONE` | Emit no UJFE CSS and no configured external stylesheets. |

Use `INTERNAL` for simple server-rendered demos and built-in utilities. Use
`EXTERNAL` for Tailwind, Bootstrap, design systems, CSS Modules, Sass, or any
application-owned asset pipeline. Use `NONE` for tests or fully custom output.

### Internal Utility CSS

Internal mode supports a Tailwind-like subset:

- layout: `grid`, `flex`, `inline-flex`, `flex-col`, `flex-wrap`,
  `items-*`, `justify-*`, `grid-cols-1..4`, `col-span-1..3`;
- app grids: `app-shell`, `demo-grid`, `docs-grid`, `catalog-grid`;
- spacing: `p-*`, `px-*`, `py-*`, `pt-*`, `pr-*`, `pb-*`, `pl-*`,
  `m-*`, `mx-*`, `my-*`, `mt-*`, `mr-*`, `mb-*`, `ml-*`, `gap-*`,
  `gap-x-*`, `gap-y-*`, including numeric scales and `px`;
- sizing/position: `min-h-screen`, `w-full`, `min-w-0`, `max-w-*`,
  `h-*`, `w-64`, `relative`, `absolute`, `fixed`, `sticky`, `inset-0`,
  `top-*`, `right-*`, `bottom-*`, `z-40`, `z-50`;
- typography: `text-xs` through `text-5xl`, `font-*`, `font-sans`,
  `font-mono`, `uppercase`, `italic`, `leading-none`, `leading-relaxed`;
- borders/effects: `rounded`, `rounded-md`, `rounded-lg`, `rounded-full`,
  `border`, `border-t`, `border-b`, `shadow-sm`, `shadow-inner`, `ring-*`;
- colors: built-in slate/zinc/white/black/emerald/indigo/amber/rose/blue
  classes plus theme-driven `primary` and `secondary` palettes;
- dynamic theme colors: `bg-primary-50`, `bg-primary-500`,
  `text-secondary-700`, `border-primary-200`, etc.;
- variants: `sm:`, `md:`, `lg:`, `hover:`, `focus:`, `active:`,
  `placeholder:`.

Unknown classes remain in HTML but do not generate internal CSS. This is
intentional so external CSS can coexist with UJFE.

### Theme

```java
CssTheme theme = CssTheme.of("#2563eb", "#10b981");

LiveSessionConfig.builder()
        .themeSupplier(() -> theme)
        .build();
```

The theme generates CSS variables for primary and secondary color steps:
`50,100,200,300,400,500,600,700,800,900,950`.

## Static Assets, JavaScript, Images, And Media

UJFE renders references to static assets. It does not serve arbitrary files from
the filesystem.

Use normal HTML:

```java
link().attr("rel", "stylesheet").href("/assets/app.css");
script().src("/assets/app.js").attr("defer", true);
img().src("/assets/logo.svg").alt("Logo");
picture()
        .child(source().attr("media", "(min-width: 960px)")
                .src("/assets/hero-wide.webp"))
        .child(img().src("/assets/hero.webp").alt("Dashboard"));
video().src("/assets/demo.mp4").poster("/assets/poster.webp")
        .controls(true).preload("metadata");
audio().controls(true)
        .child(source().src("/assets/audio.mp3").type("audio/mpeg"));
track().attr("kind", "captions").attr("srclang", "en")
        .src("/assets/captions.vtt");
canvas().width(640).height(320).ariaLabel("Chart");
iframe().src("/embedded").loading("lazy");
object().attr("data", "/assets/report.pdf").type("application/pdf");
embed().src("/assets/preview.pdf").type("application/pdf");
```

Asset serving ownership:

- Netty UJFE runtime returns safe text 404s for asset-like misses. It is not a
  general static file server.
- Servlet deployments should let the container, reverse proxy, or a dedicated
  static file mapping serve assets.
- Spring MVC deployments should use normal Spring static resource handling for
  `/assets/**`, `/static/**`, `/webjars/**`, etc.
- CDN or reverse proxy hosting is recommended for public production assets.

Do not register UJFE page routes for static files such as `/assets/app.css`.
Do not assume static assets shadow internal endpoints. `/_ujfe/*` remains UJFE
runtime-owned.

### Application JavaScript

For ordinary JavaScript files, render a script tag:

```java
LiveSessionConfig.builder()
        .head(script().src("/assets/app.js").attr("defer", true))
        .build();
```

For metadata-driven client modules:

```java
clientModule("charts")
        .src("/assets/charts.js")
        .action("render", ChartRequest.class)
        .target("#chart")
        .errorTarget("#chart-error")
        .defer(true);
```

For browser API bridges:

```java
browserBridge("clipboard")
        .moduleSrc("/assets/clipboard.js")
        .api("navigator.clipboard")
        .action("copy", CopyRequest.class, CopyResult.class)
        .trigger("[data-copy]")
        .resultTarget("#copy-result")
        .errorTarget("#copy-error");
```

These primitives render declarative metadata. The application-owned JavaScript
module must implement the browser behavior.

## Live Runtime Configuration

Important `LiveSessionConfig.Builder` methods:

```java
themeSupplier(Supplier<CssTheme>)
cssMode(CssMode)
externalStylesheet(String)
devToolsEnabled(boolean)
lang(String)
title(String)
head(Node...)
runtimeActions(RuntimeActionRegistry)
observability(ObservabilityConfig)
traceSink(TraceSink)
securityHeaders(SecurityHeadersConfig)
securityHeader(String, String)
disableSecurityHeaders()
clientStatePolicy(ClientStatePolicy)
allowClientCookie(String)
allowClientCookies(Collection<String>)
allowLocalStorageKey(String)
allowLocalStorageKeys(Collection<String>)
allowSessionStorageKey(String)
allowSessionStorageKeys(Collection<String>)
internalEndpointRateLimitingEnabled(boolean)
internalEndpointRateLimit(int capacity, int refillTokens, Duration refillPeriod)
trustedProxy(String)
trustedProxies(Collection<String>)
validationOptions(ValidationOptions)
validationMode(ValidationMode)
accessibilityValidationEnabled(boolean)
seoValidationEnabled(boolean)
canonicalLinkValidationEnabled(boolean)
openGraphValidationEnabled(boolean)
htmlLangValidationEnabled(boolean)
disableValidationRule(String)
strictValidationRule(String)
```

Unsafe development-only methods:

```java
disableCsrfProtectionForDevelopmentUnsafe()
enableDevelopmentErrorDetailsUnsafe()
```

Do not use those in production.

## Runtime Adapters

### Standalone Netty

```java
var server = new UjfeServer(
        UjfeServerConfig.builder()
                .host("0.0.0.0")
                .port(8080)
                .maxJsonPayloadBytes(262_144)
                .build(),
        liveSession
);
```

### Jakarta Servlet

Use `UjfeServlet` for non-Spring Jakarta containers. Prefer mapping it only to
UJFE routes and `/_ujfe/*`, leaving static assets to the container or proxy.

```java
new UjfeServlet(router, liveSession, 262_144);
```

### Spring MVC

Use `ujfe-spring` in Spring Boot/MVC apps. It coexists with controllers,
actuator endpoints, static resources, and Spring Security filters.

```java
@Bean
Router ujfeRouter(HomePage homePage) {
    return new Router().register(homePage);
}

@Bean
LiveSessionConfig ujfeLiveSessionConfig() {
    return LiveSessionConfig.builder()
            .title("Spring UJFE")
            .build();
}
```

Do not add `ujfe-http` to a Spring Boot app unless a second Netty server is
intentional.

## Cache And Published Performance

UJFE internal assets emit cache headers in every adapter:

| Resource | Cache behavior |
| --- | --- |
| `/_ujfe/client.js` | `public, max-age=300, must-revalidate` plus `ETag`. |
| `/_ujfe/dev.js` | `no-cache` plus `ETag`. |
| `/_ujfe/css` | `private, no-cache` plus content `ETag`. |

Adapters return `304 Not Modified` when `If-None-Match` matches.

Page response cache is route-level:

```java
new RouteDefinition("/", HomePage::new)
        .withRenderMode(RenderMode.dynamic());              // no-store

new RouteDefinition("/public", PublicPage::new)
        .withRenderMode(RenderMode.staticPage(Duration.ofMinutes(10)));

new RouteDefinition("/profile", ProfilePage::new)
        .withRenderMode(RenderMode.cached(Duration.ofSeconds(30)));

new RouteDefinition("/app", AppShellPage::new)
        .withRenderMode(RenderMode.staticShell(Duration.ofSeconds(30)));

new RouteDefinition("/feed", FeedPage::new)
        .withRenderMode(RenderMode
                .staleWhileRevalidate(Duration.ofSeconds(60), Duration.ofMinutes(5))
                .revalidateOn("feed.updated"));
```

Use `staticPage` only for public, non-user-specific HTML. Use `staticShell` or
`cached` for private browser-cacheable pages. Keep account data, dashboards with
private content, and per-user HTML on `dynamic()` unless the application has a
clear cache strategy.

Deployment still owns compression, TLS, HTTP/2 or HTTP/3, CDN rules, and long
cache windows for versioned application assets.

## Security Defaults

Safe defaults:

- text nodes and attribute values are escaped;
- unsafe raw HTML requires `unsafeHtml(...)` or `UnsafeHtml.of(...)`;
- invalid element and attribute names are rejected;
- inline event attributes such as `onclick` are blocked;
- URL attributes are sanitized;
- `javascript:` and `vbscript:` are always blocked;
- `data:image/*` is allowed by default for images;
- CSRF protection is enabled by default for live endpoints;
- internal endpoint rate limiting is enabled by default;
- safe JSON error responses avoid stack traces and secrets;
- security headers are enabled by default.

Use server-side live events instead of inline browser event attributes.

## URL Policy

Use normal relative and HTTPS URLs by default:

```java
a("Docs").href("/docs");
a("Open").href("https://example.com");
img().src("data:image/png;base64,...").alt("Preview");
```

Opt into extra schemes deliberately:

```java
UrlPolicy.setDefault(UrlPolicy.builder()
        .allowHttp()
        .allowMailto()
        .allowTel()
        .build());
```

Never try to bypass blocked `javascript:` or `vbscript:` schemes.

## Validation

Use validation in tests, CI, or development builds:

```java
LiveSessionConfig.builder()
        .validationMode(ValidationMode.STRICT)
        .accessibilityValidationEnabled(true)
        .seoValidationEnabled(true)
        .canonicalLinkValidationEnabled(true)
        .openGraphValidationEnabled(true)
        .htmlLangValidationEnabled(true)
        .build();
```

Use `ValidationMode.WARN` when findings should be logged instead of thrown.

## Runtime Actions

Use runtime actions for cross-cutting server hooks:

```java
RuntimeActionRegistry registry = RuntimeActionRegistry.builder()
        .beforeRender(ctx -> audit(ctx.path()))
        .afterRender(result -> metrics(result.renderDuration()))
        .beforeEvent(ctx -> authorize(ctx.clientState()))
        .afterEvent(result -> audit(result.eventId()))
        .onError(error -> log(error.phase(), error.exception()))
        .contributeHead(head -> head.add(meta()
                .attr("name", "robots")
                .attr("content", "index,follow")))
        .build();
```

Register with `LiveSessionConfig.builder().runtimeActions(registry)`.

## Lifecycle

Implement `Component` for reusable renderable objects and `Lifecycle` for
server-side resource setup/cleanup:

```java
public final class SubscriptionPanel implements Component, Lifecycle {
    public void onMount() {
        // open server resource
    }

    public void onUnmount() {
        // close server resource
    }

    public Node render() {
        return section().child(h2("Subscription"));
    }
}
```

Use `component(panel)` when a child component instance needs lifecycle tracking.

## REST Client

`ujfe-core` includes a small Java `HttpClient` based `RestClient`:

```java
var response = RestClient.create()
        .get("https://example.com/api/items")
        .requireSuccessful();

String body = response.body();
```

Keep external I/O out of render hot paths when possible. Use caching,
preloading, or `DataLoader` when the page requires remote data.

## Complete App-Facing API Catalog

This section lists the public APIs a LLM should use when generating application
code. Some classes also expose lower-level methods for adapters and tests; use
those only when building framework integrations.

### Base Rendering Contracts

```java
interface Renderable {
    String render(UjfeContext context);
    default String render();
}

interface Node extends Renderable {
}

interface Component {
    Node render();
}

interface Lifecycle {
    default void onMount();
    default void onUnmount();
}
```

Use `Node` as the return type for pages and reusable render methods. Implement
`Component` when an object owns reusable rendering. Implement `Lifecycle` only
when the component owns server resources that must be mounted/unmounted.

`TextNode` escapes text. `UnsafeHtml` does not escape and must be treated as a
security exception path.

### RouteBuilder

Use `UI.route(...)` for safe application-relative URLs:

```java
route("/users/{id}")
        .with("id", userId)
        .query("tab", "profile")
        .query("filter", List.of("active", "admin"))
        .continueUrl("/dashboard")
        .fragment("details")
        .build();

route("/users/{id}").with("id", userId).link("Open user");
route("/users/{id}").with("id", userId).link(span("Open"));
```

Methods:

```java
with(String name, Object value)
query(String name, Object value)
query(String name, Collection<?> values)
continueUrl(String continueUrl)
fragment(String fragment)
build()
link(String label)
link(Node child)
toString()
```

Rules: patterns are application paths, parameters are required, path segments
and query values are encoded, query/fragment markers are not allowed in the
pattern, and `continueUrl` must be an application-relative path beginning with
one `/`.

### ActionButton And ActionForm

Use these for server actions, admin commands, and form submissions that need
metadata for confirmation, loading state, targets, refresh, CSRF, or live
handlers.

```java
actionButton("Delete", "/items/42")
        .method("delete")
        .csrf(csrfToken)
        .confirm("Delete this item?")
        .loadingText("Deleting")
        .resultTarget("#result")
        .refresh("/items")
        .variant("danger")
        .permission(currentUser.canDelete())
        .css("text-sm");

actionButton("Refresh", this::reload)
        .id("refresh")
        .disabled(false);

actionForm("/items")
        .method("post")
        .csrf("_csrf", csrfToken)
        .confirm("Create item?")
        .loadingText("Saving")
        .resultTarget("#result")
        .statusTarget("#status")
        .refresh("/items")
        .onSubmit(this::save)
        .child(field("name").label("Name").required(true));
```

`ActionButton` methods:

```java
id(String), method(String), csrf(String), csrf(String, String)
confirm(String), loadingText(String), resultTarget(String), refresh(String)
variant(String), css(String), disabled(boolean), permission(boolean)
```

`ActionForm` methods:

```java
method(String), csrf(String), csrf(String, String), confirm(String)
loadingText(String), resultTarget(String), statusTarget(String), refresh(String)
css(String), disabled(boolean), onSubmit(Runnable)
child(Node), children(Node...)
```

For non-`get` and non-`post` HTTP methods, the rendered form uses `post` plus a
hidden `_method` field. Method names must contain only letters.

### FormField

Use `field(name)` to render accessible field wrappers:

```java
field("email")
        .label("Email")
        .text()
        .inputMode("email")
        .autocomplete("email")
        .placeholder("you@example.com");
```

Actual field methods:

```java
id(String), label(String), hint(String), error(String), value(Object)
placeholder(String), autocomplete(String), inputMode(String)
required(boolean), disabled(boolean), readonly(boolean), checked(boolean)
text(), number(), password(), secret(), select(), checkbox(), switchField()
textarea(), rows(int), date(), time(), dateTimeLocal(), json()
file(), audioFile(), accept(String), option(String value, String label)
```

There is no `email()` field method on `FormField`; use `inputEmail()` directly
or `field(...).text().inputMode("email").autocomplete("email")`.

Kinds:

```java
TEXT, NUMBER, PASSWORD, SELECT, CHECKBOX, SWITCH, TEXTAREA, DATE, TIME,
DATETIME_LOCAL, SECRET, JSON, FILE, AUDIO_FILE
```

### DataGrid

Use `dataGrid(rows)` for tabular administrative or operational views:

```java
dataGrid(users)
        .caption("Users")
        .emptyState("No users", "Create the first user.")
        .rowKey("userId", user -> user.id())
        .selectable(true)
        .filter("q", "Search", query)
        .sortableTextColumn("name", "Name", User::name)
        .responsiveTextColumn("email", "Email", 2, User::email)
        .column("Status", user -> badge(user.status(), "neutral"))
        .rowAction("Open", user -> route("/users/{id}").with("id", user.id()).build())
        .sortedBy("name", false)
        .pagination(page, pageSize, totalRows)
        .css("w-full");
```

Methods:

```java
caption(String), emptyState(String), emptyState(String, String)
rowKey(String name, Function<T, String> rowKey)
selectable(boolean), sortedBy(String columnKey, boolean descending)
pagination(int page, int pageSize, int totalRows), css(String)
filter(String name, String label, Object value)
column(String header, Function<T, Node> renderer)
textColumn(String header, Function<T, ?> renderer)
sortableTextColumn(String key, String header, Function<T, ?> renderer)
responsiveTextColumn(String key, String header, int priority, Function<T, ?> renderer)
rowAction(String label, Function<T, String> href)
```

`DataGrid` requires at least one column. `pagination` values must be positive
except `totalRows`, which may be zero.

### ObjectSchema, ObjectEditor, TypeSchema

Use `objectSchema()` and `objectEditor(schema)` for schema-driven forms:

```java
ObjectSchema schema = objectSchema()
        .title("Settings")
        .field("name", "Name", ObjectSchema.FieldType.TEXT)
            .required(true)
            .hint("Visible label")
            .defaultValue("Default")
            .done()
        .field("mode", "Mode", ObjectSchema.FieldType.SELECT)
            .option("basic", "Basic")
            .option("advanced", "Advanced")
            .done();

objectEditor(schema)
        .value("name", "Demo")
        .error("mode", "Choose a mode")
        .jsonPreview(true);
```

`ObjectSchema`:

```java
ObjectSchema.create()
title(String)
field(String name, String label, FieldType type)
title()
fields()
```

`ObjectSchema.Field`:

```java
required(boolean), hint(String), defaultValue(Object), option(String, String)
done(), name(), label(), type(), hint(), defaultValue(), required(), options()
```

`FieldType` values:

```java
TEXT, NUMBER, BOOLEAN, SELECT, SECRET, JSON
```

`ObjectEditor`:

```java
value(String fieldName, Object value)
values(Map<String, ?> values)
error(String fieldName, String message)
jsonPreview(boolean)
```

`TypeSchema.of(SomeClass.class)` reflects non-static declared fields and
exposes:

```java
type()
propertyNames()
json()
```

### Browser And Client Bridges

These APIs render metadata and script/module references. UJFE does not provide
application-specific client behavior; the referenced JavaScript is owned by the
application.

`ClientModule`:

```java
clientModule("charts")
        .src("/assets/charts.js")
        .action("render", ChartRequest.class)
        .target("#chart")
        .errorTarget("#chart-error")
        .defer(true);
```

Methods:

```java
src(String)
action(String name, Class<?> requestType)
target(String selector)
errorTarget(String selector)
defer(boolean)
```

Default source is `/assets/ujfe/{name}.js`; names may contain letters, digits,
`.`, `-`, and `_`. This default path is only a convention. UJFE does not serve
that application module. The host application must provide the file through
Spring resources, the servlet container, a reverse proxy, a CDN, or override it
with `.src(...)`.

`BrowserApiBridge`:

```java
browserBridge("clipboard")
        .moduleSrc("/assets/clipboard.js")
        .api("navigator.clipboard")
        .action("copy", CopyRequest.class, CopyResult.class)
        .trigger("[data-copy]")
        .resultTarget("#copy-result")
        .errorTarget("#copy-error")
        .arrayBufferBase64Url(true);
```

Methods:

```java
moduleSrc(String), api(String)
action(String action, Class<?> requestType, Class<?> responseType)
trigger(String selector), resultTarget(String selector), errorTarget(String selector)
arrayBufferBase64Url(boolean)
```

`WebAuthnBridge`:

```java
webAuthnLogin("/webauthn/login/challenge", "/webauthn/login/complete")
        .moduleSrc("/assets/webauthn.js")
        .trigger("[data-passkey-login]")
        .statusTarget("#status")
        .errorTarget("#error")
        .userVerification("preferred")
        .timeout(Duration.ofMinutes(2));

webAuthnRegistration("/webauthn/register/challenge", "/webauthn/register/complete");
```

Methods:

```java
moduleSrc(String), trigger(String), errorTarget(String), statusTarget(String)
userVerification(String), timeout(Duration)
```

Challenge and response URLs must be application-relative paths.

Default WebAuthn module source is `/assets/ujfe/webauthn.js`. This is also only
a convention, not a framework-served internal asset. Unless a future UJFE
runtime explicitly serves an official module under `/_ujfe/*`, the application
must either provide `/assets/ujfe/webauthn.js` itself or call
`.moduleSrc("/assets/your-webauthn.js")`.

External JavaScript contract:

- `clientModule(name)` renders a module script:
  `<script type="module" src="..." data-ujfe-client-module="name"
  data-ujfe-client-actions="[...]">`.
- `clientModule(...).target(selector)` renders `data-ujfe-target`.
- `clientModule(...).errorTarget(selector)` renders `data-ujfe-error-target`.
- `clientModule(...).defer(true)` renders the boolean `defer` attribute.
- `data-ujfe-client-actions` is a JSON array with action names and Java request
  type names. It is metadata for the app-owned module; UJFE does not invoke
  those functions automatically.
- A client module should initialize on import or DOM readiness, find its own
  script by `script[data-ujfe-client-module="name"]`, read the metadata, attach
  browser behavior to the configured target, and write failures to the
  configured error target when present.

`browserBridge(name)` renders a marker element plus a child `clientModule`:

```html
<div
  data-ujfe-browser-bridge="clipboard"
  data-ujfe-module-src="/assets/clipboard.js"
  data-ujfe-arraybuffer-base64url="true"
  data-ujfe-browser-api="navigator.clipboard"
  data-ujfe-browser-action="copy"
  data-ujfe-request-type="app.CopyRequest"
  data-ujfe-response-type="app.CopyResult"
  data-ujfe-trigger="[data-copy]"
  data-ujfe-result-target="#copy-result"
  data-ujfe-error-target="#copy-error">
  <script type="module" src="/assets/clipboard.js"
    data-ujfe-client-module="clipboard" data-ujfe-client-actions="[]"
    defer></script>
</div>
```

The app-owned bridge module should query
`[data-ujfe-browser-bridge="clipboard"]`, attach behavior to
`data-ujfe-trigger`, call the browser API named by `data-ujfe-browser-api`,
write successful output to `data-ujfe-result-target`, and write recoverable
errors to `data-ujfe-error-target`.

`webAuthnLogin(...)` and `webAuthnRegistration(...)` render a marker element
plus a child `clientModule("webauthn")`:

```html
<div
  data-ujfe-webauthn="authenticate"
  data-ujfe-challenge-url="/webauthn/login/challenge"
  data-ujfe-response-url="/webauthn/login/complete"
  data-ujfe-user-verification="preferred"
  data-ujfe-timeout-ms="120000"
  data-ujfe-arraybuffer-base64url="true"
  data-ujfe-trigger="[data-passkey-login]"
  data-ujfe-status-target="#status"
  data-ujfe-error-target="#error">
  <script type="module" src="/assets/ujfe/webauthn.js"
    data-ujfe-client-module="webauthn"
    data-ujfe-client-actions='[{"name":"authenticate","requestType":"ujfe.core.WebAuthnBridge$PublicKeyCredentialRequest"}]'
    defer></script>
</div>
```

The app-owned WebAuthn module should query `[data-ujfe-webauthn]`, wait for
the trigger selector when present, fetch challenge options from
`data-ujfe-challenge-url`, convert WebAuthn binary values with base64url when
`data-ujfe-arraybuffer-base64url="true"`, call
`navigator.credentials.get(...)` for `authenticate` or
`navigator.credentials.create(...)` for `register`, then post the serialized
credential to `data-ujfe-response-url`. It should write progress to
`data-ujfe-status-target` and errors to `data-ujfe-error-target` when those
targets are configured.

### Realtime And Media

`RealtimeSubscription` renders metadata for an application-owned SSE,
WebSocket, or polling bridge:

```java
realtime("/events")
        .onEvent(NotificationEvent.class)
        .heartbeat(Duration.ofSeconds(30))
        .reconnectDelay(Duration.ofSeconds(2))
        .debounce(Duration.ofMillis(150))
        .pollingFallback(Duration.ofSeconds(10))
        .connectionStateTarget("#connection")
        .payloadTarget("#notifications");
```

Methods:

```java
onEvent(Class<?> eventType), heartbeat(Duration), reconnectDelay(Duration)
debounce(Duration), pollingFallback(Duration)
connectionStateTarget(String), payloadTarget(String)
```

`AudioPlayer`:

```java
audioPlayer("/assets/audio.mp3")
        .title("Call recording")
        .preload("metadata")
        .metadata("Duration", "02:10")
        .duration("02:10")
        .waveform(List.of(8, 20, 42, 30))
        .timeline(true)
        .downloadAllowed(false)
        .errorText("Audio unavailable")
        .loadingText("Loading audio");
```

Methods:

```java
title(String), preload(String), metadata(String label, Object value)
downloadAllowed(boolean), downloadUrl(String), errorText(String)
loadingText(String), duration(String), timeline(boolean), waveform(List<Integer>)
```

Waveform samples must be integers from `0` to `100`.

### Loading And Boundaries

Use `DataLoader` when page data may fail, timeout, or load in parallel:

```java
LoadResult<User> result = loader(User.class)
        .source(() -> api.loadUser(id))
        .fallback(() -> User.empty())
        .timeout(Duration.ofSeconds(2))
        .virtualThreadsWhenAvailable()
        .load();

loadBoundary(result)
        .success(user -> userCard(user))
        .empty(p("No user"))
        .error(div().role("alert").child("Could not load user"))
        .timeout(div().role("alert").child("Timed out"));
```

`DataLoader<T>`:

```java
source(Supplier<T>), fallback(Supplier<T>), timeout(Duration)
executor(Executor), virtualThreadsWhenAvailable()
parallel(String name, Supplier<?> task)
load()
loadMap()
type()
```

`LoadResult<T>`:

```java
success(T), failure(Throwable), timeout(Throwable)
status(), value(), error(), isSuccess()
```

Statuses:

```java
SUCCESS, EMPTY, FAILURE, TIMEOUT
```

`LoadBoundary<T>`:

```java
success(Function<T, Node>)
empty(Node)
error(Node)
timeout(Node)
```

### Generic UI Surfaces

These primitives are generic building blocks for dashboards, admin consoles,
editors, monitoring tools, and product UIs.

```java
badge("Ready")
badge("Failed", "danger")
statusIndicator("Online", "success")
metricTile("Latency", "42 ms")
metricTile("Latency", "42 ms", "p95", "emerald")
toolbar(button("Refresh"), button("Export"))
sectionHeader("Deployments", "Latest production state")
splitView(primaryNode, secondaryNode)
toast("Saved", "success")
modalDialog("Confirm", p("Continue?"))
workflowBanner("Published", "The workflow is live.")
diffSummary("Changes", 2, 1, 0)
```

`AlertList`:

```java
alertList()
        .title("Alerts")
        .alert("warning", "High latency")
        .alert("critical", "Open incident", "/incidents/1");
```

Methods: `title(String)`, `alert(String tone, String message)`,
`alert(String tone, String message, String href)`.

`HealthPanel`:

```java
healthPanel()
        .title("System health")
        .item("Database", "healthy")
        .item("Queue", "degraded", "Lag is high");
```

Methods: `title(String)`, `item(String label, String status)`,
`item(String label, String status, String detail)`.

`ComponentPreview`:

```java
componentPreview("Buttons")
        .story("Primary", button("Save"))
        .story("Danger", actionButton("Delete", "/delete").variant("danger"));
```

Methods: `story(String name, Node node)`.

`Sparkline`:

```java
sparkline(List.of(10, 14, 8, 20))
        .size(160, 40)
        .label("Revenue trend")
        .tone("emerald");
```

Methods: `size(int width, int height)`, `label(String)`, `tone(String)`.

`GraphBuilder`:

```java
graphBuilder("flow")
        .size(960, 540)
        .panZoom(true)
        .minimap(true)
        .readonly(false)
        .node("start", "Start", 100, 120)
        .node("approve", "Approve", "decision", 320, 120)
        .edge("start", "approve", "next", 1);
```

Methods:

```java
size(int, int), panZoom(boolean), minimap(boolean), readonly(boolean)
node(String id, String label, int x, int y)
node(String id, String label, String type, int x, int y)
edge(String from, String to)
edge(String from, String to, String label, int order)
```

`WorkflowTimeline`:

```java
workflowTimeline()
        .title("Release")
        .currentStatus("deploying")
        .step("Build", "done")
        .step("Deploy", "running", "Production rollout");
```

Methods: `title(String)`, `currentStatus(String)`, `step(String, String)`,
`step(String, String, String)`.

### Component Metadata And Scoped Styling

`UiContract` emits typed payload/action/event metadata:

```java
contract("checkout")
        .payload(CheckoutPayload.class)
        .action("submit", SubmitRequest.class)
        .event("updated", CheckoutUpdated.class);
```

Methods: `payload(Class<?>)`, `action(String, Class<?>)`,
`event(String, Class<?>)`.

`ScopedStyle` references an external stylesheet for one component:

```java
ScopedStyle style = scopedStyle("user-card")
        .href("/assets/components/user-card.css")
        .token("accent", "#2563eb");

String titleClass = style.className("title"); // user-card__title
```

Methods: `href(String)`, `token(String, String)`, `className(String)`.

`UiComponent` groups props, style, contract, markup, and client module:

```java
uiComponent("user-card")
        .props(user)
        .style(style)
        .contract(contract("user-card").payload(User.class))
        .clientModule(clientModule("user-card").src("/assets/user-card.js"))
        .css("block")
        .child(h2(user.name()));
```

Methods:

```java
props(Object), style(ScopedStyle), clientModule(ClientModule)
contract(UiContract), css(String), child(Node), children(Node...)
```

### RenderMode

`RenderMode` is route-level cache intent. It stores policy only; authorization,
personalization, prerendering, CDN invalidation, and business-specific
revalidation remain application/runtime responsibilities.

Factory methods:

```java
RenderMode.dynamic()
RenderMode.staticPage(Duration maxAge)
RenderMode.cached(Duration maxAge)
RenderMode.staticShell(Duration maxAge)
RenderMode.staleWhileRevalidate(Duration maxAge, Duration staleWhileRevalidate)
```

Modifiers and accessors:

```java
scope(RenderMode.CacheScope cacheScope)
revalidateOn(String eventName)
kind()
cacheScope()
maxAge()
staleWhileRevalidate()
revalidateEvent()
cacheControlHeader()
```

Kinds:

```java
DYNAMIC
STATIC
CACHED
STATIC_SHELL
STALE_WHILE_REVALIDATE
```

Cache scopes:

```java
PUBLIC
PRIVATE
NO_STORE
```

Header behavior:

```java
RenderMode.dynamic().cacheControlHeader()
// no-store

RenderMode.cached(Duration.ofSeconds(30)).cacheControlHeader()
// private, max-age=30

RenderMode.staticShell(Duration.ofSeconds(30)).cacheControlHeader()
// private, max-age=30

RenderMode.staticPage(Duration.ofMinutes(10)).cacheControlHeader()
// public, max-age=600

RenderMode.staleWhileRevalidate(Duration.ofSeconds(60), Duration.ofMinutes(5))
        .cacheControlHeader()
// public, max-age=60, stale-while-revalidate=300

RenderMode.staticPage(Duration.ofMinutes(10))
        .scope(RenderMode.CacheScope.PRIVATE)
        .revalidateOn("catalog.updated");
```

Durations cannot be negative. `revalidateOn(...)` accepts event names made from
letters, digits, `.`, `_`, `-`, and `:`. `LiveHttpCache.routeHeaders(...)` emits
`Cache-Control` and, when configured, `X-UJFE-Revalidate-On`.

### Snapshots

Use snapshots in tests, prerendering experiments, or cache metadata generation:

```java
HtmlSnapshot snapshot = HtmlSnapshot.capture(page.render());
snapshot.html();
snapshot.normalizedHtml();
snapshot.fingerprint();
snapshot.containsInlineExecutableScript();

RenderSnapshot render = RenderSnapshot.of(
        "/public",
        page.render(),
        RenderMode.staticPage(Duration.ofMinutes(5))
);
render.route();
render.html();
render.renderMode();
render.createdAt();
render.etag();
render.headers();
```

`HtmlSnapshot.fingerprint()` is SHA-256 of normalized HTML. `RenderSnapshot`
headers include `ETag`, `Cache-Control`, and optional
`X-UJFE-Revalidate-On`.

### URL Policy And Safe URLs

The default `UrlPolicy` allows relative URLs, fragments, scheme-less paths,
`https:`, and `data:image/*`. It blocks `javascript:` and `vbscript:` always.

```java
UrlPolicy.setDefault(UrlPolicy.builder()
        .allowHttp()
        .allowMailto()
        .allowTel()
        .allowScheme("webcal")
        .allowDataImageUrls(true)
        .build());

SafeUrl.sanitize("/docs");
SafeUrl.sanitize("https://example.com");
SafeUrl.sanitize("mailto:support@example.com", UrlPolicy.builder().allowMailto().build());
```

`UrlPolicy`:

```java
getDefault()
setDefault(UrlPolicy)
builder()
```

`UrlPolicy.Builder`:

```java
allowHttp(), allowMailto(), allowTel(), allowScheme(String)
allowDataImageUrls(boolean), build()
```

`SafeUrl`:

```java
sanitize(String)
sanitize(String, UrlPolicy)
```

### Client State

`ClientState` stores browser state that was explicitly allowed by
`ClientStatePolicy` or `LiveSessionConfig`:

```java
ClientState state = ClientState.of(
        Map.of("theme", "dark"),
        Map.of("app.theme", "dark"),
        Map.of("tab", "settings")
);

state.cookie("theme");
state.localStorage("app.theme");
state.sessionStorage("tab");
state.cookies();
state.localStorage();
state.sessionStorage();
state.merge(next);
state.mergeCookiesAndReplaceLocalStorage(next);
```

`ClientStatePolicy`:

```java
ClientStatePolicy.denyAll()
ClientStatePolicy.builder()
allowedCookies()
allowedLocalStorageKeys()
allowedSessionStorageKeys()
allowsCookie(String)
allowsLocalStorageKey(String)
allowsSessionStorageKey(String)
filter(ClientState)
```

`ClientStatePolicy.Builder`:

```java
allowCookie(String), allowCookies(Collection<String>)
allowLocalStorageKey(String), allowLocalStorageKeys(Collection<String>)
allowSessionStorageKey(String), allowSessionStorageKeys(Collection<String>)
build()
```

`Ujfe` reads current thread-bound state during render/event handling:

```java
Ujfe.cookie("theme")
Ujfe.localStorage("app.theme")
Ujfe.sessionStorage("tab")
```

### UjfeContext

Most application pages do not construct `UjfeContext`. It is useful in tests,
custom renderers, and integrations.

```java
UjfeContext.create()
UjfeContext.builder()
UjfeContext.current()
UjfeContext.withCurrent(context, () -> render())
```

Context methods:

```java
nextElementId(String prefix)
registerEvent(Runnable)
registerEvent(Consumer<String>)
executor()
cookie(String), localStorage(String), sessionStorage(String)
clientState()
registerCssClasses(String)
cssClasses()
trackLifecycle(Object)
```

Builder:

```java
elementIdGenerator(ElementIdGenerator)
eventRegistrar(UjfeContext.EventRegistrar)
executor(Executor)
clientState(ClientState)
lifecycleTracker(LifecycleTracker)
build()
```

### CSS Theme

```java
CssTheme.defaultTheme()
CssTheme.of("#2563eb", "#10b981")
theme.primary(500)
theme.secondary(700)
theme.color("primary", 50)
```

Theme steps are `50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 950`.

### LiveSessionConfig

Defaults:

- CSS mode: `CssMode.INTERNAL`.
- `lang`: `en`.
- `title`: `UJFE`.
- CSRF protection enabled.
- security headers enabled.
- client state policy denies all browser storage.
- internal endpoint rate limiting enabled with capacity `120`, refill `60`,
  refill period `1 minute`.
- validation off.
- observability enabled with a no-op sink.

Builder methods:

```java
themeSupplier(Supplier<CssTheme>)
cssMode(CssMode)
devToolsEnabled(boolean)
lang(String)
title(String)
head(Node...)
head(Collection<? extends Node>)
runtimeActions(RuntimeActionRegistry)
observability(ObservabilityConfig)
traceSink(TraceSink)
disableCsrfProtectionForDevelopmentUnsafe()
enableDevelopmentErrorDetailsUnsafe()
securityHeaders(SecurityHeadersConfig)
securityHeader(String name, String value)
disableSecurityHeaders()
clientStatePolicy(ClientStatePolicy)
allowClientCookie(String)
allowClientCookies(Collection<String>)
allowLocalStorageKey(String)
allowLocalStorageKeys(Collection<String>)
allowSessionStorageKey(String)
allowSessionStorageKeys(Collection<String>)
internalEndpointRateLimitingEnabled(boolean)
internalEndpointRateLimit(int capacity, int refillTokens, Duration refillPeriod)
trustedProxy(String)
trustedProxies(Collection<String>)
validationOptions(ValidationOptions)
validationMode(ValidationMode)
accessibilityValidationEnabled(boolean)
seoValidationEnabled(boolean)
canonicalLinkValidationEnabled(boolean)
openGraphValidationEnabled(boolean)
htmlLangValidationEnabled(boolean)
disableValidationRule(String)
strictValidationRule(String)
externalStylesheet(String)
build()
```

Use `disableCsrfProtectionForDevelopmentUnsafe()` and
`enableDevelopmentErrorDetailsUnsafe()` only for local development.

### SecurityHeadersConfig

Default headers:

```text
X-Content-Type-Options: nosniff
Referrer-Policy: strict-origin-when-cross-origin
X-Frame-Options: DENY
Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; media-src 'self' data: https:; object-src 'none'; base-uri 'self'; frame-ancestors 'none'; form-action 'self'
Permissions-Policy: geolocation=(), microphone=(), camera=()
```

API:

```java
SecurityHeadersConfig.defaults()
SecurityHeadersConfig.disabled()
SecurityHeadersConfig.builder()
isEnabled()
headers()
```

Builder:

```java
enabled(boolean), disable(), header(String, String), remove(String)
headers(Map<String, String>), build()
```

### LiveSession

`LiveSession` is the runtime object shared by Netty, Servlet, and Spring
adapters. Application code usually creates it once and passes it to an adapter.

Constructors:

```java
LiveSession(Router router)
LiveSession(Router router, Supplier<CssTheme> themeSupplier)
LiveSession(Router router, Supplier<CssTheme> themeSupplier, boolean devToolsEnabled)
LiveSession(Router router, LiveSessionConfig config)
LiveSession(Router router, PageRenderer pageRenderer, LiveEventRegistry eventRegistry)
LiveSession(Router router, PageRenderer pageRenderer, LiveEventRegistry eventRegistry, Supplier<CssTheme> themeSupplier)
LiveSession(Router router, PageRenderer pageRenderer, LiveEventRegistry eventRegistry, Supplier<CssTheme> themeSupplier, boolean devToolsEnabled)
LiveSession(Router router, PageRenderer pageRenderer, LiveEventRegistry eventRegistry, LiveSessionConfig config)
```

Important methods:

```java
csrfToken()
sessionId()
rateLimitMetrics()
isDevelopmentErrorDetailsEnabled()
securityHeadersConfig()
securityHeaders()
checkInternalEndpointRateLimit(String endpointPath, LiveHttpRequestMetadata metadata)
renderPath(String path)
hasRoute(String path)
renderMode(String path)
handleEvent(String eventId, ClientState nextClientState)
handleEvent(String eventId, ClientState nextClientState, LiveHttpRequestMetadata metadata)
handleEvent(String eventId, String value, ClientState nextClientState, LiveHttpRequestMetadata metadata)
updateClientState(ClientState nextClientState)
updateClientState(ClientState nextClientState, LiveHttpRequestMetadata metadata)
reportHttpError(Throwable exception, RuntimePhase phase, String path, String eventId, String traceId, Map<String, Object> requestMetadata, Map<String, Object> runtimeMetadata)
renderDocument(String path, ClientState initialClientState)
renderDocument(String path, ClientState initialClientState, LiveHttpRequestMetadata metadata)
reportRouteNotFound(String path, LiveHttpRequestMetadata metadata)
renderCss(Collection<String> classes)
close()
```

Use adapter-level APIs instead of calling HTTP endpoint internals directly when
building applications.

`LiveRenderResult` exposes `html()` and `css()`.

`LiveHttpRequestMetadata` captures request context for security, rate limits,
observability, and runtime actions:

```java
csrfToken(), origin(), referer(), host(), scheme(), remoteAddress()
forwarded(), xForwardedFor(), xRealIp(), adapterName(), method(), requestId()
hasOrigin(), hasReferer()
```

### Runtime Actions

Builder methods:

```java
RuntimeActionRegistry.empty()
RuntimeActionRegistry.builder()
beforeRender(BeforeRenderAction)
beforeRender(ActionOrder, BeforeRenderAction)
afterRender(AfterRenderAction)
afterRender(ActionOrder, AfterRenderAction)
beforeEvent(BeforeEventAction)
beforeEvent(ActionOrder, BeforeEventAction)
afterEvent(AfterEventAction)
afterEvent(ActionOrder, AfterEventAction)
onError(ErrorAction)
onError(ActionOrder, ErrorAction)
contributeHead(HeadContributionAction)
contributeHead(ActionOrder, HeadContributionAction)
renderTrace(RenderTraceAction)
renderTrace(ActionOrder, RenderTraceAction)
eventTrace(EventTraceAction)
eventTrace(ActionOrder, EventTraceAction)
build()
```

`ActionOrder`: `FIRST`, `EARLY`, `NORMAL`, `LATE`, `LAST`, `of(int)`.
Lower priority executes first; equal priority preserves registration order.

Functional interfaces:

```java
BeforeRenderAction.execute(RenderContext)
AfterRenderAction.execute(RenderResult)
BeforeEventAction.execute(LiveEventContext)
AfterEventAction.execute(LiveEventResult)
ErrorAction.execute(RuntimeErrorContext)
HeadContributionAction.execute(HeadContributionContext)
RenderTraceAction.execute(RenderTrace)
EventTraceAction.execute(EventTrace)
```

Context/result accessors:

```java
RenderContext: path(), page(), session(), requestMetadata(), clientState(),
renderTimestamp(), traceId(), runtimeMetadata(), metadata()

RenderResult: html(), css(), path(), renderDuration(), traceId(), headNodes(),
routeMetadata(), cssMetadata(), runtimeMetadata(), metadata()

LiveEventContext: eventId(), eventType(), session(), requestMetadata(),
clientState(), target(), submittedValues(), traceId(), runtimeMetadata(),
metadata()

LiveEventResult: html(), eventId(), eventDuration(), traceId(),
eventMetadata(), reRenderMetadata(), clientStateMetadata(), runtimeMetadata(),
metadata()

RuntimeErrorContext: exception(), phase(), path(), eventId(), traceId(),
routeMetadata(), eventMetadata(), requestMetadata(), sessionMetadata(),
runtimeMetadata(), metadata()

HeadContributionContext: add(Node), nodes()
```

`RuntimePhase` values: `RENDER`, `EVENT`, `STATE`, `ROUTING`, `ADAPTER`,
`LIFECYCLE`, `HEAD_CONTRIBUTION`, `INTERNAL`.

### Lifecycle Runtime

Application components normally only implement `Lifecycle`. Integrations can
use:

```java
LifecycleRuntime.create()
beginRender(LifecycleContext)
cleanup(UnmountContext)
isMounted(Lifecycle)
registry()
```

Lifecycle contexts:

```java
LifecycleContext: path(), session(), traceId(), metadata()
MountContext(LifecycleContext)
UnmountContext(LifecycleContext, String reason)
UnmountContext.reason()
```

### Observability

Configure tracing through `LiveSessionConfig.observability(...)` or
`traceSink(...)`.

```java
ObservabilityConfig.builder()
        .renderTracesEnabled(true)
        .eventTracesEnabled(true)
        .traceSink(mySink)
        .addTraceSink(otherSink)
        .clock(Clock.systemUTC())
        .build();
```

`TraceSink` methods:

```java
onRenderTrace(RenderTrace trace)
onEventTrace(EventTrace trace)
```

`RenderTrace` builder:

```java
traceId(String), requestId(String), route(String), httpMethod(String)
httpStatus(Integer), adapterName(String), duration(Duration)
responseSizeBytes(long), status(TraceStatus), errorCode(String)
errorType(String), startedAt(Instant), source(String), build()
```

`EventTrace` builder:

```java
traceId(String), requestId(String), route(String), eventId(String)
httpMethod(String), httpStatus(Integer), adapterName(String), duration(Duration)
responseSizeBytes(long), status(TraceStatus), errorCode(String)
errorType(String), startedAt(Instant), handlerFound(boolean)
handlerCompleted(boolean), build()
```

`TraceStatus` values are `SUCCESS`, `CLIENT_ERROR`, `SERVER_ERROR`,
`NOT_FOUND`, `FORBIDDEN`, `RATE_LIMITED`, `VALIDATION_FAILED`, and `ERROR`.

### Validation API

`ValidationOptions` presets:

```java
ValidationOptions.off()
ValidationOptions.accessibility()
ValidationOptions.seo()
ValidationOptions.recommended()
ValidationOptions.strict()
ValidationOptions.builder()
```

Builder:

```java
mode(ValidationMode)
accessibilityValidationEnabled(boolean)
seoValidationEnabled(boolean)
canonicalLinkValidationEnabled(boolean)
openGraphValidationEnabled(boolean)
htmlLangValidationEnabled(boolean)
disableRule(String)
disabledRules(Collection<String>)
strictRule(String)
strictRules(Collection<String>)
build()
```

Accessors:

```java
mode(), accessibilityValidationEnabled(), seoValidationEnabled()
canonicalLinkValidationEnabled(), openGraphValidationEnabled()
htmlLangValidationEnabled(), disabledRuleIds(), strictRuleIds()
isRuleEnabled(String), severityFor(String), toBuilder()
```

`ValidationMode`: `OFF`, `WARN`, `STRICT`.

`DocumentValidator`:

```java
of(ValidationOptions), off(), accessibility(), seo(), recommended(), strict()
options()
validate(Node), validate(String)
validateOrThrow(Node), validateOrThrow(String)
```

`ValidationResult`:

```java
empty(), of(Collection<ValidationFinding>)
findings(), isValid(), hasFindings(), hasErrors(), errors()
findingsForRule(String), throwIfInvalid(), throwIfErrors(), summary()
```

`ValidationFinding`:

```java
ValidationFinding.builder()
ruleId(), severity(), category(), message(), suggestion()
element(), attribute(), location(), format(), toString()
```

`ValidationFinding.Builder`:

```java
ruleId(String), severity(ValidationSeverity), category(ValidationCategory)
message(String), suggestion(String), element(String), attribute(String)
location(String), build()
```

`ValidationSeverity`: `WARN`, `ERROR`.

`ValidationCategory`: `ACCESSIBILITY`, `SEO`. Use `category.value()` for the
lowercase external value.

Common accessibility rule IDs include:

```text
accessibility.document.single-main
accessibility.heading.single-h1
accessibility.img.alt
accessibility.button.accessible-name
accessibility.input.label
accessibility.id.duplicate
accessibility.aria-labelledby.target
accessibility.aria-describedby.target
accessibility.interactive.accessible-name
accessibility.interactive.nested
```

Common SEO rule IDs include:

```text
seo.document.title
seo.document.meta-description
seo.heading.single-h1
seo.heading.hierarchy
seo.link.canonical
seo.meta.duplicate-title
seo.meta.duplicate-description
seo.html.lang
seo.open-graph.basic
seo.img.alt
```

### Routing API

`Router`:

```java
register(Object pageInstance)
register(Class<?> pageType)
register(String path, Supplier<Object> pageFactory)
register(RouteDefinition route)
register(RouteSource source)
register(RouteSource... sources)
resolve(String path)
routes()
```

`RouteDefinition` constructors and methods:

```java
new RouteDefinition(String path, Supplier<Object> pageFactory)
new RouteDefinition(String path, Supplier<Object> pageFactory, RenderMode renderMode)
new RouteDefinition(String path, Supplier<Object> pageFactory, String sourceDescription)
new RouteDefinition(String path, Supplier<Object> pageFactory, String sourceDescription, RenderMode renderMode)
RouteDefinition.pageClass(String path, Class<?> pageType)
path(), createPage(), pageType(), sourceDescription(), renderMode()
withRenderMode(RenderMode)
```

`@Page("/path")` annotates route classes. Annotated page classes must be
instantiable by the router and expose a valid render method.

`ManualRouteSource`:

```java
register(String path, Supplier<Object> pageFactory)
register(RouteDefinition route)
routes()
```

`ReflectionPageScanner`:

```java
new ReflectionPageScanner(Class<?>... candidates)
ReflectionPageScanner.forPackages(String... packageNames)
routes()
```

Duplicate route paths fail explicitly.

### Signals API

```java
Signal<T> signal = Signals.signal(initialValue);
Computed<T> computed = Signals.computed(() -> transform(signal.get()));
```

`Signal<T>`:

```java
get()
set(T value)
update(Function<T, T> updater)
subscribe(Consumer<T> listener)
```

`MutableSignal<T>` adds `subscribeInvalidation(Runnable)` through the internal
dependency interface. `Computed<T>` is read-only: `set` and `update` throw
`UnsupportedOperationException`. `Computed.get()` is lazy, caches successful
values, tracks dependencies, invalidates when dependencies change, and throws
`ComputedCycleException` for cycles.

### REST API

`RestClient`:

```java
RestClient.create()
RestClient.create(Duration timeout)
get(String uri)
get(URI uri)
send(HttpRequest request)
```

`RestResponse`:

```java
statusCode()
body()
headers()
successful()
firstHeader(String name)
requireSuccessful()
```

Keep network I/O outside render hot paths unless the page intentionally blocks
on server-side data.

### Runtime Adapters

Standalone Netty:

```java
UjfeServerConfig.builder()
        .host("0.0.0.0")
        .port(8080)
        .maxJsonPayloadBytes(262_144)
        .build();

UjfeServer server = new UjfeServer(config, liveSession);
server.start();
server.blockUntilShutdown();
server.stop();
```

`UjfeServerConfig` defaults to host `0.0.0.0`, port `8080`, and
`LiveHttpCodec.DEFAULT_MAX_JSON_PAYLOAD_BYTES`.

Servlet:

```java
new UjfeServlet()
new UjfeServlet(router)
new UjfeServlet(router, liveSessionConfig)
new UjfeServlet(router, liveSessionConfig, maxJsonPayloadBytes)
new UjfeServlet(router, liveSession)
new UjfeServlet(router, liveSession, maxJsonPayloadBytes)
```

Servlet context attributes:

```text
ujfe.router
ujfe.liveSession
```

Servlet/application properties:

```text
ujfe.routes.packages
ujfe.live.title
ujfe.live.lang
ujfe.live.dev-tools-enabled
ujfe.live.css-mode
ujfe.live.max-json-payload-bytes
ujfe.live.disable-csrf-protection-for-development-unsafe
ujfe.errors.development-details.enabled
ujfe.live.rate-limit.enabled
ujfe.live.rate-limit.capacity
ujfe.live.rate-limit.refill-tokens
ujfe.live.rate-limit.refill-period-ms
ujfe.live.trusted-proxies
ujfe.client-state.cookies
ujfe.client-state.local-storage-keys
ujfe.client-state.session-storage-keys
ujfe.validation.mode
ujfe.validation.accessibility.enabled
ujfe.validation.seo.enabled
ujfe.validation.canonical.enabled
ujfe.validation.open-graph.enabled
ujfe.validation.html-lang.enabled
ujfe.validation.disabled-rules
ujfe.security.headers.enabled
ujfe.security.headers.x-content-type-options
ujfe.security.headers.x-frame-options
ujfe.security.headers.referrer-policy
ujfe.security.headers.content-security-policy
ujfe.security.headers.permissions-policy
```

Spring MVC:

```java
@Bean
Router router(HomePage home) {
    return new Router().register(home);
}

@Bean
LiveSessionConfig ujfeLiveSessionConfig() {
    return LiveSessionConfig.builder().title("App").build();
}
```

Spring Boot properties:

```yaml
ujfe:
  errors:
    development-details:
      enabled: false
  security:
    headers:
      enabled: true
      x-content-type-options: nosniff
      x-frame-options: DENY
      referrer-policy: strict-origin-when-cross-origin
      content-security-policy: "default-src 'self'; script-src 'self'"
      permissions-policy: "geolocation=(), microphone=(), camera=()"
  client-state:
    cookies: [theme]
    local-storage-keys: [app.theme]
    session-storage-keys: [active.tab]
  validation:
    mode: WARN
    accessibility-enabled: true
    seo-enabled: true
    canonical-enabled: true
    open-graph-enabled: true
    html-lang-enabled: true
    disabled-rules: []
```

`UjfeSpringHandlerMapping.DEFAULT_ORDER` is `100`. The Spring handler claims
UJFE internal endpoints and registered GET routes, but lets static asset-like
paths fall through to Spring resource handling.

### Internal HTTP Endpoints

Framework-owned endpoints:

```text
POST /_ujfe/event
POST /_ujfe/state
GET  /_ujfe/css
GET  /_ujfe/client.js
GET  /_ujfe/dev.js
```

Application code should not create page routes under `/_ujfe/*`.

### Static Asset Handling

`StaticAssetHandler` is shared by adapters:

```java
isStaticAssetPath(String path)
isUnsafePath(String path)
contentType(String path)
missingAssetBody()
rejectedAssetBody()
logNotFound(String adapter, String path)
logRejected(String adapter, String path)
```

This is an adapter safety helper, not a general static file server.

### Public Support APIs For Integrations And Tests

These APIs are public because adapters, tests, or advanced integrations may
need them. They are not the preferred primitives for ordinary page code.

Core support:

```java
ElementIdGenerator.sequential()
ElementIdGenerator.nextId(String prefix)
Base64Url.encode(byte[] bytes)
Base64Url.decode(String value)
HtmlEscaper.escape(String value)
AttributeEscaper.escape(String value)
HtmlElementMetadata.voidElements()
HtmlElementMetadata.isVoidElement(String tagName)
HtmlElementMetadata.isVoidElement(ElementNamespace namespace, String tagName)
UtilityCssRenderer.render(Collection<String> classes)
UtilityCssRenderer.render(Collection<String> classes, CssTheme theme)
```

`ElementNamespace` values: `HTML`, `SVG`, `MATHML`.

Routing support:

```java
PageRenderer.render(RouteDefinition route)
PageRenderer.render(Object page)
PageClassValidator.validateAnnotatedPageClass(Class<?>)
PageClassValidator.validateAnnotatedPageInstanceClass(Class<?>)
PageClassValidator.validatePageClass(Class<?>)
PageClassValidator.instantiate(Class<?>)
```

HTTP codec support:

```java
LiveHttpCodec.DEFAULT_MAX_JSON_PAYLOAD_BYTES
LiveHttpCodec.parseEventPayload(String json)
LiveHttpCodec.parseEventPayload(String json, int maxPayloadBytes)
LiveHttpCodec.parseStatePayload(String json)
LiveHttpCodec.parseStatePayload(String json, int maxPayloadBytes)
LiveHttpCodec.livePayload(LiveRenderResult result)
LiveHttpCodec.parseCssClasses(String classes)
LiveHttpCodec.extractEventId(String json)
LiveHttpCodec.extractClientState(String json)
LiveHttpCodec.requirePayloadSize(long payloadSizeBytes, int maxPayloadBytes)
LiveHttpCodec.readPayload(Reader reader, int maxPayloadBytes)
LiveHttpCodec.logRejectedPayload(LiveHttpCodecException, String adapter, String traceId)
LiveHttpCodec.logRejectedCsrf(LiveCsrfException, String adapter, String traceId)
LiveHttpCodec.logRejectedRateLimit(LiveRateLimitException, String adapter, String traceId)
```

`LiveHttpEventPayload`: `eventId()`, `clientState()`, `value()`.

`LiveHttpPaths` constants and helpers:

```java
CLIENT_SCRIPT, DEV_SCRIPT, CSS, EVENT, STATE
internalPaths()
isInternalPath(String path)
```

HTTP cache support:

```java
LiveHttpCache.CACHE_CONTROL
LiveHttpCache.ETAG
LiveHttpCache.IF_NONE_MATCH
LiveHttpCache.REVALIDATE_ON
LiveHttpCache.clientScriptHeaders()
LiveHttpCache.devScriptHeaders()
LiveHttpCache.cssHeaders(String css)
LiveHttpCache.routeHeaders(RenderMode renderMode)
LiveHttpCache.matchesEtag(String ifNoneMatch, String etag)

LiveHttpCache.CacheHeaders.etag()
LiveHttpCache.CacheHeaders.cacheControl()
LiveHttpCache.CacheHeaders.headers()
LiveHttpCache.CacheHeaders.matches(String ifNoneMatch)
```

Rate limiting:

```java
interface RateLimiter {
    RateLimitDecision allow(RateLimitRequest request);
    RateLimitMetrics metrics();
}

new TokenBucketRateLimiter(LiveSessionConfig config)
new RateLimitRequest(String endpointPath, String sessionId, LiveHttpRequestMetadata metadata)
RateLimitDecision.allowed(RateLimitKeyType keyType)
RateLimitDecision.rejected(RateLimitKeyType keyType, Duration retryAfter)
```

Accessors:

```java
RateLimitRequest.endpointPath(), sessionId(), metadata()
RateLimitDecision.allowed(), keyType(), retryAfter()
RateLimitMetrics.allowedRequests(), rejectedRequests()
RateLimitMetrics.allowedRequests(String endpointPath)
RateLimitMetrics.rejectedRequests(String endpointPath)
RateLimitMetrics.rejectedRequests(RateLimitKeyType keyType)
```

`RateLimitKeyType`: `SESSION`, `IP`.

Safe error responses:

```java
UjfeErrorCode.safeMessage()
UjfeErrorResponse.CONTENT_TYPE
UjfeErrorResponse.httpStatus()
UjfeErrorResponse.code()
UjfeErrorResponse.message()
UjfeErrorResponse.requestId()
UjfeErrorResponse.details()
UjfeErrorResponse.retryAfterSeconds()
UjfeErrorResponse.body()

ErrorResponseContext.builder()
ErrorResponseContext.adapter()
ErrorResponseContext.method()
ErrorResponseContext.path()
ErrorResponseContext.requestId()
ErrorResponseContext.phase()

ErrorResponseContext.Builder.adapter(String)
ErrorResponseContext.Builder.method(String)
ErrorResponseContext.Builder.path(String)
ErrorResponseContext.Builder.requestId(String)
ErrorResponseContext.Builder.phase(RuntimePhase)
ErrorResponseContext.Builder.build()

ErrorResponseRenderer.create(boolean developmentDetailsEnabled)
ErrorResponseRenderer.render(UjfeErrorCode, int httpStatus, Throwable, ErrorResponseContext)
ErrorResponseRenderer.render(Throwable, ErrorResponseContext)
ErrorResponseRenderer.routeNotFound(ErrorResponseContext)
ErrorResponseRenderer.methodNotAllowed(ErrorResponseContext)
```

`UjfeErrorCode` values:

```text
UJFE_INTERNAL_ERROR
UJFE_ROUTE_NOT_FOUND
UJFE_RENDER_ERROR
UJFE_EVENT_HANDLER_ERROR
UJFE_INVALID_REQUEST
UJFE_BAD_REQUEST
UJFE_UNAUTHORIZED
UJFE_FORBIDDEN
UJFE_RATE_LIMITED
UJFE_CSRF_VALIDATION_FAILED
UJFE_STATE_ERROR
UJFE_CONFIGURATION_ERROR
```

Live exceptions expose safe metadata:

```java
LiveHttpCodecException.category(), safeMessage(), httpStatus(),
payloadLimitBytes(), payloadSizeBytes()

LiveCsrfException.category(), safeMessage(), isCsrfHeaderPresent()

LiveRateLimitException.endpointPath(), keyType(), retryAfterSeconds(),
safeMessage()
```

`LiveHttpFailureCategory` values:

```text
EMPTY_PAYLOAD
EMPTY_JSON
INVALID_JSON
MISSING_EVENT_ID
MISSING_CLIENT_STATE
PAYLOAD_TOO_LARGE
MISSING_CSRF_TOKEN
INVALID_CSRF_TOKEN
CROSS_ORIGIN_REQUEST
```

Generated runtime scripts:

```java
LiveClientScript.script()
LiveDevToolsScript.script()
```

Use these only in adapter/tests; application pages should load the built-in
scripts through normal UJFE document rendering.

## CLI Conversion

Use the converter to migrate existing HTML into Java DSL:

```bash
ujfe convert page.html \
  --output src/main/java/app/pages/Page.java \
  --type html \
  --comments drop \
  --css extract \
  --componentize
```

The converter preserves classes and attributes, supports common HTML/media/form
tags, can extract CSS, and uses unsafe fallback only when explicitly configured.

## Testing Patterns

Render pages directly in tests:

```java
String html = new HomePage().render().render();
assertTrue(html.contains("<main"));
assertTrue(html.contains("Dashboard"));
```

Use adapter tests for HTTP headers, CSRF, rate limits, static asset behavior,
and live event JSON responses.

## LLM Coding Rules

When generating UJFE code:

1. Use normal Java and `import static ujfe.core.UI.*`.
2. Return `Node` from `render()`.
3. Prefer semantic HTML helpers and `Element.of(...)` for unknown tags.
4. Use `.child(...)` and `.children(...)`; text children are escaped.
5. Use `.css(...)` for class attributes; do not assume internal CSS supports
   every Tailwind class.
6. Use `CssMode.EXTERNAL` when the app has its own CSS pipeline.
7. Use `button(...).onClick(...)`, `inputText().onInput(...)`,
   `select().onChange(...)`, and `form().onSubmit(...)` for live behavior.
8. Do not write inline `onclick`, `onload`, or raw script event attributes.
9. Host JavaScript, images, video, audio, fonts, and documents outside UJFE page
   routing; reference them with normal URLs.
10. Do not claim standalone Netty serves arbitrary static files.
11. Keep user-specific pages on `RenderMode.dynamic()` unless explicitly told
    they are safe to cache.
12. Do not disable CSRF, security headers, or safe errors in production.
13. Do not use `unsafeHtml(...)` for untrusted input.
14. For Spring apps, use `ujfe-spring`, not `ujfe-http`, unless asked for a
    separate standalone Netty server.
15. Keep the framework project-agnostic. Avoid naming one application domain as
    if UJFE were built only for that domain.
