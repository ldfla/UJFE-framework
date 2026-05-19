package app.components;

import ujfe.core.Component;
import ujfe.core.Lifecycle;
import ujfe.core.Node;
import ujfe.signals.Signal;
import ujfe.signals.Signals;

import java.util.Objects;
import java.util.function.BooleanSupplier;

import static ujfe.core.UI.*;

public final class LifecycleResourceComponent implements Component, Lifecycle {
    private final BooleanSupplier darkMode;
    private final Signal<Integer> mounts = Signals.signal(0);
    private final Signal<Integer> cleanups = Signals.signal(0);
    private final Signal<Integer> interactions = Signals.signal(0);
    private final Signal<Boolean> resourceOpen = Signals.signal(false);

    public LifecycleResourceComponent(BooleanSupplier darkMode) {
        this.darkMode = Objects.requireNonNull(darkMode, "darkMode");
    }

    @Override
    public void onMount() {
        mounts.update(value -> value + 1);
        resourceOpen.set(true);
    }

    @Override
    public void onUnmount() {
        cleanups.update(value -> value + 1);
        resourceOpen.set(false);
    }

    @Override
    public Node render() {
        return div()
            .css(darkMode.getAsBoolean()
                ? "rounded-lg border border-slate-800 bg-slate-900 p-6 shadow-sm flex flex-col gap-5"
                : "rounded-lg border border-slate-200/60 bg-white p-6 shadow-sm flex flex-col gap-5")
            .child(
                div()
                    .css("flex flex-col gap-2")
                    .child(h2("Lifecycle resource").css(darkMode.getAsBoolean()
                        ? "text-lg font-bold text-slate-100"
                        : "text-lg font-bold text-slate-900"))
                    .child(p("A server-side component opens a resource on mount and releases it on unmount.")
                        .css(darkMode.getAsBoolean()
                            ? "text-xs text-slate-300 leading-relaxed"
                            : "text-xs text-slate-500 leading-relaxed"))
            )
            .child(
                div()
                    .css(darkMode.getAsBoolean()
                        ? "flex flex-col gap-2.5 bg-slate-950 border border-slate-800 rounded-lg p-4 font-mono text-xs text-slate-300"
                        : "flex flex-col gap-2.5 bg-slate-50 border border-slate-100 rounded-lg p-4 font-mono text-xs text-slate-600")
                    .child(p(() -> "Mount callbacks  : " + mounts.get()).css("font-semibold text-indigo-600"))
                    .child(p(() -> "Cleanup callbacks: " + cleanups.get()).css(mutedTextClass()))
                    .child(p(() -> "Resource state   : " + (resourceOpen.get() ? "open" : "closed")).css(mutedTextClass()))
                    .child(p(() -> "Interactions     : " + interactions.get()).css(mutedTextClass()))
            )
            .child(
                button("Touch stateful component")
                    .css("w-full px-4 h-10 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white font-medium text-sm shadow-sm active:scale-[0.98] transition-all")
                    .onClick(() -> interactions.update(value -> value + 1))
            )
            .child(span("Stable re-renders keep the same component instance mounted.")
                .css("text-xs text-slate-400 italic"));
    }

    private String mutedTextClass() {
        return darkMode.getAsBoolean() ? "text-slate-300" : "text-slate-500";
    }
}
