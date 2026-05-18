package app.pages;

import app.AppTheme;
import app.components.AppHeader;
import app.components.CounterComponent;
import ujfe.core.Component;
import ujfe.core.Ujfe;
import ujfe.core.Element;
import ujfe.core.Node;
import ujfe.router.Page;
import ujfe.signals.Signal;
import ujfe.signals.Signals;

import java.util.Objects;

import static ujfe.core.UI.*;

@Page("/")
public final class MainPage implements Component {
    private final AppTheme theme;
    private final CounterComponent counter;
    private final Signal<Integer> pageEvents = Signals.signal(0);
    private final Signal<Integer> formSubmits = Signals.signal(0);
    private final Signal<Boolean> modalOpen = Signals.signal(false);
    private final Signal<Boolean> toastVisible = Signals.signal(false);
    private final Signal<String> alertTone = Signals.signal("success");
    private final Signal<String> lastBrowserRead = Signals.signal("No browser state event has been processed yet.");

    public MainPage(AppTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
        this.counter = new CounterComponent(theme::darkMode);
    }

    @Override
    public Node render() {
        return AppHeader.pageShell(theme)
            .child(new AppHeader(theme, AppHeader.HOME).render())
            .child(
                div()
                    .css("max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 grid grid-cols-1 lg:grid-cols-4 gap-8")
                    .child(sidebar())
                    .child(
                        main()
                            .css("lg:col-span-3 flex flex-col gap-8")
                            .child(heroSection())
                            .child(capabilitySection())
                            .child(demoPanels())
                            .child(feedbackSection())
                            .child(codeExample())
                    )
            )
            .child(toastOverlay())
            .child(modalOverlay());
    }

    private Node sidebar() {
        return aside()
            .css(theme.darkMode()
                ? "lg:col-span-1 h-fit sticky top-24 rounded-lg border border-slate-800 bg-slate-900 p-5 shadow-sm"
                : "lg:col-span-1 h-fit sticky top-24 rounded-lg border border-slate-200/60 bg-white p-5 shadow-sm")
            .child(nav()
                .css("flex flex-col gap-4")
                .child(h2("Page map").css("text-xs font-bold uppercase text-slate-400"))
                .child(p("A concise overview of the components and live reactive features rendered in this sandbox.")
                    .css(bodyTextClass()))
                .child(
                    ul()
                        .css(theme.darkMode()
                            ? "list-none flex flex-col gap-2.5 text-sm font-medium text-slate-300"
                            : "list-none flex flex-col gap-2.5 text-sm font-medium text-slate-600")
                        .child(navItem("Showcase", "Core capabilities"))
                        .child(navItem("Live", "Signals and events"))
                        .child(navItem("Feedback", "Alerts, toasts, modals"))
                        .child(navItem("Code", "Java DSL preview"))
                ));
    }

    private Node navItem(String title, String detail) {
        return le()
            .css(theme.darkMode()
                ? "flex items-center gap-2 hover:text-primary-200 transition-colors"
                : "flex items-center gap-2 hover:text-slate-900 transition-colors")
            .child(strong(title).css(theme.darkMode() ? "text-slate-100" : "text-slate-800"))
            .child("- " + detail);
    }

    private Node heroSection() {
        return section()
            .css(theme.darkMode()
                ? "relative overflow-hidden rounded-lg border border-slate-800 bg-slate-900 p-8 sm:p-10 shadow-sm flex flex-col gap-6"
                : "relative overflow-hidden rounded-lg border border-slate-200/80 bg-white p-8 sm:p-10 shadow-sm flex flex-col gap-6")
            .child(
                div()
                    .css("flex flex-col gap-2")
                    .child(span("Example application").css(theme.darkMode()
                        ? "text-xs font-semibold uppercase text-primary-300"
                        : "text-xs font-semibold uppercase text-indigo-600"))
                    .child(h1("Reactive server-rendered UI, written natively in Java")
                        .css(theme.darkMode()
                            ? "text-3xl sm:text-4xl font-extrabold text-slate-100 leading-none"
                            : "text-3xl sm:text-4xl font-extrabold text-slate-900 leading-none"))
            )
            .child(p("This page is generated using the UJFE Java DSL, live server-side handlers, signals, dynamic scoped CSS, and runtime extension points. Zero client-side dependencies: no TypeScript, no npm, no Babel, and no heavy client framework runtimes.")
                .css(theme.darkMode()
                    ? "text-base text-slate-300 leading-relaxed max-w-3xl"
                    : "text-base text-slate-600 leading-relaxed max-w-3xl"))
            .child(
                div()
                    .css("flex flex-wrap gap-3")
                    .child(button("Open live modal")
                        .css("px-4 h-10 rounded-lg bg-primary-600 hover:bg-primary-700 text-white font-medium text-sm shadow-sm active:scale-[0.98] transition-all")
                        .onClick(this::openShowcaseModal))
                    .child(a("Read the docs")
                        .href("/docs")
                        .css(theme.darkMode()
                            ? "inline-flex items-center px-4 h-10 rounded-lg border border-slate-700 bg-slate-900 text-slate-100 hover:bg-slate-800 text-sm font-semibold transition-colors"
                            : "inline-flex items-center px-4 h-10 rounded-lg border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 text-sm font-semibold transition-colors"))
            )
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
            .css(theme.darkMode()
                ? "rounded-lg border border-slate-800 bg-slate-800 p-5 flex flex-col gap-1.5 hover:border-primary-700 hover:bg-slate-900 transition-all duration-200"
                : "rounded-lg border border-slate-100 bg-slate-50/50 p-5 flex flex-col gap-1.5 hover:border-slate-200 hover:bg-slate-50 transition-all duration-200")
            .child(strong(title).css(theme.darkMode()
                ? "text-sm font-semibold text-slate-100"
                : "text-sm font-semibold text-slate-900"))
            .child(span(body).css(bodyTextClass()));
    }

    private Node capabilitySection() {
        return section()
            .css("grid grid-cols-1 md:grid-cols-2 gap-6")
            .child(capabilityCard("Generic HTML", "Element.of(...) renders current tags, future tags, Web Components, SVG, and MathML without waiting for helper releases."))
            .child(capabilityCard("Live Events", "Click, input, change, and submit handlers execute as Java callbacks while keeping event ids opaque and server controlled."))
            .child(capabilityCard("Runtime Actions", "Server-side extension points observe rendering, live events, errors, and head contribution without tying code to an adapter."))
            .child(capabilityCard("Security Defaults", "Escaped text, URL policy, CSRF checks, rate limits, safe errors, and explicit unsafe HTML boundaries are part of the runtime surface."));
    }

    private Node capabilityCard(String title, String body) {
        return div()
            .css(cardClass("flex flex-col gap-3"))
            .child(span("Capability").css(theme.darkMode()
                ? "text-xs font-semibold uppercase text-primary-300"
                : "text-xs font-semibold uppercase text-indigo-600"))
            .child(h2(title).css(titleClass()))
            .child(p(body).css(bodyTextClass()));
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
            .css(cardClass("flex flex-col justify-between gap-5"))
            .child(
                div()
                    .css("flex flex-col gap-2")
                    .child(h2("State and Storage").css(titleClass()))
                    .child(p("Inspect browser storage state bridges directly from your Java server context.")
                        .css(bodyTextClass()))
            )
            .child(
                div()
                    .css(theme.darkMode()
                        ? "flex flex-col gap-2.5 bg-slate-800 border border-slate-700 rounded-lg p-4 font-mono text-xs text-slate-300"
                        : "flex flex-col gap-2.5 bg-slate-50 border border-slate-100 rounded-lg p-4 font-mono text-xs text-slate-600")
                    .child(p(() -> "Events processed : " + pageEvents.get()).css("font-semibold text-primary-400"))
                    .child(p(() -> "Cookie ujfe_demo : " + Ujfe.cookie("ujfe_demo").orElse("not sent"))
                        .css(bodyTextClass()))
                    .child(p(() -> "LocalStorage    : " + Ujfe.localStorage("ujfe.theme").orElse("not found"))
                        .css(bodyTextClass()))
                    .child(p(() -> "SessionStorage  : " + Ujfe.sessionStorage("ujfe.tab").orElse("not found"))
                        .css(bodyTextClass()))
                    .child(p(() -> "Log             : " + lastBrowserRead.get())
                        .css(theme.darkMode()
                            ? "text-slate-400 italic border-t border-slate-700 pt-2 mt-1"
                            : "text-slate-400 italic border-t border-slate-200/60 pt-2 mt-1"))
            )
            .child(
                button("Read browser state")
                    .css(theme.darkMode()
                        ? "w-full px-4 h-10 rounded-lg bg-primary-600 hover:bg-primary-700 text-white font-medium text-sm shadow-sm active:scale-[0.98] transition-all"
                        : "w-full px-4 h-10 rounded-lg bg-slate-900 hover:bg-slate-800 text-white font-medium text-sm shadow-sm active:scale-[0.98] transition-all")
                    .onClick(this::readBrowserState)
            )
            .child(pre()
                .css(theme.darkMode()
                    ? "overflow-x-auto rounded-lg border border-slate-700 bg-slate-950 p-3 text-[11px] leading-relaxed text-slate-300"
                    : "overflow-x-auto rounded-lg border border-slate-200 bg-white p-3 text-[11px] leading-relaxed text-slate-500")
                .child(code("document.cookie = \"ujfe_demo=active\";\n"
                    + "localStorage.setItem(\"ujfe.theme\", \"dark\");\n"
                    + "sessionStorage.setItem(\"ujfe.tab\", \"docs\");")));
    }

    private void readBrowserState() {
        pageEvents.update(value -> value + 1);
        var cookie = Ujfe.cookie("ujfe_demo").orElse("not sent");
        var theme = Ujfe.localStorage("ujfe.theme").orElse("not found");
        var tab = Ujfe.sessionStorage("ujfe.tab").orElse("not found");
        lastBrowserRead.set("cookie=" + cookie + " | theme=" + theme + " | tab=" + tab);
    }

    private Node stylePanel() {
        return div()
            .css(cardClass("flex flex-col justify-between gap-5"))
            .child(
                div()
                    .css("flex flex-col gap-2")
                    .child(h2("Declarative UI and Styles").css(titleClass()))
                    .child(p("CSS can run in internal, external, or none mode while preserving standard class attributes.")
                        .css(bodyTextClass()))
            )
            .child(
                div()
                    .css(theme.darkMode()
                        ? "relative overflow-hidden rounded-lg border border-primary-700 bg-slate-800 p-6 text-center shadow-inner"
                        : "relative overflow-hidden rounded-lg border border-indigo-100 bg-gradient-to-br from-indigo-50/40 to-slate-50/20 p-6 text-center shadow-inner")
                    .child(p("Static media such as /poster.png, /demo.mp4, and /audio.mp3 is handled outside page routing.")
                        .css(theme.darkMode()
                            ? "text-sm font-semibold text-primary-200 leading-relaxed"
                            : "text-sm font-semibold text-indigo-950 leading-relaxed"))
            )
            .child(
                p("Missing assets now return safe 404 responses without route exceptions or severe stack traces.")
                    .css("text-xs text-slate-400 italic")
            );
    }

    private Node formPanel() {
        return div()
            .css(cardClass("flex flex-col gap-5"))
            .child(
                div()
                    .css("flex flex-col gap-2")
                    .child(h2("Declarative Forms").css(titleClass()))
                    .child(p(() -> "Submits intercept: " + formSubmits.get())
                        .css("text-xs font-semibold text-secondary-500"))
            )
            .child(
                form()
                    .css("flex flex-col gap-4")
                    .onSubmit(this::submitDemoForm)
                    .child(formField("Name", "demo-name",
                        inputText().id("demo-name").name("name").placeholder("Said Adla")))
                    .child(formField("Age", "demo-age",
                        inputNumber().id("demo-age").name("age").min("0").max("120").placeholder("25")))
                    .child(formField("Password", "demo-password",
                        inputPassword().id("demo-password").name("password").placeholder("********")))
                    .child(
                        div()
                            .css("flex flex-col gap-1.5")
                            .child(label("Profile").forId("demo-role").css(labelClass()))
                            .child(
                                select()
                                    .id("demo-role")
                                    .name("role")
                                    .css(inputClass())
                                    .child(option("Java backend").value("backend"))
                                    .child(option("Full stack Java").value("fullstack").selected(true))
                            )
                    )
                    .child(
                        label()
                            .css(theme.darkMode()
                                ? "inline-flex items-center gap-2 text-xs font-medium text-slate-300"
                                : "inline-flex items-center gap-2 text-xs font-medium text-slate-700")
                            .child(checkbox().name("newsletter").checked(true))
                            .child("Receive UJFE updates")
                    )
                    .child(
                        div()
                            .css(theme.darkMode()
                                ? "flex gap-4 text-xs font-medium text-slate-300"
                                : "flex gap-4 text-xs font-medium text-slate-700")
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
                            .css(textareaClass())
                    )
                    .child(
                        button("Submit reactive payload")
                            .type("submit")
                            .css("w-full px-4 h-10 rounded-lg bg-secondary-600 hover:bg-secondary-700 text-white font-medium text-sm shadow-sm active:scale-[0.98] transition-all")
                    )
            );
    }

    private Node formField(String labelText, String id, Element inputNode) {
        return div()
            .css("flex flex-col gap-1.5")
            .child(label(labelText).forId(id).css(labelClass()))
            .child(inputNode.css(inputClass()));
    }

    private void submitDemoForm() {
        formSubmits.update(value -> value + 1);
    }

    private Node feedbackSection() {
        return section()
            .css(cardClass("flex flex-col gap-6"))
            .child(
                div()
                    .css("flex flex-col gap-2")
                    .child(span("Interactive feedback").css(theme.darkMode()
                        ? "text-xs font-semibold uppercase text-primary-300"
                        : "text-xs font-semibold uppercase text-indigo-600"))
                    .child(h2("Alerts, toasts, and modal flows rendered from Java").css(theme.darkMode()
                        ? "text-xl font-bold text-slate-100"
                        : "text-xl font-bold text-slate-900"))
                    .child(p("Feedback primitives are ordinary components: state lives in Signals, events run on the JVM, and the rendered HTML stays safe by default.")
                        .css(theme.darkMode() ? "text-sm text-slate-300 leading-relaxed" : "text-sm text-slate-600 leading-relaxed"))
            )
            .child(
                div()
                    .css("grid grid-cols-1 md:grid-cols-3 gap-4")
                    .child(alertShowcase())
                    .child(toastShowcase())
                    .child(modalShowcase())
            );
    }

    private Node alertShowcase() {
        return div()
            .css(featurePanelClass())
            .child(h3("Contextual alerts").css(titleClass()))
            .child(alertBox())
            .child(button("Change alert tone")
                .css(secondaryButtonClass())
                .onClick(this::cycleAlertTone));
    }

    private Node alertBox() {
        return div()
            .role("status")
            .css(alertClass())
            .child(strong(alertTitle()).css("text-sm font-bold"))
            .child(p(alertMessage()).css("text-xs leading-relaxed"));
    }

    private Node toastShowcase() {
        return div()
            .css(featurePanelClass())
            .child(h3("Server toast").css(titleClass()))
            .child(p("A floating status message can be displayed after any live server event.")
                .css(bodyTextClass()))
            .child(button("Show toast")
                .css(primaryButtonClass())
                .onClick(this::showToast))
            .child(p("The toast is rendered as live HTML after the server handler updates its Signal.")
                .css("text-xs text-slate-400 italic"));
    }

    private Node modalShowcase() {
        return div()
            .css(featurePanelClass())
            .child(h3("Modal workflow").css(titleClass()))
            .child(p("A modal can be driven by the same server-side state and event pipeline.")
                .css(bodyTextClass()))
            .child(button("Open modal")
                .css(primaryButtonClass())
                .onClick(this::openShowcaseModal))
            .child(p("The modal uses the same server-side state model as every other component on this page.")
                .css("text-xs text-slate-400 italic"));
    }

    private Node toastOverlay() {
        return div()
            .role("status")
            .css(toastOverlayClass())
            .child(strong("Live update completed").css(theme.darkMode() ? "text-sm text-slate-100" : "text-sm text-slate-900"))
            .child(p("This toast was rendered by a Java event handler.").css(bodyTextClass()))
            .child(button("Dismiss")
                .css(secondaryButtonClass())
                .onClick(() -> toastVisible.set(false)));
    }

    private Node modalOverlay() {
        return div()
            .css(modalOverlayClass())
            .child(
                div()
                    .role("dialog")
                    .aria("modal", "true")
                    .ariaLabel("UJFE showcase modal")
                    .css(theme.darkMode()
                        ? "max-w-2xl w-full rounded-lg border border-slate-700 bg-slate-900 p-6 shadow-sm flex flex-col gap-5"
                        : "max-w-2xl w-full rounded-lg border border-slate-200 bg-white p-6 shadow-sm flex flex-col gap-5")
                    .child(
                        div()
                            .css("flex items-start justify-between gap-4")
                            .child(
                                div()
                                    .css("flex flex-col gap-1")
                                    .child(span("Modal").css(theme.darkMode()
                                        ? "text-xs font-semibold uppercase text-primary-300"
                                        : "text-xs font-semibold uppercase text-indigo-600"))
                                    .child(h2("A modal rendered from the same Java component tree").css(theme.darkMode()
                                        ? "text-2xl font-bold text-slate-100"
                                        : "text-2xl font-bold text-slate-900"))
                            )
                            .child(button("Close")
                                .css(secondaryButtonClass())
                                .onClick(this::closeShowcaseModal))
                    )
                    .child(p("No client framework is required for this interaction. The button event updates server state, UJFE re-renders the affected tree, and the client bridge applies the new HTML.")
                        .css(theme.darkMode() ? "text-sm text-slate-300 leading-relaxed" : "text-sm text-slate-600 leading-relaxed"))
                    .child(
                        div()
                            .css(theme.darkMode()
                                ? "grid grid-cols-1 md:grid-cols-3 gap-3 rounded-lg border border-slate-800 bg-slate-950 p-4"
                                : "grid grid-cols-1 md:grid-cols-3 gap-3 rounded-lg border border-slate-100 bg-slate-50 p-4")
                            .child(modalMetric("State", "Signal<Boolean>"))
                            .child(modalMetric("Event", "onClick(...)"))
                            .child(modalMetric("HTML", "role=\"dialog\""))
                    )
            );
    }

    private String toastOverlayClass() {
        String visibleClass = theme.darkMode()
            ? "fixed right-6 bottom-6 z-50 max-w-md rounded-lg border border-slate-700 bg-slate-900 p-4 shadow-sm flex flex-col gap-2"
            : "fixed right-6 bottom-6 z-50 max-w-md rounded-lg border border-slate-200 bg-white/95 p-4 shadow-sm flex flex-col gap-2";
        return toastVisible.get() ? visibleClass : visibleClass + " hidden";
    }

    private String modalOverlayClass() {
        String visibleClass = "fixed inset-0 z-40 flex items-center justify-center p-6 bg-black/50 backdrop-blur-md";
        return modalOpen.get() ? visibleClass : visibleClass + " hidden";
    }

    private Node modalMetric(String label, String value) {
        return div()
            .css("flex flex-col gap-1")
            .child(span(label).css("text-xs text-slate-400 uppercase font-semibold"))
            .child(strong(value).css(theme.darkMode() ? "text-sm text-slate-100" : "text-sm text-slate-900"));
    }

    private void showToast() {
        toastVisible.set(true);
    }

    private void openShowcaseModal() {
        modalOpen.set(true);
    }

    private void closeShowcaseModal() {
        modalOpen.set(false);
    }

    private void cycleAlertTone() {
        String current = alertTone.get();
        if ("success".equals(current)) {
            alertTone.set("warning");
        } else if ("warning".equals(current)) {
            alertTone.set("danger");
        } else {
            alertTone.set("success");
        }
    }

    private String alertClass() {
        String tone = alertTone.get();
        if ("warning".equals(tone)) {
            return theme.darkMode()
                ? "rounded-lg border border-amber-200 bg-slate-800 p-4 flex flex-col gap-1 text-amber-300"
                : "rounded-lg border border-amber-200 bg-amber-50 p-4 flex flex-col gap-1 text-amber-800";
        }
        if ("danger".equals(tone)) {
            return theme.darkMode()
                ? "rounded-lg border border-rose-200 bg-slate-800 p-4 flex flex-col gap-1 text-slate-100"
                : "rounded-lg border border-rose-200 bg-rose-50 p-4 flex flex-col gap-1 text-rose-700";
        }
        return theme.darkMode()
            ? "rounded-lg border border-emerald-200 bg-slate-800 p-4 flex flex-col gap-1 text-emerald-400"
            : "rounded-lg border border-emerald-200 bg-emerald-50 p-4 flex flex-col gap-1 text-emerald-700";
    }

    private String alertTitle() {
        String tone = alertTone.get();
        if ("warning".equals(tone)) {
            return "Configuration review";
        }
        if ("danger".equals(tone)) {
            return "Policy blocked";
        }
        return "Render completed";
    }

    private String alertMessage() {
        String tone = alertTone.get();
        if ("warning".equals(tone)) {
            return "A server-side validation can change the rendered alert state before the response reaches the browser.";
        }
        if ("danger".equals(tone)) {
            return "Unsafe input is rejected before rendering and can be surfaced with a stable, safe message.";
        }
        return "The UI was updated from a JVM callback and text remains escaped by default.";
    }

    private Node codeExample() {
        return section()
            .css(cardClass("flex flex-col gap-4"))
            .child(
                div()
                    .css("flex flex-col gap-1")
                    .child(h2("Declarative DSL Architecture").css(theme.darkMode()
                        ? "text-xl font-bold text-slate-100"
                        : "text-xl font-bold text-slate-900"))
                    .child(p("Clean OOP component abstraction without mixing view presentation and procedural controllers.")
                        .css(theme.darkMode() ? "text-xs text-slate-300" : "text-xs text-slate-500"))
            )
            .child(pre()
                .css("overflow-x-auto rounded-lg bg-slate-900 text-slate-100 p-5 text-xs font-mono border border-slate-800 shadow-inner leading-relaxed")
                .child(highlightedSnippet()));
    }

    private String cardClass(String layoutClasses) {
        String base = theme.darkMode()
            ? "rounded-lg border border-slate-800 bg-slate-900 p-6 shadow-sm "
            : "rounded-lg border border-slate-200/60 bg-white p-6 shadow-sm ";
        return base + layoutClasses;
    }

    private String featurePanelClass() {
        return theme.darkMode()
            ? "rounded-lg border border-slate-800 bg-slate-800 p-5 flex flex-col gap-4"
            : "rounded-lg border border-slate-100 bg-slate-50/50 p-5 flex flex-col gap-4";
    }

    private String primaryButtonClass() {
        return "w-full px-4 h-10 rounded-lg bg-primary-600 hover:bg-primary-700 text-white font-medium text-sm shadow-sm active:scale-[0.98] transition-all";
    }

    private String secondaryButtonClass() {
        return theme.darkMode()
            ? "px-4 h-10 rounded-lg border border-slate-700 bg-slate-900 text-slate-100 hover:bg-slate-800 text-sm font-semibold transition-colors"
            : "px-4 h-10 rounded-lg border border-slate-200 bg-white text-slate-700 hover:bg-slate-50 text-sm font-semibold transition-colors";
    }

    private String titleClass() {
        return theme.darkMode()
            ? "text-lg font-bold text-slate-100"
            : "text-lg font-bold text-slate-900";
    }

    private String bodyTextClass() {
        return theme.darkMode()
            ? "text-xs text-slate-300 leading-relaxed"
            : "text-xs text-slate-500 leading-relaxed";
    }

    private String labelClass() {
        return theme.darkMode()
            ? "text-xs font-semibold text-slate-200"
            : "text-xs font-semibold text-slate-700";
    }

    private String inputClass() {
        return theme.darkMode()
            ? "w-full h-9 rounded-lg border border-slate-700 bg-slate-900 px-3 text-sm text-slate-100 placeholder:text-slate-400 focus:border-primary-500 focus:ring-indigo-500/50 outline-none transition-all"
            : "w-full h-9 rounded-lg border border-slate-200 bg-white px-3 text-sm placeholder:text-slate-400 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500/50 outline-none transition-all";
    }

    private String textareaClass() {
        return theme.darkMode()
            ? "w-full rounded-lg border border-slate-700 bg-slate-900 p-3 text-sm text-slate-100 placeholder:text-slate-400 focus:border-primary-500 focus:ring-indigo-500/50 outline-none resize-none transition-all"
            : "w-full rounded-lg border border-slate-200 bg-white p-3 text-sm focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500/50 outline-none resize-none transition-all";
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
            .child(" CounterComponent(theme::darkMode);\n\n")
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
