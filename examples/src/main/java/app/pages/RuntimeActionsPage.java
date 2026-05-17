package app.pages;

import ujfe.core.Component;
import ujfe.html.Node;
import ujfe.router.Page;
import ujfe.runtime.action.RuntimeActionRegistry;

import static ujfe.html.UI.*;

/**
 * Example page demonstrating server-side runtime extension points.
 */
@Page("/runtime-actions")
public final class RuntimeActionsPage implements Component {

    @Override
    public Node render() {
        return div()
                .css("min-h-screen bg-slate-50 p-8 flex flex-col gap-8")
                .child(header())
                .child(registryExample())
                .child(orderingExample())
                .child(headContributionExample())
                .child(errorHandlingExample())
                .child(enterprisePatterns())
                .child(backLink());
    }

    private Node header() {
        return div()
                .css("flex flex-col gap-2")
                .child(h1("Runtime Extension Points")
                        .css("text-3xl font-bold text-slate-800"))
                .child(p("Server-side Java runtime actions for rendering, events, errors, and head contributions.")
                        .css("text-lg text-slate-600"));
    }

    private Node registryExample() {
        return section()
                .css("rounded-lg border border-blue-200 bg-blue-50 p-6 flex flex-col gap-4")
                .child(h2("Registry").css("text-2xl font-bold text-blue-700"))
                .child(p("Actions are registered through a builder and wired into LiveSessionConfig.")
                        .css("text-base text-slate-700"))
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
                .css("rounded-lg border border-emerald-200 bg-emerald-50 p-6 flex flex-col gap-4")
                .child(h2("Ordering").css("text-2xl font-bold text-emerald-700"))
                .child(p("Actions execute deterministically. Lower priority values run first. Equal priorities preserve registration order.")
                        .css("text-base text-slate-700"))
                .child(div()
                        .css("grid grid-cols-5 gap-2")
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
                .css("rounded-lg border border-violet-200 bg-violet-50 p-6 flex flex-col gap-4")
                .child(h2("Head Contributions").css("text-2xl font-bold text-violet-700"))
                .child(p("Actions can inject nodes into the document <head> during rendering.")
                        .css("text-base text-slate-700"))
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
                .css("rounded-lg border border-rose-200 bg-rose-50 p-6 flex flex-col gap-4")
                .child(h2("Error Handling").css("text-2xl font-bold text-rose-700"))
                .child(p("Exceptions in actions are routed to onError. If onError itself throws, the exception is logged to stderr — no infinite recursion.")
                        .css("text-base text-slate-700"))
                .child(ul()
                        .css("list-disc pl-6 text-slate-700 text-sm flex flex-col gap-1")
                        .child(li().child("Action exception → routed to onError(...)"))
                        .child(li().child("onError exception → logged to stderr, execution continues"))
                        .child(li().child("Original pipeline exception → re-thrown to caller"))
                        .child(li().child("Runtime integrity is always preserved")));
    }

    private Node enterprisePatterns() {
        return section()
                .css("rounded-lg border border-amber-200 bg-amber-50 p-6 flex flex-col gap-4")
                .child(h2("Enterprise Patterns").css("text-2xl font-bold text-amber-700"))
                .child(div()
                        .css("grid grid-cols-2 gap-4")
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

    private Node backLink() {
        return p()
                .css("text-sm text-slate-500")
                .child(a("← Back to documentation").href("/docs"));
    }

    private Node orderPill(String name, String value) {
        return div()
                .css("rounded-lg bg-white border border-emerald-200 p-3 text-center flex flex-col gap-1")
                .child(span(name).css("text-sm font-bold text-emerald-700"))
                .child(span(value).css("text-xs text-slate-500"));
    }

    private Node patternCard(String title, String code) {
        return div()
                .css("rounded-lg bg-white border border-amber-200 p-4 flex flex-col gap-2")
                .child(h3(title).css("text-lg font-bold text-amber-700"))
                .child(codeBlock(code));
    }

    private Node codeBlock(String text) {
        return pre()
                .css("bg-slate-800 text-green-400 p-4 rounded-lg text-sm font-mono overflow-x-auto")
                .child(code(text));
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
