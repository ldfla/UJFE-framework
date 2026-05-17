package app.pages;

import static ujfe.html.UI.*;

import app.components.CounterComponent;
import ujfe.core.Component;
import ujfe.core.Ujfe;
import ujfe.html.Element;
import ujfe.html.Node;
import ujfe.router.Page;
import ujfe.signals.Signal;
import ujfe.signals.Signals;

@Page("/")
public final class MainPage implements Component {
    private final CounterComponent counter = new CounterComponent();
    private final Signal<Integer> pageEvents = Signals.signal(0);
    private final Signal<Integer> formSubmits = Signals.signal(0);
    private final Signal<String> lastBrowserRead = Signals.signal("No browser state event has been processed yet.");

    @Override
    public Node render() {
        return div()
                .css("min-h-screen bg-slate-50 text-slate-900 font-sans antialiased")
                .child(topBar())
                .child(
                        div()
                                .css("max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 grid grid-cols-1 lg:grid-cols-4 gap-8")
                                .child(sidebar())
                                .child(
                                        main()
                                                .css("lg:col-span-3 flex flex-col gap-8")
                                                .child(heroSection())
                                                .child(demoPanels())
                                                .child(codeExample())
                                )
                );
    }

    private Node topBar() {
        return header()
                .css("sticky top-0 z-50 border-b border-slate-200/80 bg-white/80 backdrop-blur-md")
                .child(
                        div()
                                .css("max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between")
                                .child(
                                        div()
                                                .css("flex items-center gap-3")
                                                .child(
                                                        span("UJFE")
                                                                .css("px-2.5 py-1 text-xs font-black bg-gradient-to-r from-emerald-600 to-teal-600 text-white rounded shadow-sm")
                                                )
                                                .child(
                                                        h1("Modern Reactive UI Framework")
                                                                .css("hidden md:block text-sm font-semibold text-slate-900")
                                                )
                                )
                                .child(
                                        nav()
                                                .css("flex items-center gap-4")
                                                .child(a("Documentation")
                                                        .attr("href", "/docs")
                                                        .css("text-sm font-medium text-slate-600 hover:text-slate-900 transition-colors"))
                                                .child(a("Lifecycle")
                                                        .attr("href", "/lifecycle")
                                                        .css("text-sm font-medium text-slate-600 hover:text-slate-900 transition-colors"))
                                                .child(a("Actions")
                                                        .attr("href", "/runtime-actions")
                                                        .css("text-sm font-medium text-slate-600 hover:text-slate-900 transition-colors"))
                                                .child(span("JVM SSR")
                                                        .css("inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-medium bg-emerald-50 text-emerald-700 border border-emerald-200/60"))
                                )
                );
    }

    private Node sidebar() {
        return aside()
                .css("lg:col-span-1 h-fit sticky top-24 rounded-lg border border-slate-200/60 bg-white p-5 shadow-sm")
                .child(nav()
                        .css("flex flex-col gap-4")
                        .child(h2("Page map").css("text-xs font-bold uppercase text-slate-400"))
                        .child(p("A concise overview of the components and live reactive features rendered in this sandbox.")
                                .css("text-xs text-slate-500 leading-relaxed"))
                        .child(
                                ul()
                                        .css("list-none flex flex-col gap-2.5 text-sm font-medium text-slate-600")
                                        .child(navItem("Header", "Layout brand"))
                                        .child(navItem("Sidebar", "Navigation"))
                                        .child(navItem("Main", "Reactive core"))
                                        .child(navItem("Live", "State and Signals"))
                        ));
    }

    private Node navItem(String title, String detail) {
        return le()
                .css("flex items-center gap-2 hover:text-slate-900 transition-colors")
                .child(strong(title).css("text-slate-800"))
                .child("- " + detail);
    }

    private Node heroSection() {
        return section()
                .css("relative overflow-hidden rounded-lg border border-slate-200/80 bg-white p-8 sm:p-10 shadow-sm flex flex-col gap-6")
                .child(
                        div()
                                .css("flex flex-col gap-2")
                                .child(span("Example application").css("text-xs font-semibold uppercase text-indigo-600"))
                                .child(h1("Reactive server-rendered UI, written natively in Java")
                                        .css("text-3xl sm:text-4xl font-extrabold text-slate-900 leading-none"))
                )
                .child(p("This page is generated using the UJFE Java DSL, live server-side handlers, signals, dynamic scoped CSS, and runtime extension points. Zero client-side dependencies: no TypeScript, no npm, no Babel, and no heavy client framework runtimes.")
                        .css("text-base text-slate-600 leading-relaxed max-w-3xl"))
                .child(
                        div()
                                .css("grid grid-cols-1 md:grid-cols-3 gap-4 mt-2")
                                .child(heroMetric("HTML-first", "Real DOM elements and standard semantic attributes."))
                                .child(heroMetric("Server-first", "Event propagation executes safely inside the JVM."))
                                .child(heroMetric("Safe by default", "Automatic context-aware XSS escaping out of the box."))
                );
    }

    private Node heroMetric(String title, String body) {
        return div()
                .css("rounded-lg border border-slate-100 bg-slate-50/50 p-5 flex flex-col gap-1.5 hover:border-slate-200 hover:bg-slate-50 transition-all duration-200")
                .child(strong(title).css("text-sm font-semibold text-slate-900"))
                .child(span(body).css("text-xs text-slate-500 leading-relaxed"));
    }

    private Node demoPanels() {
        return section()
                .css("grid grid-cols-1 md:grid-cols-2 gap-6")
                .child(counter.render())
                .child(browserStatePanel())
                .child(stylePanel())
                .child(formPanel());
    }

    private Node browserStatePanel() {
        return div()
                .css("rounded-lg border border-slate-200/60 bg-white p-6 shadow-sm flex flex-col justify-between gap-5")
                .child(
                        div()
                                .css("flex flex-col gap-2")
                                .child(h2("State and Storage").css("text-lg font-bold text-slate-900"))
                                .child(p("Inspect browser storage state bridges directly from your Java server context.")
                                        .css("text-xs text-slate-500 leading-relaxed"))
                )
                .child(
                        div()
                                .css("flex flex-col gap-2.5 bg-slate-50 border border-slate-100 rounded-lg p-4 font-mono text-xs text-slate-600")
                                .child(p(() -> "Events processed : " + pageEvents.get()).css("font-semibold text-indigo-600"))
                                .child(p(() -> "Cookie ujfe_demo : " + Ujfe.cookie("ujfe_demo").orElse("not sent"))
                                        .css("text-slate-500"))
                                .child(p(() -> "LocalStorage    : " + Ujfe.localStorage("ujfe.theme").orElse("not found"))
                                        .css("text-slate-500"))
                                .child(p(() -> "Log             : " + lastBrowserRead.get())
                                        .css("text-slate-400 italic border-t border-slate-200/60 pt-2 mt-1"))
                )
                .child(
                        button("Read browser state")
                                .css("w-full px-4 h-10 rounded-lg bg-slate-900 hover:bg-slate-800 text-white font-medium text-sm shadow-sm active:scale-[0.98] transition-all")
                                .onClick(this::readBrowserState)
                );
    }

    private void readBrowserState() {
        pageEvents.update(value -> value + 1);
        var cookie = Ujfe.cookie("ujfe_demo").orElse("not sent");
        var theme = Ujfe.localStorage("ujfe.theme").orElse("not found");
        lastBrowserRead.set("cookie=" + cookie + " | theme=" + theme);
    }

    private Node stylePanel() {
        return div()
                .css("rounded-lg border border-slate-200/60 bg-white p-6 shadow-sm flex flex-col justify-between gap-5")
                .child(
                        div()
                                .css("flex flex-col gap-2")
                                .child(h2("Declarative UI and Styles").css("text-lg font-bold text-slate-900"))
                                .child(p("The engine tracks utility classes evaluated during SSR and mounts an optimized stylesheet.")
                                        .css("text-xs text-slate-500 leading-relaxed"))
                )
                .child(
                        div()
                                .css("relative overflow-hidden rounded-lg border border-indigo-100 bg-gradient-to-br from-indigo-50/40 to-slate-50/20 p-6 text-center shadow-inner")
                                .child(p("This node uses scoped JIT classes evaluated on the server stream.")
                                        .css("text-sm font-semibold text-indigo-950 leading-relaxed"))
                )
                .child(
                        p("Dynamic stylesheets are optimized to strip unused CSS rules automatically.")
                                .css("text-xs text-slate-400 italic")
                );
    }

    private Node formPanel() {
        return div()
                .css("rounded-lg border border-slate-200/60 bg-white p-6 shadow-sm flex flex-col gap-5")
                .child(
                        div()
                                .css("flex flex-col gap-2")
                                .child(h2("Declarative Forms").css("text-lg font-bold text-slate-900"))
                                .child(p(() -> "Submits intercept: " + formSubmits.get())
                                        .css("text-xs font-semibold text-emerald-600"))
                )
                .child(
                        form()
                                .css("flex flex-col gap-4")
                                .onSubmit(this::submitDemoForm)
                                .child(formField("Name", "demo-name",
                                        inputText().id("demo-name").name("name").placeholder("Ada Lovelace")))
                                .child(formField("Age", "demo-age",
                                        inputNumber().id("demo-age").name("age").min("0").max("120").placeholder("25")))
                                .child(formField("Password", "demo-password",
                                        inputPassword().id("demo-password").name("password").placeholder("********")))
                                .child(
                                        div()
                                                .css("flex flex-col gap-1.5")
                                                .child(label("Profile").forId("demo-role").css("text-xs font-semibold text-slate-700"))
                                                .child(
                                                        select()
                                                                .id("demo-role")
                                                                .name("role")
                                                                .css("w-full h-9 rounded-lg border border-slate-200 bg-white px-3 text-sm focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500/50 outline-none transition-all")
                                                                .child(option("Java backend").value("backend"))
                                                                .child(option("Full stack Java").value("fullstack").selected(true))
                                                )
                                )
                                .child(
                                        label()
                                                .css("inline-flex items-center gap-2 text-xs font-medium text-slate-700")
                                                .child(checkbox().name("newsletter").checked(true))
                                                .child("Receive UJFE updates")
                                )
                                .child(
                                        div()
                                                .css("flex gap-4 text-xs font-medium text-slate-700")
                                                .child(label()
                                                        .css("inline-flex items-center gap-1.5")
                                                        .child(radio().name("plan").value("starter").checked(true))
                                                        .child("Starter"))
                                                .child(label()
                                                        .css("inline-flex items-center gap-1.5")
                                                        .child(radio().name("plan").value("pro"))
                                                        .child("Pro"))
                                )
                                .child(
                                        textarea("Server-rendered architecture comments.")
                                                .name("notes")
                                                .rows(2)
                                                .css("w-full rounded-lg border border-slate-200 bg-white p-3 text-sm focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500/50 outline-none resize-none transition-all")
                                )
                                .child(
                                        button("Submit reactive payload")
                                                .type("submit")
                                                .css("w-full px-4 h-10 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white font-medium text-sm shadow-sm active:scale-[0.98] transition-all")
                                )
                );
    }

    private Node formField(String labelText, String id, Element inputNode) {
        return div()
                .css("flex flex-col gap-1.5")
                .child(label(labelText).forId(id).css("text-xs font-semibold text-slate-700"))
                .child(inputNode.css("w-full h-9 rounded-lg border border-slate-200 bg-white px-3 text-sm placeholder:text-slate-400 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500/50 outline-none transition-all"));
    }

    private void submitDemoForm() {
        formSubmits.update(value -> value + 1);
    }

    private Node codeExample() {
        return section()
                .css("rounded-lg border border-slate-200/60 bg-white p-6 shadow-sm flex flex-col gap-4")
                .child(
                        div()
                                .css("flex flex-col gap-1")
                                .child(h2("Declarative DSL Architecture").css("text-xl font-bold text-slate-900"))
                                .child(p("Clean OOP component abstraction without mixing view presentation and procedural controllers.")
                                        .css("text-xs text-slate-500"))
                )
                .child(pre()
                        .css("overflow-x-auto rounded-lg bg-slate-900 text-slate-100 p-5 text-xs font-mono border border-slate-800 shadow-inner leading-relaxed")
                        .child(highlightedSnippet()));
    }

    private Node highlightedSnippet() {
        return code()
                .css("font-mono")
                .child(span("@Page").css("text-emerald-400"))
                .child("(\"/\")\n")
                .child(span("public final class").css("text-indigo-400"))
                .child(" ")
                .child(span("CounterPage").css("text-teal-300"))
                .child(" {\n")
                .child("    ")
                .child(span("private final").css("text-indigo-400"))
                .child(" CounterComponent counter = ")
                .child(span("new").css("text-indigo-400"))
                .child(" CounterComponent();\n\n")
                .child("    ")
                .child(span("public").css("text-indigo-400"))
                .child(" Node ")
                .child(span("render").css("text-sky-300"))
                .child("() {\n")
                .child("        ")
                .child(span("return").css("text-indigo-400"))
                .child(" div().css(")
                .child(span("\"p-8 flex flex-col gap-4\"").css("text-amber-300"))
                .child(")\n")
                .child("            .child(h1(")
                .child(span("\"UJFE\"").css("text-amber-300"))
                .child("))\n")
                .child("            .child(counter.render());\n")
                .child("    }\n")
                .child("}\n");
    }
}
