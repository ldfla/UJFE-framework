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
                .css("rounded-lg border border-amber-200 bg-amber-50 p-4 shadow-sm flex flex-col gap-3")
                .child(h2("Counter live").css("text-2xl font-bold text-amber-800"))
                .child(h3("Estado mantido no servidor").css("text-lg font-semibold text-slate-700"))
                .child(p(() -> "Counter: " + count.get()).css("text-3xl font-bold text-slate-900"))
                .child(
                        button("Incrementar")
                                .css("px-4 py-2 rounded bg-blue-600 text-white font-semibold")
                                .onClick(() -> count.update(value -> value + 1))
                )
                .child(p("O clique envia um POST para /_ujfe/event, executa o Runnable Java e re-renderiza este trecho.")
                        .css("text-sm text-slate-700 leading-relaxed"));
    }
}
