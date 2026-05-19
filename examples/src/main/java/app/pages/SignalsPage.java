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
    private final Signal<Integer> tickets = Signals.signal(1);
    private final Signal<Integer> ticketPrice = Signals.signal(79);
    private final Signal<String> cacheReadLog = Signals.signal("No cart total has been checked yet.");
    private final Signal<String> subscriberLog = Signals.signal("No stock alert sample has run yet.");
    private final AtomicInteger subtotalEvaluations = new AtomicInteger();
    private final AtomicInteger checkoutEvaluations = new AtomicInteger();
    private final Computed<Integer> subtotal = Signals.computed(() -> {
        subtotalEvaluations.incrementAndGet();
        return tickets.get() * ticketPrice.get();
    });
    private final Computed<Integer> checkoutTotal = Signals.computed(() -> {
        checkoutEvaluations.incrementAndGet();
        int serviceFee = tickets.get() >= 3 ? 15 : 9;
        return subtotal.get() + serviceFee;
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
            .child(h1("Signals for checkout state people actually change")
                .css(heroTitleClass()))
            .child(p("Signals are useful when a page has values that keep changing: cart quantity, selected plan, filters, form drafts, preview text, and totals. Static headings and copy do not need signals.")
                .css(bodyClass("text-base max-w-3xl")))
            .child(
                div()
                    .css("grid grid-cols-1 md:grid-cols-3 gap-4")
                    .child(metric("Lazy", "Only calculate totals when the page needs to show them."))
                    .child(metric("Cached", "Use the same total in summary, badge, and receipt without recalculating."))
                    .child(metric("Deterministic", "Changing quantity or price invalidates exactly the derived values that depend on them."))
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
            .child(h2("Checkout estimator").css(titleClass()))
            .child(p("A real product page changes quantity and selected price tier. The subtotal and checkout total are computed from those signals instead of being hand-updated in multiple places.")
                .css(bodyClass("text-sm")))
            .child(
                div()
                    .css(codePanelClass())
                    .child(p(() -> "tickets       : " + tickets.get()).css("font-semibold text-indigo-600"))
                    .child(p(() -> "ticket price  : $" + ticketPrice.get()).css(mutedTextClass()))
                    .child(p(() -> "subtotal      : $" + subtotal.get()).css(mutedTextClass()))
                    .child(p(() -> "checkout total: $" + checkoutTotal.get()).css(mutedTextClass()))
            )
            .child(
                div()
                    .css("grid grid-cols-2 gap-3")
                    .child(button("Add ticket")
                        .css("px-4 h-10 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white font-medium text-sm shadow-sm")
                        .onClick(() -> tickets.update(value -> value + 1)))
                    .child(button("Upgrade tier")
                        .css("px-4 h-10 rounded-lg bg-slate-900 hover:bg-slate-800 text-white font-medium text-sm shadow-sm")
                        .onClick(() -> ticketPrice.update(value -> value + 20)))
            );
    }

    private Node cachePanel() {
        return div()
            .css(cardClass("p-6 flex flex-col gap-5"))
            .child(h2("Cached derived values").css(titleClass()))
            .child(p("Checkout pages often show the same total in more than one place: order summary, sticky button, and final receipt. A computed value can be read repeatedly after one calculation.")
                .css(bodyClass("text-sm")))
            .child(
                div()
                    .css(codePanelClass())
                    .child(p(() -> "subtotal calculations : " + subtotalEvaluations.get()).css(mutedTextClass()))
                    .child(p(() -> "checkout calculations : " + checkoutEvaluations.get()).css(mutedTextClass()))
                    .child(p(() -> "last repeated read    : " + cacheReadLog.get()).css("text-slate-400 italic"))
            )
            .child(button("Read total twice")
                .css("w-full px-4 h-10 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white font-medium text-sm shadow-sm")
                .onClick(this::readComputedTwice));
    }

    private Node subscriberPanel() {
        return div()
            .css(cardClass("p-6 flex flex-col gap-5"))
            .child(h2("Predictable updates").css(titleClass()))
            .child(p("A stock badge, availability notice, or checkout warning should not flash through stale intermediate values. Signals let the UI publish the latest derived value after related changes settle.")
                .css(bodyClass("text-sm")))
            .child(p(subscriberLog::get)
                .css(codePanelClass()))
            .child(button("Run stock alert sample")
                .css("w-full px-4 h-10 rounded-lg bg-slate-900 hover:bg-slate-800 text-white font-medium text-sm shadow-sm")
                .onClick(this::runSubscriberSample));
    }

    private Node nestedPanel() {
        return div()
            .css(cardClass("p-6 flex flex-col gap-5"))
            .child(h2("When to use signals").css(titleClass()))
            .child(p("Use signals for state that changes while the user stays on the page. Use plain Java values for labels, headings, static documentation, and layout constants.")
                .css(bodyClass("text-sm")))
            .child(
                div()
                    .css("grid grid-cols-2 gap-3")
                    .child(metric("Good fit", "cart, filters, form drafts, live previews"))
                    .child(metric("Avoid", "static copy, fixed nav labels, one-time constants"))
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
            .child(p("The snippet models a checkout summary with the current UJFE signals API: mutable page inputs with Signals.signal(...) and read-only totals with Signals.computed(...).")
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
        int before = checkoutEvaluations.get();
        int first = checkoutTotal.get();
        int second = checkoutTotal.get();
        int after = checkoutEvaluations.get();
        cacheReadLog.set("summary=$" + first + ", receipt=$" + second + ", new calculations=" + (after - before));
    }

    private void runSubscriberSample() {
        Signal<Integer> base = Signals.signal(1);
        Computed<Integer> sample = Signals.computed(() -> base.get() < 3 ? 1 : 0);
        List<Integer> updates = new ArrayList<>();

        try {
            AutoCloseable subscription = sample.subscribe(updates::add);
            sample.get();
            base.set(2);
            base.set(3);
            sample.get();
            subscription.close();
            subscriberLog.set("low-stock alert updates: " + updates + " (latest state only)");
        } catch (Exception exception) {
            subscriberLog.set("subscriber sample failed: " + exception.getMessage());
        }
    }

    private String snippet() {
        return "Signal<Integer> tickets = Signals.signal(1);\n"
            + "Signal<Integer> ticketPrice = Signals.signal(79);\n\n"
            + "Computed<Integer> subtotal = Signals.computed(() ->\n"
            + "    tickets.get() * ticketPrice.get());\n\n"
            + "Computed<Integer> checkoutTotal = Signals.computed(() -> {\n"
            + "    int serviceFee = tickets.get() >= 3 ? 15 : 9;\n"
            + "    return subtotal.get() + serviceFee;\n"
            + "});\n\n"
            + "// Lazy: no total is calculated until the UI reads it.\n"
            + "checkoutTotal.get();\n"
            + "checkoutTotal.get(); // cached for another part of the page\n\n"
            + "// Deterministic: changing either input invalidates the right totals.\n"
            + "tickets.set(2);\n"
            + "ticketPrice.set(99);\n"
            + "checkoutTotal.get(); // recalculates from the latest page state\n";
    }
}
