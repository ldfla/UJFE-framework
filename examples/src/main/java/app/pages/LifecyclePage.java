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
    private final LifecycleResourceComponent resourceComponent;

    public LifecyclePage(AppTheme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
        this.resourceComponent = new LifecycleResourceComponent(theme::darkMode);
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
            .css(cardClass("p-8 sm:p-10 flex flex-col gap-6"))
            .child(span("Runtime lifecycle").css(kickerClass()))
            .child(h1("Lifecycle for pages that open real server resources")
                .css(heroTitleClass()))
            .child(p("Lifecycle behavior explains what happens when a user opens a live page, clicks controls that update server state, and then navigates away. Use it for resources that need a clear start and cleanup point.")
                .css(bodyClass("text-base max-w-3xl")))
            .child(
                div()
                    .css("grid grid-cols-1 md:grid-cols-3 gap-4")
                    .child(metric("Open", "Start a subscription, observer, or handle when the page appears."))
                    .child(metric("Update", "Event handlers update server state without recreating stable resources."))
                    .child(metric("Cleanup", "Close resources on route change or session shutdown."))
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
            .css(cardClass("p-6 flex flex-col gap-5"))
            .child(h2("Order page lifecycle").css(titleClass()))
            .child(p("A live order page renders the current order, mounts the page once, then event handlers update server state and trigger re-rendering. The mounted page instance is reused while the user stays on the same route.")
                .css(bodyClass("text-sm")))
            .child(
                div()
                    .css(codePanelClass())
                    .child(p(() -> "Order page mounted : " + pageMounts.get()).css("font-semibold text-indigo-600"))
                    .child(p(() -> "Order page cleaned : " + pageUnmounts.get()).css(mutedTextClass()))
                    .child(p(() -> "Status refreshes   : " + refreshes.get()).css(mutedTextClass()))
            )
            .child(
                button("Refresh order timeline")
                    .css("w-full px-4 h-10 rounded-lg bg-slate-900 hover:bg-slate-800 text-white font-medium text-sm shadow-sm active:scale-[0.98] transition-all")
                    .onClick(() -> refreshes.update(value -> value + 1))
            );
    }

    private Node cleanupPatternPanel() {
        return div()
            .css(cardClass("p-6 flex flex-col gap-5"))
            .child(h2("Cleanup pattern").css(titleClass()))
            .child(p("Use onMount for work that should start when the live page becomes active: subscribe to order updates, attach an observer, or allocate a scoped handle. Use onUnmount to release it.")
                .css(bodyClass("text-sm")))
            .child(
                div()
                    .css(theme.darkMode()
                        ? "rounded-lg border border-indigo-800 bg-slate-950 p-6"
                        : "rounded-lg border border-indigo-100 bg-indigo-50 p-6")
                    .child(strong("Do not fetch a whole report or run slow business logic inside lifecycle callbacks. Start lightweight resources there and release them predictably in onUnmount.").css(theme.darkMode()
                        ? "text-sm text-indigo-200"
                        : "text-sm text-indigo-950"))
            );
    }

    private Node routeTransitionPanel() {
        return div()
            .css(cardClass("p-6 flex flex-col gap-5"))
            .child(h2("Route transition").css(titleClass()))
            .child(p("Move to another route to unmount this order page and its live subscription. Return here to mount them again in the same server session.")
                .css(bodyClass("text-sm")))
            .child(
                div()
                    .css("flex gap-3")
                    .child(a("Open runtime actions").attr("href", "/runtime-actions")
                        .css("px-4 py-2 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white font-medium text-sm transition-all"))
                    .child(a("Open docs").attr("href", "/docs")
                        .css(theme.darkMode()
                            ? "px-4 py-2 rounded-lg border border-slate-700 bg-slate-900 text-slate-100 font-medium text-sm transition-all"
                            : "px-4 py-2 rounded-lg border border-slate-200 bg-white text-slate-700 font-medium text-sm transition-all"))
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
}
