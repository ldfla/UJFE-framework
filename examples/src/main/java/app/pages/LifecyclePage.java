package app.pages;

import app.AppTheme;
import app.components.AppHeader;
import app.components.LifecycleResourceComponent;
import ujfe.core.Component;
import ujfe.core.Lifecycle;
import ujfe.core.Node;
import ujfe.router.Page;
import ujfe.signals.Signal;
import ujfe.signals.Signals;

import java.util.Objects;

import static ujfe.core.UI.*;

@Page("/lifecycle")
public final class LifecyclePage implements Component, Lifecycle {
    private final AppTheme theme;
    private final Signal<Integer> pageMounts = Signals.signal(0);
    private final Signal<Integer> pageUnmounts = Signals.signal(0);
    private final Signal<Integer> refreshes = Signals.signal(0);
    private final LifecycleResourceComponent resourceComponent = new LifecycleResourceComponent();

    public LifecyclePage(AppTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
    }

    @Override
    public void onMount() {
        pageMounts.update(value -> value + 1);
    }

    @Override
    public void onUnmount() {
        pageUnmounts.update(value -> value + 1);
    }

    @Override
    public Node render() {
        return AppHeader.pageShell(theme)
                .child(new AppHeader(theme, AppHeader.LIFECYCLE).render())
                .child(
                        main()
                                .css("max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10 flex flex-col gap-8")
                                .child(hero())
                                .child(panels())
                );
    }

    private Node hero() {
        return section()
                .css("rounded-lg border border-slate-200/80 bg-white p-8 sm:p-10 shadow-sm flex flex-col gap-6")
                .child(span("Runtime lifecycle").css("text-xs font-semibold uppercase text-indigo-600"))
                .child(h1("Deterministic mount and cleanup for live server components")
                        .css("text-3xl sm:text-4xl font-extrabold text-slate-900 leading-none"))
                .child(p("Lifecycle callbacks run on the server and are tied to live session rendering, route transitions, and session shutdown.")
                        .css("text-base text-slate-600 leading-relaxed max-w-3xl"))
                .child(
                        div()
                                .css("grid grid-cols-1 md:grid-cols-3 gap-4")
                                .child(metric("Mount once", "Stable component instances do not remount on ordinary re-render."))
                                .child(metric("Unmount once", "Removed components are cleaned up exactly once."))
                                .child(metric("Runtime-safe", "Failures are routed through runtime error actions."))
                );
    }

    private Node panels() {
        return section()
                .css("grid grid-cols-1 md:grid-cols-2 gap-6")
                .child(pageLifecyclePanel())
                .child(component(resourceComponent))
                .child(cleanupPatternPanel())
                .child(routeTransitionPanel());
    }

    private Node pageLifecyclePanel() {
        return div()
                .css("rounded-lg border border-slate-200/60 bg-white p-6 shadow-sm flex flex-col gap-5")
                .child(h2("Page lifecycle").css("text-lg font-bold text-slate-900"))
                .child(
                        div()
                                .css("flex flex-col gap-2.5 bg-slate-50 border border-slate-100 rounded-lg p-4 font-mono text-xs text-slate-600")
                                .child(p(() -> "Page mounts   : " + pageMounts.get()).css("font-semibold text-indigo-600"))
                                .child(p(() -> "Page unmounts : " + pageUnmounts.get()).css("text-slate-500"))
                                .child(p(() -> "Refresh events: " + refreshes.get()).css("text-slate-500"))
                )
                .child(
                        button("Refresh lifecycle view")
                                .css("w-full px-4 h-10 rounded-lg bg-slate-900 hover:bg-slate-800 text-white font-medium text-sm shadow-sm active:scale-[0.98] transition-all")
                                .onClick(() -> refreshes.update(value -> value + 1))
                );
    }

    private Node cleanupPatternPanel() {
        return div()
                .css("rounded-lg border border-slate-200/60 bg-white p-6 shadow-sm flex flex-col gap-5")
                .child(h2("Cleanup pattern").css("text-lg font-bold text-slate-900"))
                .child(p("Use onMount for server resources such as subscriptions, handles, or observers. Use onUnmount to release them when the component leaves the live tree.")
                        .css("text-sm text-slate-600 leading-relaxed"))
                .child(
                        div()
                                .css("rounded-lg border border-indigo-100 bg-gradient-to-br from-indigo-50/40 to-slate-50/20 p-6")
                                .child(strong("No browser lifecycle hooks are required.").css("text-sm text-indigo-950"))
                );
    }

    private Node routeTransitionPanel() {
        return div()
                .css("rounded-lg border border-slate-200/60 bg-white p-6 shadow-sm flex flex-col gap-5")
                .child(h2("Route transition").css("text-lg font-bold text-slate-900"))
                .child(p("Move to another route to unmount this page and its nested resource component. Return here to mount them again in the same server session.")
                        .css("text-sm text-slate-600 leading-relaxed"))
                .child(
                        div()
                                .css("flex gap-3")
                                .child(a("Open runtime actions").attr("href", "/runtime-actions")
                                        .css("px-4 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white font-medium text-sm transition-all"))
                                .child(a("Open docs").attr("href", "/docs")
                                        .css("px-4 py-2 rounded-lg border border-slate-200 bg-white text-slate-700 font-medium text-sm transition-all"))
                );
    }

    private Node metric(String title, String body) {
        return div()
                .css("rounded-lg border border-slate-100 bg-slate-50/50 p-5 flex flex-col gap-1.5")
                .child(strong(title).css("text-sm font-semibold text-slate-900"))
                .child(span(body).css("text-xs text-slate-500 leading-relaxed"));
    }
}
