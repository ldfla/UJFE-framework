package app.components;

import ujfe.core.Component;
import ujfe.core.Node;
import ujfe.signals.Signal;
import ujfe.signals.Signals;

import java.util.Objects;
import java.util.function.BooleanSupplier;

import static ujfe.core.UI.*;

public final class CounterComponent implements Component {
    private final BooleanSupplier darkMode;
    private final Signal<Integer> count = Signals.signal(0);

    public CounterComponent(BooleanSupplier darkMode) {
        this.darkMode = Objects.requireNonNull(darkMode, "darkMode");
    }

    @Override
    public Node render() {
        return div()
                .css(cardClass())
                .child(
                        div()
                                .css("flex flex-col gap-2")
                                .child(h2("Counter live").css(titleClass()))
                                .child(p("Estado mantido no servidor por meio de arquitetura baseada em Signals.")
                                        .css(bodyClass()))
                )
                .child(
                        div()
                                .css("py-4 text-center")
                                .child(
                                        p()
                                                .css(counterTextClass())
                                                .child("Counter: ")
                                                .child(span().child(() -> String.valueOf(count.get())).css("text-primary-500"))
                                )
                )
                .child(
                        button("Incrementar via RPC")
                                .css("w-full px-4 h-10 rounded-lg bg-primary-600 hover:bg-primary-700 text-white font-medium text-sm shadow-sm active:scale-[0.98] transition-all")
                                .onClick(() -> count.update(value -> value + 1))
                );
    }

    private String cardClass() {
        return darkMode.getAsBoolean()
                ? "rounded-lg border border-slate-800 bg-slate-900 p-6 shadow-sm flex flex-col justify-between gap-5"
                : "rounded-lg border border-slate-200/60 bg-white p-6 shadow-sm flex flex-col justify-between gap-5";
    }

    private String titleClass() {
        return darkMode.getAsBoolean()
                ? "text-lg font-bold text-slate-100"
                : "text-lg font-bold text-slate-900";
    }

    private String bodyClass() {
        return darkMode.getAsBoolean()
                ? "text-xs text-slate-300 leading-relaxed"
                : "text-xs text-slate-500 leading-relaxed";
    }

    private String counterTextClass() {
        return darkMode.getAsBoolean()
                ? "text-5xl font-black text-slate-100 leading-none"
                : "text-5xl font-black text-slate-900 leading-none";
    }
}
