package app.pages;

import app.AppTheme;
import app.components.AppHeader;
import app.components.BankSelectComponent;
import ujfe.core.Component;
import ujfe.core.Element;
import ujfe.core.Node;
import ujfe.router.Page;

import java.util.Objects;

import static ujfe.core.UI.*;

@Page("/docs")
public final class DocumentationPage implements Component {
    private final AppTheme theme;
    private final BankSelectComponent bankSelect;

    public DocumentationPage(AppTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
        this.bankSelect = new BankSelectComponent(theme::darkMode);
    }

    @Override
    public Node render() {
        return AppHeader.pageShell(theme)
            .child(new AppHeader(theme, AppHeader.DOCS).render())
            .child(
                div()
                    .css("max-w-7xl mx-auto p-6 docs-grid gap-6")
                    .child(sidebar())
                    .child(
                        main()
                            .css("min-w-0 flex flex-col gap-6")
                            .child(introSection())
                            .child(modernJavaSection())
                            .child(themeSection())
                            .child(elementCatalogSection())
                            .child(formSection())
                            .child(cssSection())
                            .child(assetSection())
                            .child(springSection())
                            .child(securitySection())
                            .child(signalsSection())
                            .child(routerSection())
                            .child(performanceSection())
                            .child(runtimeActionsSection())
                            .child(lifecycleSection())
                            .child(restSection())
                            .child(cliSection())
                            .child(devPreviewSection())
                    )
            );
    }

    private Node sidebar() {
        return aside()
            .css(panelClass("p-4"))
            .child(nav()
                .css("flex flex-col gap-3")
                .child(h2("Documentation map").css(sectionTitleClass("text-xl")))
                .child(p("Use this page as a guided map through UJFE concepts: page declaration, routing, CSS modes, live endpoints, runtime lifecycle, and security defaults.")
                    .css(bodyTextClass("text-sm")))
                .child(ul()
                    .css(theme.darkMode()
                        ? "flex flex-col gap-2 text-sm text-slate-300"
                        : "flex flex-col gap-2 text-sm text-slate-700")
                    .child(le().child(strong("Theme"))
                        .child(" with dynamic primary/secondary palettes"))
                    .child(le().child(strong("Modern Java"))
                        .child(" with Java 11 classes, var, lambdas, and streams"))
                    .child(le().child(strong("Elements"))
                        .child(" with API usage examples"))
                    .child(le().child(strong("Forms"))
                        .child(" with inputs and events"))
                    .child(le().child(strong("CSS"))
                        .child(" with utilities and color scales"))
                    .child(le().child(strong("Assets"))
                        .child(" with JavaScript, images, audio, video, and static ownership"))
                    .child(le().child(strong("Spring MVC"))
                        .child(" on the same Tomcat port"))
                    .child(le().child(strong("Security"))
                        .child(" with escaping, safe URLs, attribute validation, URL policy, raw HTML boundaries, and headers"))
                    .child(le().child(strong("Signals"))
                        .child(" with lazy computed values, cache invalidation, and subscribers"))
                    .child(le().child(strong("Router"))
                        .child(" with route sources, deterministic discovery, and AOT metadata direction"))
                    .child(le().child(strong("Performance"))
                        .child(" with internal ETags and route RenderMode cache intent"))
                    .child(le().child(strong("Runtime Actions"))
                        .child(" with server-side extension points for rendering, events, errors, and head contributions"))
                    .child(le().child(strong("Lifecycle"))
                        .child(" with deterministic mount, unmount, and cleanup"))
                    .child(le().child(strong("REST"))
                        .child(" with a select populated from BrasilAPI"))
                    .child(le().child(strong("CLI"))
                        .child(" with HTML-to-UJFE conversion"))
                    .child(le().child(strong("Dev Preview"))
                        .child(" with a visual inspector"))));
    }

    private Node introSection() {
        return section()
            .css(panelClass("p-6 flex flex-col gap-4"))
            .child(h2("How to use UJFE documentation").css(sectionTitleClass("text-2xl")))
            .child(p("UJFE is a server-rendered Java UI framework. Declare pages with @Page, register them in a Router, render standard HTML through the DSL, and use live endpoints when server-side events need to update the browser.")
                .css(bodyTextClass("text-base")))
            .child(p("Each block shows the rendered result and the equivalent Java code. The examples favor the current public API: helpers from ujfe.core.UI, Element.of(...) for generic tags, Signals.signal(...) for live state, and LiveSession for routing plus event dispatch.")
                .css(bodyTextClass("text-base")))
            .child(
                div()
                    .css("grid grid-cols-3 gap-3")
                    .child(cssPill("Safe default", "text and attributes are escaped"))
                    .child(cssPill("Standards first", "real HTML, SVG, MathML, and custom elements"))
                    .child(cssPill("Unsafe boundary", "raw HTML requires unsafeHtml(...)"))
            )
            .child(codeBlock(factoryIndex()));
    }

    private Node themeSection() {
        return section()
            .css(panelClass("p-6 flex flex-col gap-4"))
            .child(h2("CSS modes and dynamic theme").css(sectionTitleClass("text-2xl")))
            .child(p(() -> "Primary: " + theme.primaryColor() + " | Secondary: " + theme.secondaryColor())
                .css(theme.darkMode() ? "text-sm font-mono text-slate-300" : "text-sm font-mono text-slate-700"))
            .child(
                div()
                    .css("flex flex-wrap gap-2")
                    .child(button("Blue + Emerald")
                        .css("px-4 py-2 rounded bg-primary-700 text-white font-semibold")
                        .onClick(theme::useBlueEmerald))
                    .child(button("Rose + Amber")
                        .css(themeButtonClass("primary"))
                        .onClick(theme::useRoseAmber))
                    .child(button("Indigo + Cyan")
                        .css(themeButtonClass("secondary"))
                        .onClick(theme::useIndigoCyan))
            )
            .child(
                div()
                    .css("grid grid-cols-2 gap-3")
                    .child(colorScale("Primary", "primary"))
                    .child(colorScale("Secondary", "secondary"))
            )
            .child(codeBlock(themeCode()));
    }

    private Node modernJavaSection() {
        return section()
            .css(panelClass("p-6 flex flex-col gap-4"))
            .child(h2("Modern Java examples").css(sectionTitleClass("text-2xl")))
            .child(p("UJFE examples prefer modern Java syntax where it improves clarity: var for local values, records for small immutable view models, streams and lambdas for declarative collection mapping, and switch expressions for compact branching.")
                .css(bodyTextClass("text-base")))
            .child(codeBlock(modernJavaCode()));
    }

    private Node colorScale(String title, String palette) {
        return div()
            .css(panelClass("p-3 flex flex-col gap-2"))
            .child(h3(title).css(theme.darkMode() ? "text-lg font-bold text-slate-100" : "text-lg font-bold text-slate-900"))
            .child(
                div()
                    .css("grid grid-cols-3 gap-2")
                    .child(swatch("bg-" + palette + "-50 text-slate-900", palette + "-50"))
                    .child(swatch("bg-" + palette + "-200 text-slate-900", palette + "-200"))
                    .child(swatch("bg-" + palette + "-500 text-white", palette + "-500"))
                    .child(swatch("bg-" + palette + "-700 text-white", palette + "-700"))
                    .child(swatch("bg-" + palette + "-900 text-white", palette + "-900"))
            );
    }

    private Node swatch(String classes, String label) {
        return div()
            .css("rounded border border-slate-200 p-2 text-xs font-semibold " + classes)
            .child(label);
    }

    private Node elementCatalogSection() {
        return section()
            .css("flex flex-col gap-4")
            .child(h2("Supported HTML elements").css("text-2xl font-bold"))
            .child(
                div()
                    .css("catalog-grid gap-4")
                    .child(
                        div()
                            .css("flex flex-col gap-4")
                            .child(docCard(
                                "Layout and text",
                                "Containers, headings, text, emphasis, and code blocks.",
                                layoutCode(),
                                layoutPreview()
                            ))
                            .child(docCard(
                                "Live events",
                                "Click, input, change, and submit generate event ids and execute Runnable handlers in Java.",
                                liveEventCode(),
                                liveEventPreview()
                            ))
                    )
                    .child(docCard(
                        "Links, lists, and media",
                        "Anchors, lists, images, picture/source, canvas, audio, video, maps, and embedded content.",
                        listAndMediaCode(),
                        listAndMediaPreview()
                    ))
                    .child(docCard(
                        "Interactivity and tables",
                        "Details, summary, dialog, popover, template, slot, and real HTML tables generated with Java loops.",
                        modernHtmlCode(),
                        modernHtmlPreview()
                    ))
            );
    }

    private Node formSection() {
        return section()
            .css(tonePanelClass("secondary", "p-6 flex flex-col gap-4"))
            .child(h2("Forms").css(toneTitleClass("secondary", "text-2xl")))
            .child(p("The DSL covers form elements with helpers, but attributes such as required, placeholder, min, and max can also be declared directly with attr(...).")
                .css(bodyTextClass("text-base")))
            .child(
                form()
                    .css("grid grid-cols-2 gap-3")
                    .onSubmit(() -> {
                    })
                    .child(label("Text").forId("doc-text").css(labelClass()))
                    .child(inputText().id("doc-text")
                        .name("text")
                        .placeholder("inputText()")
                        .css(inputClass()))
                    .child(label("Number").forId("doc-number").css(labelClass()))
                    .child(inputNumber().id("doc-number")
                        .name("number")
                        .min("0")
                        .max("99")
                        .css(inputClass()))
                    .child(label("Password").forId("doc-password").css(labelClass()))
                    .child(inputPassword().id("doc-password")
                        .name("password")
                        .placeholder("inputPassword()")
                        .css(inputClass()))
                    .child(label("Select").forId("doc-select").css(labelClass()))
                    .child(select().id("doc-select")
                        .name("select")
                        .css(inputClass())
                        .child(option("Java").value("java"))
                        .child(option("UJFE").value("ujfe")
                            .selected(true)))
                    .child(label().css(checkLabelClass())
                        .child(checkbox().name("check")
                            .checked(true))
                        .child("checkbox()"))
                    .child(label().css(checkLabelClass())
                        .child(radio().name("radio")
                            .value("a")
                            .checked(true))
                        .child("radio()"))
                    .child(textarea("textarea()").name("message")
                        .rows(3)
                        .css(inputClass()))
                    .child(button("Submit live").type("submit")
                        .css("px-4 py-2 rounded bg-secondary-700 text-white font-semibold"))
            )
            .child(codeBlock(formCode()));
    }

    private Node cssSection() {
        return section()
            .css(tonePanelClass("primary", "p-6 flex flex-col gap-4"))
            .child(h2("Internal or external CSS").css(toneTitleClass("primary", "text-2xl")))
            .child(p("INTERNAL mode keeps the current server-side utility renderer. EXTERNAL mode disables UJFE CSS generation and lets teams use Tailwind, Bootstrap, plain CSS, or design systems through regular head links.")
                .css(bodyTextClass("text-base")))
            .child(
                div()
                    .css("grid grid-cols-3 gap-3")
                    .child(cssPill("Layout", "flex, grid, app-shell, demo-grid, docs-grid"))
                    .child(cssPill("Spacing", "p-2..p-8, px-*, py-*, gap-*"))
                    .child(cssPill("Typography", "text-xs..text-4xl, font-*, font-mono"))
                    .child(cssPill("Borders", "rounded, border, border-primary-200"))
                    .child(cssPill("Colors", "bg-primary-50, text-secondary-700"))
                    .child(cssPill("Responsive", "media queries for example grids"))
            )
            .child(codeBlock(cssCode()));
    }

    private Node assetSection() {
        return section()
            .css(tonePanelClass("emerald", "p-6 flex flex-col gap-4"))
            .child(h2("Static assets and browser modules").css(toneTitleClass("emerald", "text-2xl")))
            .child(p("UJFE renders normal HTML references to JavaScript, images, audio, video, fonts, and documents. The framework owns only its internal endpoints under /_ujfe; application assets should be served by Spring resource handlers, a servlet container, a reverse proxy, a CDN, or another static file server.")
                .css(bodyTextClass("text-base")))
            .child(
                div()
                    .css("grid grid-cols-3 gap-3")
                    .child(cssPill("JavaScript", "script(), clientModule(...), BrowserApiBridge"))
                    .child(cssPill("Images", "img(), picture(), source(), data:image/*"))
                    .child(cssPill("Media", "video(), audio(), track(), canvas(), svg()"))
                    .child(cssPill("Files", "object(), embed(), iframe(), link()"))
                    .child(cssPill("Ownership", "host app serves /assets/** and /static/**"))
                    .child(cssPill("Safety", "path traversal rejected before page rendering"))
            )
            .child(codeBlock(assetCode()));
    }

    private Node springSection() {
        return section()
            .css(tonePanelClass("secondary", "p-6 flex flex-col gap-4"))
            .child(h2("Spring Boot on the same port").css(toneTitleClass("secondary", "text-2xl")))
            .child(p("Use ujfe-spring when the Spring Boot application must serve UJFE pages, live events, and internal assets through the same DispatcherServlet/Tomcat port. The ujfe-http module remains available for standalone Netty mode.")
                .css(bodyTextClass("text-base")))
            .child(
                div()
                    .css("grid grid-cols-3 gap-3")
                    .child(cssPill("Tomcat", "same server.port as the application"))
                    .child(cssPill("UJFE routes", "only paths registered in Router"))
                    .child(cssPill("Spring MVC", "controllers and static resources coexist"))
            )
            .child(codeBlock(springCode()));
    }

    private Node securitySection() {
        return section()
            .css(tonePanelClass("rose", "p-6 flex flex-col gap-4"))
            .child(h2("Security by default").css(toneTitleClass("rose", "text-2xl")))
            .child(p("Text and attributes are escaped during SSR. Attribute names are validated to block inline event handlers and malformed names. URL attributes are sanitized through a configurable UrlPolicy that blocks dangerous schemes. Trusted raw HTML has a deliberately unsafe name so security review can find it.")
                .css(bodyTextClass("text-base")))
            .child(
                div()
                    .css("grid grid-cols-3 gap-3")
                    .child(cssPill("A03 Injection", "HTML/attribute escaping, attribute validation, and safe URLs"))
                    .child(cssPill("Attribute validation", "on* handlers blocked, malformed names rejected"))
                    .child(cssPill("URL policy", "javascript: and vbscript: always blocked"))
                    .child(cssPill("A05 Misconfiguration", "CSP, nosniff, frame-ancestors"))
                    .child(cssPill("Configurable schemes", "http:, mailto:, tel: opt-in via UrlPolicy"))
                    .child(cssPill("Unsafe raw HTML", "only unsafeHtml(...) bypasses escaping"))
            )
            .child(
                div()
                    .css("grid grid-cols-3 gap-3")
                    .child(
                        div()
                            .css(tonePanelClass("emerald", "p-4 flex flex-col gap-2"))
                            .child(h3("Escaped path").css(toneTitleClass("emerald", "text-lg")))
                            .child(p("Use normal text and element helpers for application UI, user content, and request data.")
                                .css(bodyTextClass("text-sm")))
                            .child(codeBlock("p(\"<script>\")\n// <p>&lt;script&gt;</p>\n"))
                    )
                    .child(
                        div()
                            .css(tonePanelClass("rose", "p-4 flex flex-col gap-2"))
                            .child(h3("Blocked attributes").css(toneTitleClass("rose", "text-lg")))
                            .child(p("Inline event handlers and malformed attribute names are rejected before rendering.")
                                .css(bodyTextClass("text-sm")))
                            .child(codeBlock("// Throws IllegalArgumentException:\ndiv().attr(\"onclick\", \"alert(1)\")\ndiv().attr(\"my attr\", \"value\")\n"))
                    )
                    .child(
                        div()
                            .css(tonePanelClass("rose", "p-4 flex flex-col gap-2"))
                            .child(h3("Unsafe path").css(toneTitleClass("rose", "text-lg")))
                            .child(p("Use only for trusted, pre-sanitized fragments where escaping would be incorrect.")
                                .css(bodyTextClass("text-sm")))
                            .child(codeBlock("unsafeHtml(\"<strong>trusted</strong>\")\n// <strong>trusted</strong>\n"))
                    )
            )
            .child(codeBlock(securityCode()));
    }

    private Node signalsSection() {
        return section()
            .css(tonePanelClass("emerald", "p-6 flex flex-col gap-4"))
            .child(h2("Signals and computed values").css(toneTitleClass("emerald", "text-2xl")))
            .child(p("Mutable signals hold live server state. Computed signals derive read-only values lazily, cache successful evaluations, and invalidate through tracked dependencies.")
                .css(bodyTextClass("text-base")))
            .child(
                div()
                    .css("grid grid-cols-3 gap-3")
                    .child(cssPill("Mutable", "set, update, subscribe"))
                    .child(cssPill("Computed", "lazy get with cached result"))
                    .child(cssPill("Nested", "computed values can depend on computed values"))
                    .child(cssPill("Invalidation", "dependency changes mark cache stale"))
                    .child(cssPill("Exceptions", "failed evaluations do not publish values"))
                    .child(cssPill("Cycles", "ComputedCycleException"))
            )
            .child(codeBlock(signalsCode()))
            .child(a("Open signals example")
                .attr("href", "/signals")
                .css("text-sm font-semibold text-emerald-700"));
    }

    private Node routerSection() {
        return section()
            .css(panelClass("p-6 flex flex-col gap-4"))
            .child(h2("Route sources and AOT metadata").css(theme.darkMode() ? "text-2xl font-bold text-slate-100" : "text-2xl font-bold text-slate-900"))
            .child(p("Routes can come from reflection-based scanning or explicit RouteSource implementations. ManualRouteSource is the runtime shape future generated AOT metadata will target.")
                .css(bodyTextClass("text-base")))
            .child(
                div()
                    .css("grid grid-cols-3 gap-3")
                    .child(cssPill("Manual", "explicit routes without scanning"))
                    .child(cssPill("Reflection", "ReflectionPageScanner for development"))
                    .child(cssPill("AOT", "generated RouteSource direction"))
                    .child(cssPill("Ordering", "registration order is preserved"))
                    .child(cssPill("Duplicates", "startup fails explicitly"))
                    .child(cssPill("Validation", "render methods and paths are checked"))
            )
            .child(codeBlock(routerCode()));
    }

    private Node performanceSection() {
        return section()
            .css(tonePanelClass("primary", "p-6 flex flex-col gap-4"))
            .child(h2("Production cache and runtime performance").css(toneTitleClass("primary", "text-2xl")))
            .child(p("Internal UJFE assets return ETag and Cache-Control headers in every adapter. Page responses use the route RenderMode so applications can declare cache intent without coupling the page to Netty, Servlet, Spring, or a CDN.")
                .css(bodyTextClass("text-base")))
            .child(
                div()
                    .css("grid grid-cols-3 gap-3")
                    .child(cssPill("/_ujfe/client.js", "short public cache with ETag"))
                    .child(cssPill("/_ujfe/dev.js", "no-cache with ETag"))
                    .child(cssPill("/_ujfe/css", "private revalidation"))
                    .child(cssPill("RenderMode.dynamic", "Cache-Control: no-store"))
                    .child(cssPill("staticPage", "public max-age for public HTML"))
                    .child(cssPill("staticShell", "private max-age for app shells"))
            )
            .child(codeBlock(performanceCode()));
    }

    private Node runtimeActionsSection() {
        return section()
            .css(tonePanelClass("indigo", "p-6 flex flex-col gap-4"))
            .child(h2("Runtime extension points").css(toneTitleClass("indigo", "text-2xl")))
            .child(p("Register server-side Java actions for rendering, live events, errors, and document head contributions without introducing Spring, Servlet, or Netty coupling into the core API.")
                .css(bodyTextClass("text-base")))
            .child(
                div()
                    .css("grid grid-cols-3 gap-3")
                    .child(cssPill("Rendering", "beforeRender and afterRender"))
                    .child(cssPill("Events", "beforeEvent and afterEvent"))
                    .child(cssPill("Errors", "onError with runtime phase metadata"))
                    .child(cssPill("Head", "ordered meta, link, style, script, and custom nodes"))
                    .child(cssPill("Ordering", "ActionOrder preserves deterministic execution"))
                    .child(cssPill("Runtime agnostic", "wired through LiveSessionConfig"))
            )
            .child(codeBlock(runtimeActionsCode()))
            .child(a("Open runtime actions example")
                .attr("href", "/runtime-actions")
                .css("text-sm font-semibold text-indigo-700"));
    }

    private Node lifecycleSection() {
        return section()
            .css(tonePanelClass("indigo", "p-6 flex flex-col gap-4"))
            .child(h2("Server-side lifecycle").css(toneTitleClass("indigo", "text-2xl")))
            .child(p("Lifecycle callbacks are integrated into LiveSession rendering. Stable component instances mount once, route transitions unmount removed instances, and session shutdown cleans up mounted components.")
                .css(bodyTextClass("text-base")))
            .child(
                div()
                    .css("grid grid-cols-3 gap-3")
                    .child(cssPill("Mount", "onMount once per instance"))
                    .child(cssPill("Unmount", "onUnmount on route change or close"))
                    .child(cssPill("Identity", "Java object identity"))
                    .child(cssPill("Nested", "component(...) tracks child components"))
                    .child(cssPill("Errors", "RuntimePhase.LIFECYCLE"))
                    .child(cssPill("Cleanup", "reverse mount order"))
            )
            .child(codeBlock(lifecycleCode()))
            .child(a("Open lifecycle example")
                .attr("href", "/lifecycle")
                .css("text-sm font-semibold text-indigo-700"));
    }

    private Node restSection() {
        return section()
            .css(tonePanelClass("secondary", "p-6 flex flex-col gap-4"))
            .child(h2("Components with REST APIs").css(toneTitleClass("secondary", "text-2xl")))
            .child(p("The core module includes RestClient, based on Java HttpClient. The component below queries BrasilAPI and uses the result to populate a select.")
                .css(bodyTextClass("text-base")))
            .child(bankSelect.render())
            .child(codeBlock(restCode()));
    }

    private Node cliSection() {
        return section()
            .css(tonePanelClass("primary", "p-6 flex flex-col gap-4"))
            .child(h2("CLI").css(toneTitleClass("primary", "text-2xl")))
            .child(p("The convert command migrates plain HTML into UJFE Java DSL pages with preserved classes and attributes, strict malformed HTML errors, optional unsafe fallback, CSS extraction, and deterministic component methods for larger pages.")
                .css(bodyTextClass("text-base")))
            .child(
                div()
                    .css("grid grid-cols-2 gap-3")
                    .child(
                        div()
                            .css(tonePanelClass("primary", "p-3 flex flex-col gap-2"))
                            .child(h3("Command").css(toneTitleClass("primary", "text-lg")))
                            .child(codeBlock(cliCommandCode()))
                    )
                    .child(
                        div()
                            .css(tonePanelClass("primary", "p-3 flex flex-col gap-2"))
                            .child(h3("HTML input").css(toneTitleClass("primary", "text-lg")))
                            .child(codeBlock(cliInputCode()))
                    )
            )
            .child(h3("Generated output").css(toneTitleClass("primary", "text-lg")))
            .child(codeBlock(cliOutputCode()));
    }

    private Node devPreviewSection() {
        return section()
            .css(tonePanelClass("primary", "p-6 flex flex-col gap-4"))
            .child(h2("Visual Dev Preview").css(toneTitleClass("primary", "text-2xl")))
            .child(p("In the example app, the Dev Preview script is loaded but the panel starts disabled behind an explicit feature toggle. When the toggle is enabled, the floating panel lets developers select elements, inspect tag/id, and test CSS classes directly in the browser without changing Java.")
                .css(bodyTextClass("text-base")))
            .child(codeBlock(devPreviewCode()));
    }

    private Node docCard(String title, String description, String code, Node preview) {
        return div()
            .css(panelClass("p-5 flex flex-col gap-3"))
            .child(h3(title).css(sectionTitleClass("text-xl")))
            .child(p(description).css(bodyTextClass("text-sm")))
            .child(div().css(theme.darkMode()
                ? "rounded-md border border-slate-800 bg-slate-950 p-3 flex flex-col gap-2"
                : "rounded-md border border-slate-200 bg-slate-50 p-3 flex flex-col gap-2")
                .child(preview))
            .child(codeBlock(code));
    }

    private Node cssPill(String title, String body) {
        return div()
            .css(theme.darkMode()
                ? "rounded-lg border border-primary-800 bg-slate-950 p-3 flex flex-col gap-1"
                : "rounded-lg border border-primary-200 bg-white p-3 flex flex-col gap-1")
            .child(strong(title).css(theme.darkMode() ? "text-primary-200" : "text-primary-700"))
            .child(span(body).css(theme.darkMode() ? "text-xs text-slate-300 font-mono" : "text-xs text-slate-600 font-mono"));
    }

    private Node codeBlock(String codeSample) {
        return pre()
            .css("overflow-x-auto rounded-lg border border-zinc-800 bg-zinc-950 text-zinc-50 p-4 text-sm font-mono")
            .child(code(codeSample).css("font-mono"));
    }

    private String panelClass(String extra) {
        return (theme.darkMode()
            ? "rounded-lg border border-slate-800 bg-slate-900 shadow-sm "
            : "rounded-lg border border-slate-200 bg-white shadow-sm ") + extra;
    }

    private String tonePanelClass(String tone, String extra) {
        return (theme.darkMode()
            ? "rounded-lg border border-" + tone + "-900 bg-slate-900 shadow-sm "
            : "rounded-lg border border-" + tone + "-200 bg-white shadow-sm ") + extra;
    }

    private String themeButtonClass(String tone) {
        return theme.darkMode()
            ? "px-4 py-2 rounded border border-" + tone + "-800 bg-slate-950 text-" + tone + "-200 font-semibold"
            : "px-4 py-2 rounded border border-" + tone + "-200 bg-" + tone + "-50 text-" + tone + "-700 font-semibold";
    }

    private String sectionTitleClass(String size) {
        return theme.darkMode()
            ? size + " font-bold text-primary-200"
            : size + " font-bold text-primary-700";
    }

    private String toneTitleClass(String tone, String size) {
        return theme.darkMode()
            ? size + " font-bold text-" + tone + "-200"
            : size + " font-bold text-" + tone + "-700";
    }

    private String bodyTextClass(String size) {
        return theme.darkMode()
            ? size + " text-slate-300 leading-relaxed"
            : size + " text-slate-700 leading-relaxed";
    }

    private String labelClass() {
        return theme.darkMode()
            ? "text-sm font-semibold text-slate-200"
            : "text-sm font-semibold text-slate-700";
    }

    private String checkLabelClass() {
        return theme.darkMode()
            ? "flex items-center gap-2 text-sm text-slate-300"
            : "flex items-center gap-2 text-sm text-slate-700";
    }

    private String inputClass() {
        return theme.darkMode()
            ? "w-full rounded border border-slate-700 bg-slate-950 p-2 text-sm text-slate-100"
            : "w-full rounded border border-slate-200 bg-white p-2 text-sm text-slate-900";
    }

    private Node layoutPreview() {
        return div()
            .css("flex flex-col gap-2")
            .child(h1("h1").css("text-3xl font-bold text-primary-700"))
            .child(h2("h2").css("text-2xl font-semibold"))
            .child(h3("h3").css("text-lg font-semibold text-secondary-700"))
            .child(p("p() with ").child(strong("strong"))
                .child(", ")
                .child(em("em"))
                .child(", and ")
                .child(u("u")))
            .child(pre().css("rounded bg-zinc-950 text-zinc-50 p-2 text-xs font-mono")
                .child(code("pre().child(code(...))").css("font-mono")));
    }

    private Node listAndMediaPreview() {
        return div()
            .css("flex flex-col gap-2")
            .child(a("Home link").href("/")
                .title("Back to home")
                .css("text-primary-700 font-semibold"))
            .child(ul().css("flex flex-col gap-1 text-sm")
                .child(le().child("ul + le/li"))
                .child(le().child("item two")))
            .child(ol().css("flex flex-col gap-1 text-sm")
                .child(le().child("ol + le/li"))
                .child(le().child("item two")))
            .child(dl().css("text-sm")
                .child(dt().child(strong("dt()")))
                .child(dd("inside dl()")))
            .child(canvas().width(240)
                .height(72)
                .ariaLabel("Empty canvas")
                .css(theme.darkMode()
                    ? "border border-slate-700 rounded bg-slate-950"
                    : "border border-slate-200 rounded bg-white"))
            .child(video().src("/assets/demo.mp4")
                .controls(true)
                .preload("none")
                .css("w-full rounded border border-slate-200"))
            .child(audio().controls(true)
                .preload("none")
                .child(source().src("/assets/audio.mp3")
                    .type("audio/mpeg")))
            .child(img().src("data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==")
                .alt("pixel")
                .css("border border-slate-200 rounded"));
    }

    private Node modernHtmlPreview() {
        return div()
            .css("flex flex-col gap-2")
            .child(details()
                .attr("open", true)
                .child(summary("details + summary"))
                .child(p("Expanded content without JavaScript.")))
            .child(tagTablePreview())
            .child(template().child(slot().attr("name", "actions")))
            .child(Element.of("future-html-element")
                .attr("data-ready", true)
                .child("Future tag through Element.of"))
            .child(Element.of("my-card")
                .attr("data-kind", "component")
                .child("Custom element with a hyphen"))
            .child(Element.of("math")
                .child(Element.mathMl("mi")
                    .child("x"))
                .child(Element.mathMl("mo")
                    .child("="))
                .child(Element.mathMl("mn")
                    .child("1")));
    }

    private Node tagTablePreview() {
        var rows = new String[][]{
            {"dialog", "Native interaction"},
            {"template", "Inert content"},
            {"slot", "Web Components"},
            {"popover", "Modern interaction"}
        };
        var body = tbody();
        for (var row : rows) {
            body.child(tr().child(td(row[0]))
                .child(td(row[1])));
        }

        return table()
            .css("w-full text-sm border border-slate-200")
            .child(caption("Real table generated with a Java loop").css("text-left font-semibold"))
            .child(colgroup().child(col().attr("span", "1"))
                .child(col().attr("span", "1")))
            .child(thead().child(tr().child(th("Tag"))
                .child(th("Use"))))
            .child(body)
            .child(tfoot().child(tr().child(td().attr("colspan", "2")
                .child("Rows generated in Java"))));
    }

    private Node liveEventPreview() {
        return div()
            .css("flex flex-col gap-2")
            .child(button("onClick").css("px-3 py-2 rounded bg-primary-700 text-white font-semibold")
                .onClick(() -> {
                }))
            .child(inputText().placeholder("onInput")
                .css("w-full rounded border border-slate-200 p-2 text-sm")
                .onInput(() -> {
                }))
            .child(select().css("w-full rounded border border-slate-200 p-2 text-sm")
                .onChange(() -> {
                })
                .child(option("onChange").value("change")))
            .child(form().onSubmit(() -> {
                })
                .child(button("onSubmit").type("submit")
                    .css("px-3 py-2 rounded bg-secondary-700 text-white font-semibold")));
    }

    private String factoryIndex() {
        return "import ujfe.core.Element;\n\n"
            + "import static ujfe.core.UI.*;\n\n"
            + "// Safe by default: text content is escaped.\n"
            + "p(\"<script>\")\n\n"
            + "// Explicit unsafe escape hatch for trusted, pre-sanitized HTML only.\n"
            + "unsafeHtml(\"<p>Trusted HTML</p>\")\n\n"
            + "// Generic core: supports current tags, future tags, custom elements, and Web Components.\n"
            + "Element.of(\"dialog\").attr(\"open\", true)\n"
            + "Element.of(\"future-html-element\").attr(\"data-ready\", true)\n"
            + "Element.of(\"my-card\").attr(\"data-kind\", \"component\")\n"
            + "Element.svg(\"path\").attr(\"d\", \"M0 0h10v10H0z\")\n"
            + "Element.mathMl(\"mi\").child(\"x\")\n\n"
            + "// Document and layout.\n"
            + "html(), head(), body(), title(), meta(), link(), style(), script(), base()\n"
            + "div(), header(), main(), aside(), section(), article(), nav(), footer(), address()\n"
            + "figure(), figcaption(), details(), summary(), dialog(), modal()\n\n"
            + "// Text.\n"
            + "text(), span(), h1(), h2(), h3(), h4(), h5(), h6(), p(), br(), hr()\n"
            + "strong(), em(), b(), i(), u(), small(), mark(), abbr(), cite(), pre(), code(), blockquote(), q()\n\n"
            + "// Links, lists, media, tables, and templates.\n"
            + "a(), ul(), ol(), li(), le(), dl(), dt(), dd()\n"
            + "img(), picture(), source(), track(), audio(), video(), canvas(), svg(), math()\n"
            + "map(), area(), iframe(), object(), embed(), param(), table(), thead(), tbody(), tfoot(), tr(), td(), th(), caption(), colgroup(), col()\n"
            + "template(), slot()\n\n"
            + "// Forms and attributes.\n"
            + "form(), label(), input(), inputText(), inputNumber(), inputPassword()\n"
            + "inputEmail(), inputSearch(), inputTel(), inputUrl(), inputHidden()\n"
            + "inputDate(), inputTime(), inputDateTimeLocal(), inputMonth(), inputWeek()\n"
            + "inputColor(), inputFile(), inputRange(), inputButton(), inputImage()\n"
            + "inputSubmit(), inputReset(), checkbox(), radio(), select(), option()\n"
            + "optgroup(), textarea(), fieldset(), legend(), datalist(), output()\n"
            + "progress(), meter(), button()\n\n"
            + "input().attr(\"placeholder\", \"Name\").attr(\"required\", true)\n";
    }

    private String themeCode() {
        return "AppTheme appTheme = new AppTheme();\n"
            + "Router router = new Router().register(new DocumentationPage(appTheme));\n"
            + "LiveSessionConfig config = LiveSessionConfig.builder()\n"
            + "    .themeSupplier(appTheme::cssTheme)\n"
            + "    .lang(\"en\")\n"
            + "    .title(\"UJFE Docs\")\n"
            + "    .build();\n"
            + "var liveSession = new LiveSession(router, config);\n\n"
            + "public CssTheme cssTheme() {\n"
            + "    return CssTheme.of(primaryColor.get(), secondaryColor.get());\n"
            + "}\n\n"
            + "div().css(\"bg-primary-50 border border-primary-200 text-primary-700\")\n"
            + "button(\"Switch theme\").onClick(appTheme::useRoseAmber)\n";
    }

    private String modernJavaCode() {
        return "import java.util.List;\n"
            + "import java.util.stream.Collectors;\n"
            + "import ujfe.core.ClientState;\n"
            + "import ujfe.core.Node;\n"
            + "import ujfe.live.LiveSession;\n"
            + "import ujfe.router.Router;\n"
            + "import static ujfe.core.UI.*;\n\n"
            + "public final class Demo {\n"
            + "    static final class Metric {\n"
            + "        private final String label;\n"
            + "        private final int value;\n"
            + "        private final String status;\n\n"
            + "        Metric(String label, int value, String status) {\n"
            + "            this.label = label;\n"
            + "            this.value = value;\n"
            + "            this.status = status;\n"
            + "        }\n"
            + "    }\n\n"
            + "    static final class MetricsPage {\n"
            + "        private final List<Metric> metrics;\n\n"
            + "        MetricsPage(List<Metric> metrics) {\n"
            + "            this.metrics = metrics;\n"
            + "        }\n\n"
            + "        public Node render() {\n"
            + "            var rows = metrics.stream()\n"
            + "                .map(metric -> tr()\n"
            + "                    .child(td(metric.label))\n"
            + "                    .child(td(String.valueOf(metric.value)).css(tone(metric.status))))\n"
            + "                .collect(Collectors.toList());\n\n"
            + "            return table()\n"
            + "                .child(caption(\"Metrics\"))\n"
            + "                .child(thead().child(tr().child(th(\"Label\")).child(th(\"Value\"))))\n"
            + "                .child(tbody().children(rows));\n"
            + "        }\n"
            + "    }\n\n"
            + "    public static void main(String[] args) {\n"
            + "        var metrics = List.of(\n"
            + "            new Metric(\"Users\", 42, \"ok\"),\n"
            + "            new Metric(\"Errors\", 2, \"warn\")\n"
            + "        );\n\n"
            + "        var router = new Router().register(\"/\", () -> new MetricsPage(metrics));\n"
            + "        var liveSession = new LiveSession(router);\n"
            + "        System.out.println(liveSession.renderDocument(\"/\", ClientState.empty()));\n"
            + "    }\n\n"
            + "    private static String tone(String status) {\n"
            + "        if (\"ok\".equals(status)) return \"text-emerald-700\";\n"
            + "        if (\"warn\".equals(status)) return \"text-amber-700\";\n"
            + "        return \"text-slate-700\";\n"
            + "    }\n"
            + "}\n";
    }

    private String layoutCode() {
        return "div()\n"
            + "    .child(header().child(h1(\"Title\")))\n"
            + "    .child(main().child(section().child(h2(\"Section\"))))\n"
            + "    .child(article().child(h3(\"Article\")).child(p(\"Content\")))\n"
            + "    .child(aside().child(nav().child(a(\"Docs\").attr(\"href\", \"/docs\"))))\n"
            + "    .child(footer().child(address(\"contact@example.com\")))\n"
            + "    .child(p(\"Text\").child(br()).child(strong(\"strong\")).child(\" \").child(mark(\"marked\")))\n"
            + "    .child(pre().child(code(\"code\")));\n";
    }

    private String listAndMediaCode() {
        return "a(\"Home\").attr(\"href\", \"/\")\n"
            + "// Void elements render without closing tags and reject children.\n"
            + "img().src(\"/assets/logo.png\").alt(\"Logo\")\n"
            + "picture()\n"
            + "    .child(source().attr(\"media\", \"(min-width: 800px)\").src(\"/assets/hero-wide.webp\"))\n"
            + "    .child(img().src(\"/assets/hero.webp\").alt(\"Dashboard\"))\n"
            + "canvas().width(320).height(180).ariaLabel(\"Chart\")\n"
            + "video().src(\"/assets/demo.mp4\").poster(\"/assets/poster.png\").controls(true).preload(\"metadata\")\n"
            + "audio().controls(true)\n"
            + "    .child(source().src(\"/assets/audio.mp3\").type(\"audio/mpeg\"))\n"
            + "    .child(track().attr(\"kind\", \"captions\").attr(\"srclang\", \"en\").src(\"/assets/captions.vtt\"))\n"
            + "figure().child(svg().attr(\"viewBox\", \"0 0 10 10\")).child(figcaption(\"SVG\"))\n"
            + "map().attr(\"name\", \"primary-map\").child(area().attr(\"shape\", \"rect\").attr(\"coords\", \"0,0,20,20\").href(\"/docs\"))\n"
            + "iframe().src(\"/embedded\").attr(\"loading\", \"lazy\")\n"
            + "object().attr(\"data\", \"/assets/report.pdf\").type(\"application/pdf\")\n"
            + "embed().src(\"/assets/preview.pdf\").type(\"application/pdf\")\n"
            + "param().attr(\"name\", \"autoplay\").attr(\"value\", \"false\")\n"
            + "ul().child(le().child(\"Item\"))\n"
            + "ol().child(li().child(\"Item\"))\n"
            + "dl().child(dt(\"Term\")).child(dd(\"Description\"));\n";
    }

    private String modernHtmlCode() {
        return "final class Metric {\n"
            + "    final String name;\n"
            + "    final int value;\n"
            + "    final String status;\n\n"
            + "    Metric(String name, int value, String status) {\n"
            + "        this.name = name;\n"
            + "        this.value = value;\n"
            + "        this.status = status;\n"
            + "    }\n"
            + "}\n\n"
            + "var metrics = List.of(\n"
            + "    new Metric(\"Users\", 42, \"ok\"),\n"
            + "    new Metric(\"Errors\", 2, \"warn\")\n"
            + ");\n\n"
            + "var rows = metrics.stream()\n"
            + "    .map(metric -> {\n"
            + "        var tone = \"text-slate-700\";\n"
            + "        if (\"ok\".equals(metric.status)) tone = \"text-emerald-700\";\n"
            + "        if (\"warn\".equals(metric.status)) tone = \"text-amber-700\";\n"
            + "        return tr()\n"
            + "            .child(td(metric.name))\n"
            + "            .child(td(String.valueOf(metric.value)).css(tone));\n"
            + "    })\n"
            + "    .collect(Collectors.toList());\n\n"
            + "details().attr(\"open\", true)\n"
            + "    .child(summary(\"More\"))\n"
            + "    .child(p(\"Native content.\"))\n\n"
            + "Element.of(\"dialog\")\n"
            + "    .attr(\"open\", true)\n"
            + "    .child(p(\"Real HTML dialog\"))\n\n"
            + "Element.of(\"future-html-element\")\n"
            + "    .attr(\"data-ready\", true)\n\n"
            + "Element.of(\"my-card\")\n"
            + "    .attr(\"data-kind\", \"component\")\n"
            + "    .child(\"Custom element\")\n\n"
            + "table()\n"
            + "    .child(caption(\"Metrics\"))\n"
            + "    .child(colgroup().child(col()).child(col()))\n"
            + "    .child(thead().child(tr().child(th(\"Name\")).child(th(\"Value\"))))\n"
            + "    .child(tbody().children(rows))\n"
            + "    .child(tfoot().child(tr().child(td().attr(\"colspan\", \"2\").child(\"Generated with Java\"))))\n\n"
            + "template().child(slot().attr(\"name\", \"actions\"))\n"
            + "Element.of(\"math\").child(Element.mathMl(\"mi\").child(\"x\"))\n"
            + "div().attr(\"popover\", true).child(\"Popover content\");\n";
    }

    private String liveEventCode() {
        return "button(\"Save\").onClick(this::save)\n"
            + "inputText().onInput(this::markDirty)\n"
            + "select().onChange(this::reload)\n"
            + "form().onSubmit(this::submit);\n";
    }

    private String formCode() {
        return "form()\n"
            + "    .onSubmit(this::submit)\n"
            + "    .child(fieldset()\n"
            + "        .child(legend(\"Account\"))\n"
            + "        .child(label(\"Name\").forId(\"name\")))\n"
            + "    .child(input().attr(\"type\", \"text\").attr(\"id\", \"name\").attr(\"name\", \"name\").attr(\"required\", true))\n"
            + "    .child(inputNumber().attr(\"name\", \"age\").attr(\"min\", \"0\").attr(\"max\", \"120\"))\n"
            + "    .child(inputPassword().name(\"password\"))\n"
            + "    .child(checkbox().name(\"terms\").checked(true))\n"
            + "    .child(radio().name(\"plan\").value(\"pro\"))\n"
            + "    .child(select().child(optgroup().label(\"Stack\").child(option(\"Java\").value(\"java\"))))\n"
            + "    .child(input().attr(\"list\", \"languages\"))\n"
            + "    .child(datalist().id(\"languages\").child(option(\"Java\")).child(option(\"Kotlin\")))\n"
            + "    .child(textarea(\"Notes\").rows(3))\n"
            + "    .child(output(\"Ready\").name(\"status\"))\n"
            + "    .child(progress().attr(\"value\", \"70\").attr(\"max\", \"100\"))\n"
            + "    .child(meter().attr(\"value\", \"0.7\").attr(\"min\", \"0\").attr(\"max\", \"1\"))\n"
            + "    .child(button(\"Submit\").type(\"submit\"));\n";
    }

    private String cssCode() {
        return "div().css(\"flex flex-col gap-4 p-6 rounded-lg border shadow-sm\")\n"
            + "h2(\"Title\").css(\"text-2xl font-bold text-primary-700\")\n"
            + "p(\"Text\").css(\"text-sm leading-relaxed text-slate-700\")\n"
            + "button(\"Action\").css(\"px-4 py-2 rounded bg-secondary-700 text-white\")\n\n"
            + "// The renderer generates a gradual scale from base colors:\n"
            + "bg-primary-50, bg-primary-500, bg-primary-900\n"
            + "text-secondary-700, border-secondary-200\n\n"
            + "// External CSS: disable the internal renderer and use link/head.\n"
            + "// In the default Netty runtime, prefer same-origin stylesheets.\n"
            + "LiveSessionConfig.builder()\n"
            + "    .cssMode(CssMode.EXTERNAL)\n"
            + "    .externalStylesheet(\"/app.css\")\n"
            + "    .build();\n";
    }

    private String assetCode() {
        return "// UJFE renders asset references. It does not serve arbitrary filesystem files.\n"
            + "// Host /assets/** through Spring resources, a servlet container, a reverse proxy, or CDN.\n\n"
            + "LiveSessionConfig.builder()\n"
            + "    .cssMode(CssMode.EXTERNAL)\n"
            + "    .externalStylesheet(\"/assets/app.css\")\n"
            + "    .head(script().src(\"/assets/app.js\").attr(\"defer\", true))\n"
            + "    .head(link().attr(\"rel\", \"preload\").href(\"/assets/app.woff2\").attr(\"as\", \"font\"))\n"
            + "    .build();\n\n"
            + "img().src(\"/assets/logo.svg\").alt(\"Product logo\")\n"
            + "picture()\n"
            + "    .child(source().attr(\"media\", \"(min-width: 960px)\").src(\"/assets/hero-wide.webp\"))\n"
            + "    .child(img().src(\"/assets/hero.webp\").alt(\"Dashboard\"))\n"
            + "video().src(\"/assets/demo.mp4\").poster(\"/assets/poster.webp\").controls(true).preload(\"metadata\")\n"
            + "audio().controls(true).child(source().src(\"/assets/audio.mp3\").type(\"audio/mpeg\"))\n"
            + "canvas().width(640).height(320).ariaLabel(\"Chart canvas\")\n"
            + "object().attr(\"data\", \"/assets/report.pdf\").type(\"application/pdf\")\n\n"
            + "// Metadata-only client module descriptor for app-owned JavaScript.\n"
            + "clientModule(\"charts\")\n"
            + "    .src(\"/assets/charts.js\")\n"
            + "    .action(\"render\", ChartRequest.class)\n"
            + "    .target(\"#chart\")\n"
            + "    .errorTarget(\"#chart-error\")\n"
            + "    .defer(true);\n";
    }

    private String performanceCode() {
        return "Router router = new Router()\n"
            + "    .register(new RouteDefinition(\"/\", HomePage::new)\n"
            + "        .withRenderMode(RenderMode.staticShell(Duration.ofSeconds(30))))\n"
            + "    .register(new RouteDefinition(\"/marketing\", MarketingPage::new)\n"
            + "        .withRenderMode(RenderMode.staticPage(Duration.ofMinutes(10))))\n"
            + "    .register(new RouteDefinition(\"/feed\", FeedPage::new)\n"
            + "        .withRenderMode(RenderMode\n"
            + "            .staleWhileRevalidate(Duration.ofSeconds(60), Duration.ofMinutes(5))\n"
            + "            .revalidateOn(\"feed.updated\")));\n\n"
            + "// Dynamic is the default and emits Cache-Control: no-store.\n"
            + "new RouteDefinition(\"/account\", AccountPage::new)\n"
            + "    .withRenderMode(RenderMode.dynamic());\n";
    }

    private String springCode() {
        return "// pom.xml: use dev.ujfe:ujfe-spring no app Spring Boot.\n"
            + "// Do not add ujfe-http unless you want a separate Netty server.\n\n"
            + "@Page(\"/\")\n"
            + "@Component\n"
            + "public final class HomePage {\n"
            + "    public Node render() {\n"
            + "        return main().child(h1(\"UJFE + Spring Boot\"));\n"
            + "    }\n"
            + "}\n\n"
            + "@Configuration\n"
            + "public class UjfeConfig {\n"
            + "    @Bean\n"
            + "    Router ujfeRouter(HomePage homePage) {\n"
            + "        return new Router().register(homePage);\n"
            + "    }\n\n"
            + "    @Bean\n"
            + "    LiveSessionConfig ujfeLiveSessionConfig() {\n"
            + "        return LiveSessionConfig.builder()\n"
            + "            .title(\"UJFE Spring Demo\")\n"
            + "            .build();\n"
            + "    }\n"
            + "}\n";
    }

    private String securityCode() {
        return "// Safe attributes:\n"
            + "input().attr(\"placeholder\", \"Name\").attr(\"required\", true)\n"
            + "div().attr(\"aria-label\", \"Close\")\n"
            + "div().attr(\"data-id\", \"123\")\n"
            + "div().attr(\"hx-get\", \"/fragment\")\n\n"
            + "// Blocked: inline event handler attributes (on*):\n"
            + "div().attr(\"onclick\", \"alert(1)\")  // throws\n"
            + "img().attr(\"onload\", \"steal()\")    // throws\n"
            + "// Use server-side live events instead:\n"
            + "button(\"Save\").on(\"click\", this::save)\n\n"
            + "// Safe URLs:\n"
            + "a(\"Safe\").href(\"/home\")\n"
            + "a(\"Safe\").href(\"https://example.com\")\n"
            + "a(\"Safe\").href(\"../settings\")\n"
            + "a(\"Safe\").href(\"#section\")\n"
            + "img().src(\"data:image/png;base64,...\").alt(\"Preview\")\n\n"
            + "// Blocked URL schemes:\n"
            + "a(\"XSS\").href(\"javascript:alert(1)\")  // always blocked\n"
            + "a(\"XSS\").href(\"vbscript:MsgBox(1)\")   // always blocked\n\n"
            + "// Configurable URL policy (startup):\n"
            + "UrlPolicy.setDefault(UrlPolicy.builder()\n"
            + "    .allowHttp()\n"
            + "    .allowMailto()\n"
            + "    .allowTel()\n"
            + "    .build());\n\n"
            + "// Safe text remains escaped:\n"
            + "p(\"<script>\") // renders &lt;script&gt;\n\n"
            + "// Trusted raw HTML must be explicit and reviewed:\n"
            + "unsafeHtml(\"<strong>Trusted fragment</strong>\")\n"
            + "UnsafeHtml.of(\"<p>Trusted CMS block</p>\")\n\n"
            + "// The server also emits headers such as CSP, nosniff,\n"
            + "// Referrer-Policy, Permissions-Policy, and frame-ancestors.\n";
    }

    private String signalsCode() {
        return "Signal<Integer> count = Signals.signal(1);\n"
            + "Signal<Integer> multiplier = Signals.signal(2);\n\n"
            + "Computed<Integer> doubled = Signals.computed(() -> count.get() * 2);\n"
            + "Computed<Integer> total = Signals.computed(() -> doubled.get() * multiplier.get());\n\n"
            + "// Lazy: nothing evaluates until get().\n"
            + "total.get();\n"
            + "total.get(); // cached\n\n"
            + "// Invalidation is lazy and collapsed.\n"
            + "count.set(2);\n"
            + "count.set(3);\n"
            + "total.get(); // one recomputation with latest values\n\n"
            + "total.subscribe(value -> audit(\"total=\" + value));\n";
    }

    private String routerCode() {
        return "ManualRouteSource routes = new ManualRouteSource()\n"
            + "    .register(\"/\", HomePage::new)\n"
            + "    .register(\"/dashboard\", DashboardPage::new);\n\n"
            + "Router router = new Router().register(routes);\n\n"
            + "// Development-time discovery remains available:\n"
            + "Router devRouter = new Router()\n"
            + "    .register(ReflectionPageScanner.forPackages(\"app.pages\"));\n\n"
            + "// Future AOT flow:\n"
            + "// @Page -> annotation processor -> generated RouteSource -> Router\n";
    }

    private String runtimeActionsCode() {
        return "RuntimeActionRegistry registry = RuntimeActionRegistry.builder()\n"
            + "    .beforeRender(ctx -> log(\"render \" + ctx.path()))\n"
            + "    .afterRender(result -> metrics(result.renderDuration()))\n"
            + "    .beforeEvent(ctx -> authorize(ctx.clientState()))\n"
            + "    .afterEvent(result -> audit(result.eventId()))\n"
            + "    .onError(error -> log(error.phase(), error.exception()))\n"
            + "    .contributeHead(head -> head.add(\n"
            + "        meta().attr(\"name\", \"robots\")\n"
            + "              .attr(\"content\", \"index,follow\")))\n"
            + "    .build();\n\n"
            + "LiveSessionConfig config = LiveSessionConfig.builder()\n"
            + "    .runtimeActions(registry)\n"
            + "    .build();\n";
    }

    private String lifecycleCode() {
        return "public final class ResourcePanel implements Component, Lifecycle {\n"
            + "    private final Signal<Boolean> open = Signals.signal(false);\n\n"
            + "    public void onMount() {\n"
            + "        open.set(true);\n"
            + "    }\n\n"
            + "    public void onUnmount() {\n"
            + "        open.set(false);\n"
            + "    }\n\n"
            + "    public Node render() {\n"
            + "        return p(() -> \"Resource: \" + (open.get() ? \"open\" : \"closed\"));\n"
            + "    }\n"
            + "}\n\n"
            + "private final ResourcePanel panel = new ResourcePanel();\n\n"
            + "public Node render() {\n"
            + "    return div().child(component(panel));\n"
            + "}\n";
    }

    private String restCode() {
        return "var client = RestClient.create();\n"
            + "var response = client\n"
            + "    .get(\"https://brasilapi.com.br/api/banks/v1\")\n"
            + "    .requireSuccessful();\n\n"
            + "final class Bank {\n"
            + "    final String label;\n"
            + "    final String selectValue;\n\n"
            + "    Bank(String label, String selectValue) {\n"
            + "        this.label = label;\n"
            + "        this.selectValue = selectValue;\n"
            + "    }\n"
            + "}\n\n"
            + "var banks = parseBanks(response.body());\n\n"
            + "select().children(banks.stream()\n"
            + "    .map(bank -> option(bank.label).value(bank.selectValue))\n"
            + "    .collect(Collectors.toList()));\n";
    }

    private String cliCommandCode() {
        return "ujfe convert page.html \\\n"
            + "  --output src/main/java/app/pages/Page.java \\\n"
            + "  --type html \\\n"
            + "  --comments drop \\\n"
            + "  --css extract \\\n"
            + "  --componentize\n";
    }

    private String cliInputCode() {
        return "<section class=\"p-4 flex flex-col gap-2\">\n"
            + "  <h1 title=\"Hero\">Hello UJFE</h1>\n"
            + "  <p>HTML converted to the Java DSL.</p>\n"
            + "  <button>Save</button>\n"
            + "</section>\n";
    }

    private String cliOutputCode() {
        return "package app.pages;\n\n"
            + "import static ujfe.core.UI.*;\n\n"
            + "import ujfe.core.Node;\n"
            + "import ujfe.router.Page;\n\n"
            + "@Page(\"/\")\n"
            + "public final class Page {\n\n"
            + "    public Node render() {\n"
            + "        return section()\n"
            + "                .attr(\"class\", \"p-4 flex flex-col gap-2\")\n"
            + "                .child(h1().attr(\"title\", \"Hero\").child(text(\"Hello UJFE\")))\n"
            + "                .child(p().child(text(\"HTML converted to the Java DSL.\")))\n"
            + "                .child(button().child(text(\"Save\")));\n"
            + "    }\n"
            + "}\n";
    }

    private String devPreviewCode() {
        return "LiveSessionConfig config = LiveSessionConfig.builder()\n"
            + "    .themeSupplier(appTheme::cssTheme)\n"
            + "    .devToolsEnabled(true)\n"
            + "    .build();\n"
            + "\n"
            + "// The page controls whether the panel is active:\n"
            + "div().attr(\"data-ujfe-dev-preview\", String.valueOf(devPreviewEnabled))\n\n"
            + "LiveSession liveSession = new LiveSession(router, config);\n\n"
            + "// When enabled, the HTML includes:\n"
            + "<script src=\"/_ujfe/dev.js\"></script>\n\n"
            + "// The panel lets you click elements and test CSS classes\n"
            + "// in the browser without writing manual JavaScript or Node/npm.\n";
    }
}
