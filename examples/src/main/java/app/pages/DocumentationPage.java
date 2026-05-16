package app.pages;

import app.AppTheme;
import app.components.BankSelectComponent;
import ujfe.core.Component;
import ujfe.html.Element;
import ujfe.html.Node;
import ujfe.router.Page;

import java.util.Objects;

import static ujfe.html.UI.*;

@Page("/docs")
public final class DocumentationPage implements Component {
    private final AppTheme theme;
    private final BankSelectComponent bankSelect = new BankSelectComponent();

    public DocumentationPage(AppTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    @Override
    public Node render() {
        return div()
                .css("min-h-screen bg-slate-50 text-slate-900")
                .child(topBar())
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
                                                .child(springSection())
                                                .child(securitySection())
                                                .child(restSection())
                                                .child(cliSection())
                                                .child(devPreviewSection())
                                )
                );
    }

    private Node topBar() {
        return header()
                .css("border-b border-primary-200 bg-white")
                .child(
                        div()
                                .css("max-w-7xl mx-auto p-6 flex items-center justify-between gap-4")
                                .child(
                                        div()
                                                .css("flex flex-col gap-1")
                                                .child(span("UJFE Docs").css("text-sm font-bold text-primary-700"))
                                                .child(h1("HTML, live UI, and security reference").css("text-3xl font-bold"))
                                                .child(p("A compact reference for building server-rendered interfaces with safe defaults.")
                                                        .css("text-sm text-slate-600"))
                                )
                                .child(
                                        nav()
                                                .css("flex items-center gap-2")
                                                .child(a("Example").attr("href", "/").css("px-3 py-2 rounded border border-slate-200 bg-white text-sm font-semibold text-slate-700"))
                                                .child(a("Docs").attr("href", "/docs").css("px-3 py-2 rounded bg-primary-700 text-white text-sm font-semibold"))
                                )
                );
    }

    private Node sidebar() {
        return aside()
                .css("rounded-lg border border-slate-200 bg-white p-4 shadow-sm")
                .child(nav()
                        .css("flex flex-col gap-3")
                        .child(h2("Documentation map").css("text-xl font-bold text-primary-700"))
                        .child(p("Visual API reference for elements, forms, CSS, integrations, and security behavior.")
                                .css("text-sm text-slate-700 leading-relaxed"))
                        .child(ul()
                                .css("flex flex-col gap-2 text-sm text-slate-700")
                                .child(le().child(strong("Theme")).child(" with dynamic primary/secondary palettes"))
                                .child(le().child(strong("Modern Java")).child(" with var, records, streams, switch expressions, and script-style source files"))
                                .child(le().child(strong("Elements")).child(" with API usage examples"))
                                .child(le().child(strong("Forms")).child(" with inputs and events"))
                                .child(le().child(strong("CSS")).child(" with utilities and color scales"))
                                .child(le().child(strong("Spring MVC")).child(" on the same Tomcat port"))
                                .child(le().child(strong("Security")).child(" with escaping, safe URLs, raw HTML boundaries, and headers"))
                                .child(le().child(strong("REST")).child(" with a select populated from BrasilAPI"))
                                .child(le().child(strong("CLI")).child(" with HTML-to-UJFE conversion"))
                                .child(le().child(strong("Dev Preview")).child(" with a visual inspector"))));
    }

    private Node introSection() {
        return section()
                .css("rounded-lg border border-primary-200 bg-white p-6 shadow-sm flex flex-col gap-4")
                .child(h2("How to read this page").css("text-2xl font-bold text-primary-700"))
                .child(p("Each block shows the rendered result and the equivalent Java code. The DSL uses Element.of(...) as universal support for HTML and Web Components, Element.svg(...) and Element.mathMl(...) for namespaced generic tags, helpers as convenience methods, and generic attributes through attr(...).")
                        .css("text-base text-slate-700 leading-relaxed"))
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
                .css("rounded-lg border border-primary-200 bg-white p-6 shadow-sm flex flex-col gap-4")
                .child(h2("Dynamic theme").css("text-2xl font-bold text-primary-700"))
                .child(p(() -> "Primary: " + theme.primaryColor() + " | Secondary: " + theme.secondaryColor())
                        .css("text-sm font-mono text-slate-700"))
                .child(
                        div()
                                .css("flex flex-wrap gap-2")
                                .child(button("Blue + Emerald")
                                        .css("px-4 py-2 rounded bg-primary-700 text-white font-semibold")
                                        .onClick(theme::useBlueEmerald))
                                .child(button("Rose + Amber")
                                        .css("px-4 py-2 rounded border border-primary-200 bg-primary-50 text-primary-700 font-semibold")
                                        .onClick(theme::useRoseAmber))
                                .child(button("Indigo + Cyan")
                                        .css("px-4 py-2 rounded border border-secondary-200 bg-secondary-50 text-secondary-700 font-semibold")
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
                .css("rounded-lg border border-secondary-200 bg-white p-6 shadow-sm flex flex-col gap-4")
                .child(h2("Modern Java examples").css("text-2xl font-bold text-secondary-700"))
                .child(p("UJFE examples prefer modern Java syntax where it improves clarity: var for local values, records for small immutable view models, streams and lambdas for declarative collection mapping, and switch expressions for compact branching.")
                        .css("text-base text-slate-700 leading-relaxed"))
                .child(codeBlock(modernJavaCode()));
    }

    private Node colorScale(String title, String palette) {
        return div()
                .css("rounded-lg border border-slate-200 bg-white p-3 flex flex-col gap-2")
                .child(h3(title).css("text-lg font-bold text-slate-900"))
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
                .css("rounded-lg border border-secondary-200 bg-white p-6 shadow-sm flex flex-col gap-4")
                .child(h2("Forms").css("text-2xl font-bold text-secondary-700"))
                .child(p("The DSL covers form elements with helpers, but attributes such as required, placeholder, min, and max can also be declared directly with attr(...).")
                        .css("text-base text-slate-700 leading-relaxed"))
                .child(
                        form()
                                .css("grid grid-cols-2 gap-3")
                                .onSubmit(() -> {
                                })
                                .child(label("Text").forId("doc-text").css("text-sm font-semibold text-slate-700"))
                                .child(inputText().id("doc-text").name("text").placeholder("inputText()").css("w-full rounded border border-slate-200 bg-white p-2 text-sm"))
                                .child(label("Number").forId("doc-number").css("text-sm font-semibold text-slate-700"))
                                .child(inputNumber().id("doc-number").name("number").min("0").max("99").css("w-full rounded border border-slate-200 bg-white p-2 text-sm"))
                                .child(label("Password").forId("doc-password").css("text-sm font-semibold text-slate-700"))
                                .child(inputPassword().id("doc-password").name("password").placeholder("inputPassword()").css("w-full rounded border border-slate-200 bg-white p-2 text-sm"))
                                .child(label("Select").forId("doc-select").css("text-sm font-semibold text-slate-700"))
                                .child(select().id("doc-select").name("select").css("w-full rounded border border-slate-200 bg-white p-2 text-sm")
                                        .child(option("Java").value("java"))
                                        .child(option("UJFE").value("ujfe").selected(true)))
                                .child(label().css("flex items-center gap-2 text-sm text-slate-700").child(checkbox().name("check").checked(true)).child("checkbox()"))
                                .child(label().css("flex items-center gap-2 text-sm text-slate-700").child(radio().name("radio").value("a").checked(true)).child("radio()"))
                                .child(textarea("textarea()").name("message").rows(3).css("w-full rounded border border-slate-200 bg-white p-2 text-sm"))
                                .child(button("Submit live").type("submit").css("px-4 py-2 rounded bg-secondary-700 text-white font-semibold"))
                )
                .child(codeBlock(formCode()));
    }

    private Node cssSection() {
        return section()
                .css("rounded-lg border border-primary-200 bg-primary-50 p-6 shadow-sm flex flex-col gap-4")
                .child(h2("Internal or external CSS").css("text-2xl font-bold text-primary-700"))
                .child(p("INTERNAL mode keeps the current server-side utility renderer. EXTERNAL mode disables UJFE CSS generation and lets teams use Tailwind, Bootstrap, plain CSS, or design systems through regular head links.")
                        .css("text-base text-slate-700 leading-relaxed"))
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

    private Node springSection() {
        return section()
                .css("rounded-lg border border-secondary-200 bg-white p-6 shadow-sm flex flex-col gap-4")
                .child(h2("Spring Boot on the same port").css("text-2xl font-bold text-secondary-700"))
                .child(p("Use ujfe-spring when the Spring Boot application must serve UJFE pages, live events, and internal assets through the same DispatcherServlet/Tomcat port. The ujfe-http module remains available for standalone Netty mode.")
                        .css("text-base text-slate-700 leading-relaxed"))
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
                .css("rounded-lg border border-rose-200 bg-rose-50 p-6 shadow-sm flex flex-col gap-4")
                .child(h2("Security by default").css("text-2xl font-bold text-rose-700"))
                .child(p("Text and attributes are escaped during SSR. URL attributes such as href, src, action, and poster reject dangerous protocols before rendering. Trusted raw HTML has a deliberately unsafe name so security review can find it.")
                        .css("text-base text-slate-700 leading-relaxed"))
                .child(
                        div()
                                .css("grid grid-cols-3 gap-3")
                                .child(cssPill("A03 Injection", "HTML/attribute escaping and safe URLs"))
                                .child(cssPill("A05 Misconfiguration", "CSP, nosniff, frame-ancestors"))
                                .child(cssPill("Unsafe raw HTML", "only unsafeHtml(...) bypasses escaping"))
                )
                .child(
                        div()
                                .css("grid grid-cols-2 gap-3")
                                .child(
                                        div()
                                                .css("rounded-lg border border-emerald-200 bg-white p-4 flex flex-col gap-2")
                                                .child(h3("Escaped path").css("text-lg font-bold text-emerald-700"))
                                                .child(p("Use normal text and element helpers for application UI, user content, and request data.")
                                                        .css("text-sm text-slate-700 leading-relaxed"))
                                                .child(codeBlock("p(\"<script>\")\n// <p>&lt;script&gt;</p>\n"))
                                )
                                .child(
                                        div()
                                                .css("rounded-lg border border-rose-200 bg-white p-4 flex flex-col gap-2")
                                                .child(h3("Unsafe path").css("text-lg font-bold text-rose-700"))
                                                .child(p("Use only for trusted, pre-sanitized fragments where escaping would be incorrect.")
                                                        .css("text-sm text-slate-700 leading-relaxed"))
                                                .child(codeBlock("unsafeHtml(\"<strong>trusted</strong>\")\n// <strong>trusted</strong>\n"))
                                )
                )
                .child(codeBlock(securityCode()));
    }

    private Node restSection() {
        return section()
                .css("rounded-lg border border-secondary-200 bg-white p-6 shadow-sm flex flex-col gap-4")
                .child(h2("Components with REST APIs").css("text-2xl font-bold text-secondary-700"))
                .child(p("The core module includes RestClient, based on Java HttpClient. The component below queries BrasilAPI and uses the result to populate a select.")
                        .css("text-base text-slate-700 leading-relaxed"))
                .child(bankSelect.render())
                .child(codeBlock(restCode()));
    }

    private Node cliSection() {
        return section()
                .css("rounded-lg border border-primary-200 bg-primary-50 p-6 shadow-sm flex flex-col gap-4")
                .child(h2("CLI").css("text-2xl font-bold text-primary-700"))
                .child(p("The first CLI command converts plain HTML to a Java UJFE page. React/JSX stays outside the MVP because it requires dynamic props, hooks, conditionals, map, and external component handling.")
                        .css("text-base text-slate-700 leading-relaxed"))
                .child(
                        div()
                                .css("grid grid-cols-2 gap-3")
                                .child(
                                        div()
                                                .css("rounded-lg border border-primary-200 bg-white p-3 flex flex-col gap-2")
                                                .child(h3("Command").css("text-lg font-bold text-primary-700"))
                                                .child(codeBlock(cliCommandCode()))
                                )
                                .child(
                                        div()
                                                .css("rounded-lg border border-primary-200 bg-white p-3 flex flex-col gap-2")
                                                .child(h3("HTML input").css("text-lg font-bold text-primary-700"))
                                                .child(codeBlock(cliInputCode()))
                                )
                )
                .child(h3("Generated output").css("text-lg font-bold text-primary-700"))
                .child(codeBlock(cliOutputCode()));
    }

    private Node devPreviewSection() {
        return section()
                .css("rounded-lg border border-primary-200 bg-white p-6 shadow-sm flex flex-col gap-4")
                .child(h2("Visual Dev Preview").css("text-2xl font-bold text-primary-700"))
                .child(p("In the example app, the server starts with Dev Preview enabled. The floating panel lets developers select elements, inspect tag/id, and test CSS classes directly in the browser without changing Java.")
                        .css("text-base text-slate-700 leading-relaxed"))
                .child(codeBlock(devPreviewCode()));
    }

    private Node docCard(String title, String description, String code, Node preview) {
        return div()
                .css("rounded-lg border border-slate-200 bg-white p-5 shadow-sm flex flex-col gap-3")
                .child(h3(title).css("text-xl font-bold text-primary-700"))
                .child(p(description).css("text-sm text-slate-700 leading-relaxed"))
                .child(div().css("rounded-md border border-slate-200 bg-slate-50 p-3 flex flex-col gap-2").child(preview))
                .child(codeBlock(code));
    }

    private Node cssPill(String title, String body) {
        return div()
                .css("rounded-lg border border-primary-200 bg-white p-3 flex flex-col gap-1")
                .child(strong(title).css("text-primary-700"))
                .child(span(body).css("text-xs text-slate-600 font-mono"));
    }

    private Node codeBlock(String codeSample) {
        return pre()
                .css("overflow-x-auto rounded-lg bg-zinc-950 text-zinc-50 p-4 text-sm font-mono")
                .child(code(codeSample).css("font-mono"));
    }

    private Node layoutPreview() {
        return div()
                .css("flex flex-col gap-2")
                .child(h1("h1").css("text-3xl font-bold text-primary-700"))
                .child(h2("h2").css("text-2xl font-semibold"))
                .child(h3("h3").css("text-lg font-semibold text-secondary-700"))
                .child(p("p() with ").child(strong("strong")).child(", ").child(em("em")).child(", and ").child(u("u")))
                .child(pre().css("rounded bg-zinc-950 text-zinc-50 p-2 text-xs font-mono").child(code("pre().child(code(...))").css("font-mono")));
    }

    private Node listAndMediaPreview() {
        return div()
                .css("flex flex-col gap-2")
                .child(a("Home link").href("/").title("Back to home").css("text-primary-700 font-semibold"))
                .child(ul().css("flex flex-col gap-1 text-sm").child(le().child("ul + le/li")).child(le().child("item two")))
                .child(ol().css("flex flex-col gap-1 text-sm").child(le().child("ol + le/li")).child(le().child("item two")))
                .child(dl().css("text-sm").child(dt().child(strong("dt()"))).child(dd("inside dl()")))
                .child(canvas().width(240).height(72).ariaLabel("Empty canvas").css("border border-slate-200 rounded bg-white"))
                .child(video().src("/demo.mp4").poster("/poster.png").controls(true).preload("metadata").css("w-full rounded border border-slate-200"))
                .child(audio().controls(true).preload("metadata").child(source().src("/audio.mp3").type("audio/mpeg")))
                .child(img().src("data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==").alt("pixel").css("border border-slate-200 rounded"));
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
                .child(Element.of("future-html-element").attr("data-ready", true).child("Future tag through Element.of"))
                .child(Element.of("my-card").attr("data-kind", "component").child("Custom element with a hyphen"))
                .child(Element.of("math")
                        .child(Element.mathMl("mi").child("x"))
                        .child(Element.mathMl("mo").child("="))
                        .child(Element.mathMl("mn").child("1")));
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
            body.child(tr().child(td(row[0])).child(td(row[1])));
        }

        return table()
                .css("w-full text-sm border border-slate-200")
                .child(caption("Real table generated with a Java loop").css("text-left font-semibold"))
                .child(colgroup().child(col().attr("span", "1")).child(col().attr("span", "1")))
                .child(thead().child(tr().child(th("Tag")).child(th("Use"))))
                .child(body)
                .child(tfoot().child(tr().child(td().attr("colspan", "2").child("Rows generated in Java"))));
    }

    private Node liveEventPreview() {
        return div()
                .css("flex flex-col gap-2")
                .child(button("onClick").css("px-3 py-2 rounded bg-primary-700 text-white font-semibold").onClick(() -> {
                }))
                .child(inputText().placeholder("onInput").css("w-full rounded border border-slate-200 p-2 text-sm").onInput(() -> {
                }))
                .child(select().css("w-full rounded border border-slate-200 p-2 text-sm").onChange(() -> {
                }).child(option("onChange").value("change")))
                .child(form().onSubmit(() -> {
                }).child(button("onSubmit").type("submit").css("px-3 py-2 rounded bg-secondary-700 text-white font-semibold")));
    }

    private String factoryIndex() {
        return "import ujfe.html.Element;\n\n"
                + "import static ujfe.html.UI.*;\n\n"
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
        return "// Demo.java - run with: java Demo.java\n"
                + "import java.util.List;\n"
                + "import ujfe.core.ClientState;\n"
                + "import ujfe.core.Node;\n"
                + "import ujfe.live.LiveSession;\n"
                + "import ujfe.router.Router;\n"
                + "import static ujfe.html.UI.*;\n\n"
                + "record Metric(String label, int value, String status) {}\n\n"
                + "record MetricsPage(List<Metric> metrics) {\n"
                + "    public Node render() {\n"
                + "        var rows = metrics.stream()\n"
                + "            .map(metric -> tr()\n"
                + "                .child(td(metric.label()))\n"
                + "                .child(td(String.valueOf(metric.value())).css(switch (metric.status()) {\n"
                + "                    case \"ok\" -> \"text-emerald-700\";\n"
                + "                    case \"warn\" -> \"text-amber-700\";\n"
                + "                    default -> \"text-slate-700\";\n"
                + "                })))\n"
                + "            .toList();\n\n"
                + "        return table()\n"
                + "            .child(caption(\"Metrics\"))\n"
                + "            .child(thead().child(tr().child(th(\"Label\")).child(th(\"Value\"))))\n"
                + "            .child(tbody().children(rows));\n"
                + "    }\n"
                + "}\n\n"
                + "void main() {\n"
                + "    var metrics = List.of(\n"
                + "        new Metric(\"Users\", 42, \"ok\"),\n"
                + "        new Metric(\"Errors\", 2, \"warn\")\n"
                + "    );\n\n"
                + "    var router = new Router().register(\"/\", () -> new MetricsPage(metrics));\n"
                + "    var liveSession = new LiveSession(router);\n"
                + "    System.out.println(liveSession.renderDocument(\"/\", ClientState.empty()));\n"
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
                + "img().src(\"/logo.png\").alt(\"Logo\")\n"
                + "picture()\n"
                + "    .child(source().attr(\"media\", \"(min-width: 800px)\").src(\"/hero-wide.webp\"))\n"
                + "    .child(img().src(\"/hero.webp\").alt(\"Dashboard\"))\n"
                + "canvas().width(320).height(180).ariaLabel(\"Chart\")\n"
                + "video().src(\"/demo.mp4\").poster(\"/poster.png\").controls(true)\n"
                + "audio().controls(true)\n"
                + "    .child(source().src(\"/audio.mp3\").type(\"audio/mpeg\"))\n"
                + "    .child(track().attr(\"kind\", \"captions\").attr(\"srclang\", \"en\").src(\"/captions.vtt\"))\n"
                + "figure().child(svg().attr(\"viewBox\", \"0 0 10 10\")).child(figcaption(\"SVG\"))\n"
                + "map().attr(\"name\", \"primary-map\").child(area().attr(\"shape\", \"rect\").attr(\"coords\", \"0,0,20,20\").href(\"/docs\"))\n"
                + "iframe().src(\"/embedded\").attr(\"loading\", \"lazy\")\n"
                + "object().attr(\"data\", \"/report.pdf\").type(\"application/pdf\")\n"
                + "embed().src(\"/preview.pdf\").type(\"application/pdf\")\n"
                + "param().attr(\"name\", \"autoplay\").attr(\"value\", \"false\")\n"
                + "ul().child(le().child(\"Item\"))\n"
                + "ol().child(li().child(\"Item\"))\n"
                + "dl().child(dt(\"Term\")).child(dd(\"Description\"));\n";
    }

    private String modernHtmlCode() {
        return "record Metric(String name, int value, String status) {}\n\n"
                + "var metrics = List.of(\n"
                + "    new Metric(\"Users\", 42, \"ok\"),\n"
                + "    new Metric(\"Errors\", 2, \"warn\")\n"
                + ");\n\n"
                + "var rows = metrics.stream()\n"
                + "    .map(metric -> {\n"
                + "        var tone = switch (metric.status()) {\n"
                + "            case \"ok\" -> \"text-emerald-700\";\n"
                + "            case \"warn\" -> \"text-amber-700\";\n"
                + "            default -> \"text-slate-700\";\n"
                + "        };\n"
                + "        return tr()\n"
                + "            .child(td(metric.name()))\n"
                + "            .child(td(String.valueOf(metric.value())).css(tone));\n"
                + "    })\n"
                + "    .toList();\n\n"
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
        return "a(\"Safe\").href(\"https://example.com\")\n"
                + "img().src(\"data:image/png;base64,...\").alt(\"Preview\")\n"
                + "div().title(\"Title\").ariaLabel(\"Region\").data(\"test-id\", \"hero\")\n\n"
                + "// Safe text remains escaped:\n"
                + "p(\"<script>\") // renders &lt;script&gt;\n\n"
                + "// Trusted raw HTML must be explicit and reviewed:\n"
                + "unsafeHtml(\"<strong>Trusted fragment</strong>\")\n"
                + "UnsafeHtml.of(\"<p>Trusted CMS block</p>\")\n\n"
                + "// Rejected before rendering:\n"
                + "a(\"XSS\").href(\"javascript:alert(1)\")\n\n"
                + "// The server also emits headers such as CSP, nosniff,\n"
                + "// Referrer-Policy, Permissions-Policy, and frame-ancestors.\n";
    }

    private String restCode() {
        return "var client = RestClient.create();\n"
                + "var response = client\n"
                + "    .get(\"https://brasilapi.com.br/api/banks/v1\")\n"
                + "    .requireSuccessful();\n\n"
                + "record Bank(String label, String selectValue) {}\n"
                + "var banks = parseBanks(response.body());\n\n"
                + "select().children(banks.stream()\n"
                + "    .map(bank -> option(bank.label()).value(bank.selectValue()))\n"
                + "    .toList());\n";
    }

    private String cliCommandCode() {
        return "ujfe convert page.html \\\n"
                + "  --out src/main/java/app/pages/Page.java \\\n"
                + "  --type html\n";
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
                + "import static ujfe.html.UI.*;\n\n"
                + "import ujfe.html.Node;\n"
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
                + "LiveSession liveSession = new LiveSession(router, config);\n\n"
                + "// When enabled, the HTML includes:\n"
                + "<script src=\"/_ujfe/dev.js\"></script>\n\n"
                + "// The panel lets you click elements and test CSS classes\n"
                + "// in the browser without writing manual JavaScript or Node/npm.\n";
    }
}
