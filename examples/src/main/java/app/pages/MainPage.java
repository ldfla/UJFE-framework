package app.pages;

import static ujfe.html.UI.*;

import app.components.CounterComponent;
import ujfe.core.Ujfe;
import ujfe.html.Node;
import ujfe.router.Page;
import ujfe.signals.Signal;
import ujfe.signals.Signals;

@Page("/")
public final class MainPage {
    private final CounterComponent counter = new CounterComponent();
    private final Signal<Integer> pageEvents = Signals.signal(0);
    private final Signal<Integer> formSubmits = Signals.signal(0);
    private final Signal<String> lastBrowserRead = Signals.signal("No browser state event has been processed yet.");

    public Node render() {
        return div()
                .css("min-h-screen bg-stone-50 text-slate-900")
                .child(topBar())
                .child(
                        div()
                                .css("max-w-6xl mx-auto p-4 app-shell gap-4")
                                .child(sidebar())
                                .child(
                                        main()
                                                .css("flex flex-col gap-4")
                                                .child(heroSection())
                                                .child(demoPanels())
                                                .child(codeExample())
                                )
                );
    }

    private Node topBar() {
        return header()
                .css("border-b border-slate-200 bg-white")
                .child(
                        div()
                                .css("max-w-6xl mx-auto p-4 flex items-center justify-between gap-4")
                                .child(
                                        div()
                                                .css("flex flex-col gap-1")
                                                .child(span("UJFE").css("text-sm font-bold text-emerald-700"))
                                                .child(h1("Using Java For Everything").css("text-2xl font-bold"))
                                )
                                .child(
                                        nav()
                                                .css("flex items-center gap-2")
                                                .child(a("Documentation")
                                                        .attr("href", "/docs")
                                                        .css("px-3 py-2 rounded border border-slate-200 bg-white text-sm font-semibold text-slate-700"))
                                                .child(span("Java-first Reactive SSR")
                                                        .css("px-3 py-1 rounded bg-emerald-50 text-emerald-700 text-sm font-semibold"))
                                )
                );
    }

    private Node sidebar() {
        return aside()
                .css("rounded-lg border border-emerald-200 bg-emerald-50 p-4 shadow-sm")
                .child(nav()
                        .css("flex flex-col gap-3")
                        .child(h2("Page map").css("text-lg font-bold text-emerald-700"))
                        .child(p("A small example showing layout, declarative HTML, optional internal CSS, and live events.")
                                .css("text-sm text-slate-700 leading-relaxed"))
                        .child(
                                ul()
                                        .css("flex flex-col gap-2 text-sm text-slate-700")
                                        .child(le().child(strong("Header")).child(" with brand and subtitle"))
                                        .child(le().child(strong("Sidebar")).child(" with navigation"))
                                        .child(le().child(strong("Main")).child(" with h1, h2, h3, and panels"))
                                        .child(le().child(strong("Live")).child(" with Signal and onClick"))
                        ));
    }

    private Node heroSection() {
        return section()
                .css("rounded-lg border border-indigo-200 bg-white p-6 text-center shadow-sm flex flex-col gap-2")
                .child(h1("UJFE").css("text-3xl font-bold text-indigo-700"))
                .child(h2("Modern web interfaces using Java only").css("text-2xl font-semibold"))
                .child(h3("Reactive SSR, server-side events, and backend-rendered CSS")
                        .css("text-lg font-semibold text-emerald-700"))
                .child(p("This page is written with the Java DSL, without TypeScript, npm, Babel, or a bundler.")
                        .css("text-base text-slate-600 leading-relaxed"));
    }

    private Node demoPanels() {
        return section()
                .css("demo-grid gap-4")
                .child(counter.render())
                .child(browserStatePanel())
                .child(stylePanel())
                .child(formPanel());
    }

    private Node browserStatePanel() {
        return div()
                .css("rounded-lg border border-indigo-200 bg-indigo-50 p-4 shadow-sm flex flex-col gap-3")
                .child(h2("State, cookies, and localStorage").css("text-2xl font-bold text-indigo-700"))
                .child(h3("Browser data available from Java").css("text-lg font-semibold text-slate-700"))
                .child(p(() -> "Page events processed: " + pageEvents.get())
                        .css("text-base font-semibold text-slate-900"))
                .child(p(() -> "Cookie ujfe_demo: " + Ujfe.cookie("ujfe_demo").orElse("not sent"))
                        .css("text-sm text-slate-700"))
                .child(p(() -> "localStorage ujfe.theme: " + Ujfe.localStorage("ujfe.theme").orElse("not found"))
                        .css("text-sm text-slate-700"))
                .child(p(() -> "Last read: " + lastBrowserRead.get())
                        .css("text-sm text-slate-700 leading-relaxed"))
                .child(
                        button("Read browser state")
                                .css("px-4 py-2 rounded bg-emerald-700 text-white font-semibold")
                                .onClick(this::readBrowserState)
                )
                .child(p("To test it, set this in the browser: document.cookie = 'ujfe_demo=active'; localStorage.setItem('ujfe.theme', 'dark');")
                        .css("text-sm text-slate-600 leading-relaxed"));
    }

    private void readBrowserState() {
        pageEvents.update(value -> value + 1);
        var cookie = Ujfe.cookie("ujfe_demo").orElse("not sent");
        var theme = Ujfe.localStorage("ujfe.theme").orElse("not found");
        lastBrowserRead.set("cookie ujfe_demo=" + cookie + ", localStorage ujfe.theme=" + theme);
    }

    private Node stylePanel() {
        return div()
                .css("rounded-lg border border-rose-200 bg-rose-50 p-4 shadow-sm flex flex-col gap-3")
                .child(h2("Declarative HTML and CSS").css("text-2xl font-bold text-rose-700"))
                .child(h3("Borders, colors, and centered text").css("text-lg font-semibold text-slate-700"))
                .child(
                        div()
                                .css("rounded-lg border border-indigo-200 bg-indigo-50 p-4 text-center")
                                .child(p("This div uses Tailwind-like classes rendered on the server.")
                                        .css("font-semibold text-indigo-700"))
                )
                .child(p("The renderer collects classes used during SSR and generates a small stylesheet in the document.")
                        .css("text-sm text-slate-700 leading-relaxed"));
    }

    private Node formPanel() {
        return div()
                .css("rounded-lg border border-emerald-200 bg-white p-4 shadow-sm flex flex-col gap-3")
                .child(h2("Declarative form").css("text-2xl font-bold text-emerald-700"))
                .child(h3("Inputs, select, checkbox, radio, and live submit").css("text-lg font-semibold text-slate-700"))
                .child(p(() -> "Submits processed on the server: " + formSubmits.get())
                        .css("text-base font-semibold text-slate-900"))
                .child(
                        form()
                                .css("flex flex-col gap-3")
                                .onSubmit(this::submitDemoForm)
                                .child(
                                        label("Name")
                                                .forId("demo-name")
                                                .css("text-sm font-semibold text-slate-700")
                                )
                                .child(
                                        inputText()
                                                .id("demo-name")
                                                .name("name")
                                                .placeholder("Ada Lovelace")
                                                .css("w-full rounded border border-slate-200 bg-white p-2 text-sm")
                                )
                                .child(
                                        label("Age")
                                                .forId("demo-age")
                                                .css("text-sm font-semibold text-slate-700")
                                )
                                .child(
                                        inputNumber()
                                                .id("demo-age")
                                                .name("age")
                                                .min("0")
                                                .max("120")
                                                .css("w-full rounded border border-slate-200 bg-white p-2 text-sm")
                                )
                                .child(
                                        label("Password")
                                                .forId("demo-password")
                                                .css("text-sm font-semibold text-slate-700")
                                )
                                .child(
                                        inputPassword()
                                                .id("demo-password")
                                                .name("password")
                                                .placeholder("********")
                                                .css("w-full rounded border border-slate-200 bg-white p-2 text-sm")
                                )
                                .child(
                                        label("Profile")
                                                .forId("demo-role")
                                                .css("text-sm font-semibold text-slate-700")
                                )
                                .child(
                                        select()
                                                .id("demo-role")
                                                .name("role")
                                                .css("w-full rounded border border-slate-200 bg-white p-2 text-sm")
                                                .child(option("Java backend").value("backend"))
                                                .child(option("Full stack Java").value("fullstack").selected(true))
                                )
                                .child(
                                        label()
                                                .css("flex items-center gap-2 text-sm text-slate-700")
                                                .child(checkbox().name("newsletter").checked(true))
                                                .child("Receive UJFE updates")
                                )
                                .child(
                                        div()
                                                .css("flex gap-3 text-sm text-slate-700")
                                                .child(label()
                                                        .css("flex items-center gap-2")
                                                        .child(radio().name("plan").value("starter").checked(true))
                                                        .child("Starter"))
                                                .child(label()
                                                        .css("flex items-center gap-2")
                                                        .child(radio().name("plan").value("pro"))
                                                        .child("Pro"))
                                )
                                .child(
                                        textarea("Server-rendered form.")
                                                .name("notes")
                                                .rows(3)
                                                .css("w-full rounded border border-slate-200 bg-white p-2 text-sm")
                                )
                                .child(
                                        button("Submit form")
                                                .type("submit")
                                                .css("px-4 py-2 rounded bg-emerald-700 text-white font-semibold")
                                )
                );
    }

    private void submitDemoForm() {
        formSubmits.update(value -> value + 1);
    }

    private Node codeExample() {
        return section()
                .css("rounded-lg border border-slate-200 bg-white p-4 shadow-sm flex flex-col gap-3")
                .child(h2("Usage code").css("text-2xl font-bold"))
                .child(p("The example below shows the basic way to declare state, HTML, and events in UJFE.")
                        .css("text-sm text-slate-600"))
                .child(pre()
                        .css("overflow-x-auto rounded-lg bg-zinc-950 text-zinc-50 p-4 text-sm font-mono")
                        .child(code(usageSnippet()).css("font-mono")));
    }

    private String usageSnippet() {
        return "// app/pages/CounterPage.java\n"
                + "import static ujfe.html.UI.*;\n"
                + "import app.components.CounterComponent;\n\n"
                + "@Page(\"/\")\n"
                + "public final class CounterPage {\n"
                + "    private final CounterComponent counter = new CounterComponent();\n"
                + "    private final Signal<Integer> pageEvents = Signals.signal(0);\n\n"
                + "    private void readBrowserState() {\n"
                + "        var cookie = Ujfe.cookie(\"ujfe_demo\").orElse(\"not sent\");\n"
                + "        var theme = Ujfe.localStorage(\"ujfe.theme\").orElse(\"not found\");\n"
                + "        pageEvents.update(value -> value + 1);\n"
                + "    }\n\n"
                + "    public Node render() {\n"
                + "        return div()\n"
                + "            .css(\"p-8 flex flex-col gap-4\")\n"
                + "            .child(h1(\"UJFE\"))\n"
                + "            .child(p(() -> \"Events: \" + pageEvents.get()))\n"
                + "            .child(p(() -> \"Cookie: \" + Ujfe.cookie(\"ujfe_demo\").orElse(\"not sent\")))\n"
                + "            .child(p(() -> \"Theme: \" + Ujfe.localStorage(\"ujfe.theme\").orElse(\"not found\")))\n"
                + "            .child(button(\"Read browser state\").onClick(this::readBrowserState))\n"
                + "            .child(counter.render());\n"
                + "    }\n"
                + "}\n\n"
                + "// app/components/CounterComponent.java\n"
                + "import static ujfe.html.UI.*;\n\n"
                + "public final class CounterComponent implements Component {\n"
                + "    private final Signal<Integer> count = Signals.signal(0);\n\n"
                + "    public Node render() {\n"
                + "        return div()\n"
                + "            .child(h2(\"Live counter\"))\n"
                + "            .child(p(() -> \"Counter: \" + count.get()))\n"
                + "            .child(button(\"Increment\")\n"
                + "                .onClick(() -> count.update(value -> value + 1)));\n"
                + "    }\n"
                + "}\n";
    }
}
