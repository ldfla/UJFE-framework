package app.pages;

import app.AppTheme;
import app.components.AppHeader;
import ujfe.core.Component;
import ujfe.core.Node;
import ujfe.router.Page;
import ujfe.runtime.action.RuntimeActionRegistry;

import java.util.Objects;

import static ujfe.core.UI.*;

/**
 * Example page demonstrating server-side runtime extension points.
 */
@Page("/runtime-actions")
public final class RuntimeActionsPage implements Component {
    private final AppTheme theme;

    public RuntimeActionsPage(AppTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    @Override
    public Node render() {
        return AppHeader.pageShell(theme)
                .child(new AppHeader(theme, AppHeader.ACTIONS).render())
                .child(main()
                        .css("max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 flex flex-col gap-8")
                        .child(hero())
                        .child(sectionGrid())
                        .child(enterprisePatterns()));
    }

    private Node hero() {
        return section()
                .css(cardClass("relative overflow-hidden p-8 sm:p-10 flex flex-col gap-6"))
                .child(div()
                        .css("flex flex-col gap-2")
                        .child(span("Runtime actions").css(theme.darkMode()
                                ? "text-xs font-semibold uppercase text-primary-300"
                                : "text-xs font-semibold uppercase text-indigo-600"))
                        .child(h1("Server-side extension points without runtime coupling")
                                .css(theme.darkMode()
                                        ? "text-3xl sm:text-4xl font-extrabold text-slate-100 leading-none"
                                        : "text-3xl sm:text-4xl font-extrabold text-slate-900 leading-none")))
                .child(p("Actions let applications participate in rendering, live events, error handling, and document head assembly while keeping the runtime deterministic and adapter-agnostic.")
                        .css(theme.darkMode()
                                ? "text-base text-slate-300 leading-relaxed max-w-3xl"
                                : "text-base text-slate-600 leading-relaxed max-w-3xl"))
                .child(div()
                        .css("grid grid-cols-1 md:grid-cols-3 gap-4 mt-2")
                        .child(metric("Explicit", "Registered through RuntimeActionRegistry."))
                        .child(metric("Ordered", "Lower priority executes first."))
                        .child(metric("Server-side", "No browser lifecycle hooks or JS framework concepts.")));
    }

    private Node metric(String title, String body) {
        return div()
                .css(theme.darkMode()
                        ? "rounded-lg border border-slate-800 bg-slate-800 p-5 flex flex-col gap-1.5"
                        : "rounded-lg border border-slate-100 bg-slate-50/50 p-5 flex flex-col gap-1.5")
                .child(strong(title).css(theme.darkMode()
                        ? "text-sm font-semibold text-slate-100"
                        : "text-sm font-semibold text-slate-900"))
                .child(span(body).css(bodyTextClass()));
    }

    private Node sectionGrid() {
        return section()
                .css("grid grid-cols-1 lg:grid-cols-2 gap-6")
                .child(registryExample())
                .child(orderingExample())
                .child(headContributionExample())
                .child(errorHandlingExample());
    }

    private Node registryExample() {
        return section()
                .css(cardClass("p-6 flex flex-col gap-4"))
                .child(sectionEyebrow("Registry"))
                .child(h2("Register runtime behavior explicitly").css(titleClass()))
                .child(p("Actions are registered through a builder and wired into LiveSessionConfig.")
                        .css(bodyTextClass()))
                .child(codeBlock(
                        "RuntimeActionRegistry registry = RuntimeActionRegistry.builder()\n"
                                + "    .beforeRender(ctx -> log(\"Rendering \" + ctx.path()))\n"
                                + "    .afterRender(result -> metrics.record(result.renderDuration()))\n"
                                + "    .beforeEvent(ctx -> authorize(ctx.clientState()))\n"
                                + "    .afterEvent(result -> audit(result.eventId()))\n"
                                + "    .onError(ctx -> logger.error(ctx.exception()))\n"
                                + "    .contributeHead(ctx -> ctx.add(meta().attr(\"name\", \"robots\")))\n"
                                + "    .build();\n\n"
                                + "LiveSessionConfig config = LiveSessionConfig.builder()\n"
                                + "    .runtimeActions(registry)\n"
                                + "    .build();"
                ));
    }

    private Node orderingExample() {
        return section()
                .css(cardClass("p-6 flex flex-col gap-4"))
                .child(sectionEyebrow("Ordering"))
                .child(h2("Deterministic action order").css(titleClass()))
                .child(p("Actions execute deterministically. Lower priority values run first. Equal priorities preserve registration order.")
                        .css(bodyTextClass()))
                .child(div()
                        .css("grid grid-cols-2 sm:grid-cols-5 gap-2")
                        .child(orderPill("FIRST", "0"))
                        .child(orderPill("EARLY", "250"))
                        .child(orderPill("NORMAL", "500"))
                        .child(orderPill("LATE", "750"))
                        .child(orderPill("LAST", "1000")))
                .child(codeBlock(
                        ".beforeRender(ActionOrder.FIRST, ctx -> security())\n"
                                + ".beforeRender(ActionOrder.NORMAL, ctx -> business())\n"
                                + ".beforeRender(ActionOrder.LAST, ctx -> metrics())"
                ));
    }

    private Node headContributionExample() {
        return section()
                .css(cardClass("p-6 flex flex-col gap-4"))
                .child(sectionEyebrow("Head"))
                .child(h2("Contribute document head nodes").css(titleClass()))
                .child(p("Actions can inject nodes into the document <head> during rendering.")
                        .css(bodyTextClass()))
                .child(codeBlock(
                        ".contributeHead(ctx -> {\n"
                                + "    ctx.add(meta().attr(\"name\", \"viewport\")\n"
                                + "                  .attr(\"content\", \"width=device-width\"));\n"
                                + "    ctx.add(link().attr(\"rel\", \"icon\")\n"
                                + "                 .attr(\"href\", \"/favicon.ico\"));\n"
                                + "})"
                ));
    }

    private Node errorHandlingExample() {
        return section()
                .css(cardClass("p-6 flex flex-col gap-4"))
                .child(sectionEyebrow("Errors"))
                .child(h2("Observe failures without leaking details").css(titleClass()))
                .child(p("Exceptions in actions are routed to onError. If onError itself throws, the exception is logged to stderr and execution avoids recursive error handling.")
                        .css(bodyTextClass()))
                .child(ul()
                        .css(theme.darkMode()
                                ? "list-disc pl-6 text-slate-300 text-sm flex flex-col gap-1"
                                : "list-disc pl-6 text-slate-700 text-sm flex flex-col gap-1")
                        .child(li().child("Action exception -> routed to onError(...)"))
                        .child(li().child("onError exception -> logged to stderr, execution continues"))
                        .child(li().child("Original pipeline exception -> re-thrown to caller"))
                        .child(li().child("Runtime integrity is always preserved")));
    }

    private Node enterprisePatterns() {
        return section()
                .css(cardClass("p-6 flex flex-col gap-4"))
                .child(sectionEyebrow("Patterns"))
                .child(h2("Enterprise usage patterns").css(titleClass()))
                .child(p("These examples show where security, observability, SEO, and auditing concerns can attach without coupling the app to Netty, Servlet, or Spring APIs.")
                        .css(bodyTextClass()))
                .child(div()
                        .css("grid grid-cols-1 md:grid-cols-2 gap-4")
                        .child(patternCard("Observability",
                                ".afterRender(result -> {\n"
                                        + "    metrics.record(\"render.ms\",\n"
                                        + "        result.renderDuration().toMillis());\n"
                                        + "})"))
                        .child(patternCard("Authorization",
                                ".beforeEvent(ActionOrder.FIRST, ctx -> {\n"
                                        + "    if (!authorized(ctx.clientState()))\n"
                                        + "        throw new SecurityException();\n"
                                        + "})"))
                        .child(patternCard("SEO",
                                ".contributeHead(ctx -> {\n"
                                        + "    ctx.add(meta()\n"
                                        + "        .attr(\"name\", \"description\")\n"
                                        + "        .attr(\"content\", \"...\"));\n"
                                        + "})"))
                        .child(patternCard("Error Logging",
                                ".onError(ctx -> {\n"
                                        + "    logger.error(\"{} phase: {}\",\n"
                                        + "        ctx.phase(),\n"
                                        + "        ctx.exception().getMessage());\n"
                                        + "})")));
    }

    private Node orderPill(String name, String value) {
        return div()
                .css(theme.darkMode()
                        ? "rounded-lg bg-slate-800 border border-slate-700 p-3 text-center flex flex-col gap-1"
                        : "rounded-lg bg-slate-50 border border-slate-200 p-3 text-center flex flex-col gap-1")
                .child(span(name).css(theme.darkMode()
                        ? "text-sm font-bold text-primary-200"
                        : "text-sm font-bold text-primary-700"))
                .child(span(value).css(theme.darkMode() ? "text-xs text-slate-400" : "text-xs text-slate-500"));
    }

    private Node patternCard(String title, String code) {
        return div()
                .css(theme.darkMode()
                        ? "rounded-lg bg-slate-800 border border-slate-700 p-4 flex flex-col gap-3"
                        : "rounded-lg bg-slate-50 border border-slate-200 p-4 flex flex-col gap-3")
                .child(h3(title).css(theme.darkMode()
                        ? "text-lg font-bold text-slate-100"
                        : "text-lg font-bold text-slate-900"))
                .child(codeBlock(code));
    }

    private Node codeBlock(String text) {
        return pre()
                .css("overflow-x-auto rounded-lg bg-slate-950 text-slate-100 p-5 text-xs font-mono border border-slate-800 shadow-inner leading-relaxed")
                .child(code(text));
    }

    private Node sectionEyebrow(String label) {
        return span(label).css(theme.darkMode()
                ? "text-xs font-semibold uppercase text-primary-300"
                : "text-xs font-semibold uppercase text-indigo-600");
    }

    private String cardClass(String layoutClasses) {
        String base = theme.darkMode()
                ? "rounded-lg border border-slate-800 bg-slate-900 shadow-sm "
                : "rounded-lg border border-slate-200/60 bg-white shadow-sm ";
        return base + layoutClasses;
    }

    private String titleClass() {
        return theme.darkMode()
                ? "text-xl font-bold text-slate-100"
                : "text-xl font-bold text-slate-900";
    }

    private String bodyTextClass() {
        return theme.darkMode()
                ? "text-sm text-slate-300 leading-relaxed"
                : "text-sm text-slate-600 leading-relaxed";
    }

    /**
     * Builds a sample registry for demonstration. This method is public
     * so that it can be referenced from the application startup.
     */
    public static RuntimeActionRegistry sampleRegistry() {
        return RuntimeActionRegistry.builder()
                .beforeRender(ctx ->
                        System.out.println("[UJFE] Rendering: " + ctx.path()))
                .afterRender(result ->
                        System.out.println("[UJFE] Rendered in " + result.renderDuration().toMillis() + "ms"))
                .onError(ctx ->
                        System.err.println("[UJFE] Error in " + ctx.phase() + ": " + ctx.exception().getMessage()))
                .build();
    }
}
