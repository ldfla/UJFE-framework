package app.components;

import ujfe.core.Component;
import ujfe.core.Lifecycle;
import ujfe.core.Node;
import ujfe.signals.Signal;
import ujfe.signals.Signals;

import static ujfe.core.UI.*;

public final class LifecycleResourceComponent implements Component, Lifecycle {
    private final Signal<Integer> mounts = Signals.signal(0);
    private final Signal<Integer> cleanups = Signals.signal(0);
    private final Signal<Integer> interactions = Signals.signal(0);
    private final Signal<Boolean> resourceOpen = Signals.signal(false);

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
                .css("rounded-lg border border-slate-200/60 bg-white p-6 shadow-sm flex flex-col gap-5")
                .child(
                        div()
                                .css("flex flex-col gap-2")
                                .child(h2("Lifecycle resource").css("text-lg font-bold text-slate-900"))
                                .child(p("A server-side component opens a resource on mount and releases it on unmount.")
                                        .css("text-xs text-slate-500 leading-relaxed"))
                )
                .child(
                        div()
                                .css("flex flex-col gap-2.5 bg-slate-50 border border-slate-100 rounded-lg p-4 font-mono text-xs text-slate-600")
                                .child(p(() -> "Mount callbacks  : " + mounts.get()).css("font-semibold text-indigo-600"))
                                .child(p(() -> "Cleanup callbacks: " + cleanups.get()).css("text-slate-500"))
                                .child(p(() -> "Resource state   : " + (resourceOpen.get() ? "open" : "closed")).css("text-slate-500"))
                                .child(p(() -> "Interactions     : " + interactions.get()).css("text-slate-500"))
                )
                .child(
                        button("Touch stateful component")
                                .css("w-full px-4 h-10 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white font-medium text-sm shadow-sm active:scale-[0.98] transition-all")
                                .onClick(() -> interactions.update(value -> value + 1))
                )
                .child(span("Stable re-renders keep the same component instance mounted.")
                        .css("text-xs text-slate-400 italic"));
    }
}
