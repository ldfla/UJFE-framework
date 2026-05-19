package app.pages;

import app.AppTheme;
import app.components.AppHeader;
import ujfe.core.Component;
import ujfe.core.Node;
import ujfe.router.Page;
import ujfe.signals.Computed;
import ujfe.signals.Signal;
import ujfe.signals.Signals;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static ujfe.core.UI.*;

@Page("/signals")
public final class SignalsPage implements Component {
    private final AppTheme theme;
    private final Signal<Integer> count = Signals.signal(1);
    private final Signal<Integer> multiplier = Signals.signal(2);
    private final Signal<String> cacheReadLog = Signals.signal("No explicit cache read has run yet.");
    private final Signal<String> subscriberLog = Signals.signal("Subscriber sample has not run yet.");
    private final AtomicInteger doubledEvaluations = new AtomicInteger();
    private final AtomicInteger totalEvaluations = new AtomicInteger();
    private final Computed<Integer> doubled = Signals.computed(() -> {
        doubledEvaluations.incrementAndGet();
        return count.get() * 2;
    });
    private final Computed<Integer> total = Signals.computed(() -> {
        totalEvaluations.incrementAndGet();
        return doubled.get() * multiplier.get();
    });

    public SignalsPage(AppTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    @Override
    public Node render() {
        return AppHeader.pageShell(theme)
            .child(new AppHeader(theme, AppHeader.SIGNALS).render())
            .child(
                main()
                    .css("max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 flex flex-col gap-8")
                    .child(hero())
                    .child(panels())
                    .child(codeSample())
            );
    }

    private Node hero() {
        return section()
            .css(cardClass("p-8 sm:p-10 flex flex-col gap-6"))
            .child(span("Signals runtime").css(kickerClass()))
            .child(h1("Lazy computed state with deterministic invalidation")
                .css(heroTitleClass()))
            .child(p("Signals are the state primitive for live UJFE components. Use them for values that change because of server-side events, form input, or browser state; keep static content as plain Java values.")
                .css(bodyClass("text-base max-w-3xl")))
            .child(
                div()
                    .css("grid grid-cols-1 md:grid-cols-3 gap-4")
                    .child(metric("Lazy", "Computed suppliers run only when get() is called."))
                    .child(metric("Cached", "Repeated reads reuse the last successful result."))
                    .child(metric("Deterministic", "Dependency invalidation is explicit and tested."))
            );
    }

    private Node panels() {
        return section()
            .css("grid grid-cols-1 md:grid-cols-2 gap-6")
            .child(valuesPanel())
            .child(cachePanel())
            .child(subscriberPanel())
            .child(nestedPanel());
    }

    private Node valuesPanel() {
        return div()
            .css(cardClass("p-6 flex flex-col gap-5"))
            .child(h2("Mutable inputs").css(titleClass()))
            .child(p("Counter and multiplier are mutable signals. The derived values below update after the next render reads their computed graph.")
                .css(bodyClass("text-sm")))
            .child(
                div()
                    .css(codePanelClass())
                    .child(p(() -> "count      : " + count.get()).css("font-semibold text-indigo-600"))
                    .child(p(() -> "multiplier : " + multiplier.get()).css(mutedTextClass()))
                    .child(p(() -> "doubled    : " + doubled.get()).css(mutedTextClass()))
                    .child(p(() -> "total      : " + total.get()).css(mutedTextClass()))
            )
            .child(
                div()
                    .css("grid grid-cols-2 gap-3")
                    .child(button("Increment count")
                        .css("px-4 h-10 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white font-medium text-sm shadow-sm")
                        .onClick(() -> count.update(value -> value + 1)))
                    .child(button("Increment multiplier")
                        .css("px-4 h-10 rounded-lg bg-slate-900 hover:bg-slate-800 text-white font-medium text-sm shadow-sm")
                        .onClick(() -> multiplier.update(value -> value + 1)))
            );
    }

    private Node cachePanel() {
        return div()
            .css(cardClass("p-6 flex flex-col gap-5"))
            .child(h2("Cache behavior").css(titleClass()))
            .child(p("Read the nested computed value twice. Without invalidation, the second read reuses the same cache entry.")
                .css(bodyClass("text-sm")))
            .child(
                div()
                    .css(codePanelClass())
                    .child(p(() -> "doubled evaluations : " + doubledEvaluations.get()).css(mutedTextClass()))
                    .child(p(() -> "total evaluations   : " + totalEvaluations.get()).css(mutedTextClass()))
                    .child(p(() -> "last cache read     : " + cacheReadLog.get()).css("text-slate-400 italic"))
            )
            .child(button("Read computed twice")
                .css("w-full px-4 h-10 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white font-medium text-sm shadow-sm")
                .onClick(this::readComputedTwice));
    }

    private Node subscriberPanel() {
        return div()
            .css(cardClass("p-6 flex flex-col gap-5"))
            .child(h2("Subscriber semantics").css(titleClass()))
            .child(p("The sample invalidates a computed value multiple times and reads it once. The subscriber receives only the latest successful value.")
                .css(bodyClass("text-sm")))
            .child(p(subscriberLog::get)
                .css(codePanelClass()))
            .child(button("Run subscriber sample")
                .css("w-full px-4 h-10 rounded-lg bg-slate-900 hover:bg-slate-800 text-white font-medium text-sm shadow-sm")
                .onClick(this::runSubscriberSample));
    }

    private Node nestedPanel() {
        return div()
            .css(cardClass("p-6 flex flex-col gap-5"))
            .child(h2("Nested computed graph").css(titleClass()))
            .child(p("total depends on doubled, and doubled depends on count. A count update invalidates both values without recomputing until render reads them.")
                .css(bodyClass("text-sm")))
            .child(
                div()
                    .css("grid grid-cols-2 gap-3")
                    .child(metric("doubled", "count * 2"))
                    .child(metric("total", "doubled * multiplier"))
            );
    }

    private Node metric(String title, String body) {
        return div()
            .css(theme.darkMode()
                ? "rounded-lg border border-slate-800 bg-slate-950 p-5 flex flex-col gap-1.5"
                : "rounded-lg border border-slate-100 bg-slate-50/50 p-5 flex flex-col gap-1.5")
            .child(strong(title).css(theme.darkMode()
                ? "text-sm font-semibold text-slate-100"
                : "text-sm font-semibold text-slate-900"))
            .child(span(body).css(bodyClass("text-xs")));
    }

    private Node codeSample() {
        return section()
            .css(cardClass("p-6 flex flex-col gap-4"))
            .child(h2("Computed signal contract").css(theme.darkMode()
                ? "text-xl font-bold text-slate-100"
                : "text-xl font-bold text-slate-900"))
            .child(p("The snippet uses the current UJFE signals API: create mutable state with Signals.signal(...) and derive read-only state with Signals.computed(...).")
                .css(bodyClass("text-sm")))
            .child(pre()
                .css("overflow-x-auto rounded-lg bg-zinc-950 text-zinc-50 p-5 text-xs font-mono leading-relaxed")
                .child(code(snippet()).css("font-mono")));
    }

    private String cardClass(String extra) {
        return (theme.darkMode()
            ? "rounded-lg border border-slate-800 bg-slate-900 shadow-sm "
            : "rounded-lg border border-slate-200/80 bg-white shadow-sm ") + extra;
    }

    private String kickerClass() {
        return theme.darkMode()
            ? "text-xs font-semibold uppercase text-primary-300"
            : "text-xs font-semibold uppercase text-indigo-600";
    }

    private String heroTitleClass() {
        return theme.darkMode()
            ? "text-3xl sm:text-4xl font-extrabold text-slate-100 leading-none"
            : "text-3xl sm:text-4xl font-extrabold text-slate-900 leading-none";
    }

    private String titleClass() {
        return theme.darkMode()
            ? "text-lg font-bold text-slate-100"
            : "text-lg font-bold text-slate-900";
    }

    private String bodyClass(String size) {
        return theme.darkMode()
            ? size + " text-slate-300 leading-relaxed"
            : size + " text-slate-600 leading-relaxed";
    }

    private String mutedTextClass() {
        return theme.darkMode() ? "text-slate-300" : "text-slate-500";
    }

    private String codePanelClass() {
        return theme.darkMode()
            ? "flex flex-col gap-2.5 bg-slate-950 border border-slate-800 rounded-lg p-4 font-mono text-xs text-slate-300"
            : "flex flex-col gap-2.5 bg-slate-50 border border-slate-100 rounded-lg p-4 font-mono text-xs text-slate-600";
    }

    private void readComputedTwice() {
        int before = totalEvaluations.get();
        int first = total.get();
        int second = total.get();
        int after = totalEvaluations.get();
        cacheReadLog.set("first=" + first + ", second=" + second + ", new evaluations=" + (after - before));
    }

    private void runSubscriberSample() {
        Signal<Integer> base = Signals.signal(1);
        Computed<Integer> sample = Signals.computed(() -> base.get() * 10);
        List<Integer> updates = new ArrayList<>();

        try {
            AutoCloseable subscription = sample.subscribe(updates::add);
            sample.get();
            base.set(2);
            base.set(3);
            sample.get();
            subscription.close();
            subscriberLog.set("subscriber updates: " + updates);
        } catch (Exception exception) {
            subscriberLog.set("subscriber sample failed: " + exception.getMessage());
        }
    }

    private String snippet() {
        return "Signal<Integer> count = Signals.signal(1);\n"
            + "Computed<Integer> doubled = Signals.computed(() -> count.get() * 2);\n"
            + "Computed<Integer> total = Signals.computed(() -> doubled.get() * multiplier.get());\n\n"
            + "// Lazy: no evaluation until get().\n"
            + "total.get();\n"
            + "total.get(); // cached\n\n"
            + "// Invalidation does not recompute immediately.\n"
            + "count.set(2);\n"
            + "count.set(3);\n"
            + "total.get(); // one recomputation with the latest count\n";
    }
}
