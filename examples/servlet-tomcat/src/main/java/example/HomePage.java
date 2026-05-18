package example;

import ujfe.core.Node;
import ujfe.router.Page;
import ujfe.signals.Signal;
import ujfe.signals.Signals;

import static ujfe.core.UI.*;

@Page("/home")
public final class HomePage {
    private final Signal<Integer> clicks = Signals.signal(0);

    public Node render() {
        return main()
                .css("min-h-screen bg-slate-50 text-slate-900 p-8 flex flex-col gap-4")
                .child(h1("UJFE Servlet"))
                .child(p("Plain Jakarta Servlet runtime, no Spring Boot required."))
                .child(p(() -> "Clicks: " + clicks.get()).css("font-semibold text-indigo-700"))
                .child(div()
                        .child(button("Increment")
                                .css("px-4 h-10 rounded-lg bg-indigo-600 text-white font-semibold")
                                .onClick(() -> clicks.update(value -> value + 1))));
    }
}
