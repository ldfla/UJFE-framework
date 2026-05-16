package app.components;

import static ujfe.html.UI.*;

import ujfe.core.Component;
import ujfe.html.Node;
import ujfe.signals.Signal;
import ujfe.signals.Signals;

public final class CounterComponent implements Component {
    private final Signal<Integer> count = Signals.signal(0);

    @Override
    public Node render() {
        return div()
                .css("rounded-lg border border-slate-200/60 bg-white p-6 shadow-sm flex flex-col justify-between gap-5")
                .child(
                        div()
                                .css("flex flex-col gap-2")
                                .child(h2("Counter live").css("text-lg font-bold text-slate-900"))
                                .child(p("Estado mantido no servidor por meio de arquitetura baseada em Signals.")
                                        .css("text-xs text-slate-500 leading-relaxed"))
                )
                .child(
                        div()
                                .css("py-4 text-center")
                                .child(
                                        p()
                                                .css("text-5xl font-black text-slate-900 leading-none")
                                                .child("Counter: ")
                                                .child(span().child(() -> String.valueOf(count.get())).css("text-indigo-600"))
                                )
                )
                .child(
                        button("Incrementar via RPC")
                                .css("w-full px-4 h-10 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white font-medium text-sm shadow-sm active:scale-[0.98] transition-all")
                                .onClick(() -> count.update(value -> value + 1))
                );
    }
}
