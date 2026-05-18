package app.components;

import app.AppTheme;
import ujfe.core.Component;
import ujfe.core.Element;
import ujfe.core.Node;

import java.util.Objects;

import static ujfe.core.UI.*;

public final class AppHeader implements Component {
    public static final String HOME = "/";
    public static final String DOCS = "/docs";
    public static final String FORMS = "/forms";
    public static final String SIGNALS = "/signals";
    public static final String LIFECYCLE = "/lifecycle";
    public static final String ACTIONS = "/runtime-actions";

    private final AppTheme theme;
    private final String activePath;

    public AppHeader(AppTheme theme, String activePath) {
        this.theme = Objects.requireNonNull(theme, "theme");
        this.activePath = Objects.requireNonNull(activePath, "activePath");
    }

    public static Element pageShell(AppTheme theme) {
        return div()
            .attr("data-ujfe-dev-preview", String.valueOf(theme.devPreviewEnabled()))
            .css(pageShellClass(theme));
    }

    public static String pageShellClass(AppTheme theme) {
        return theme.darkMode()
            ? "min-h-screen bg-slate-950 text-zinc-50 font-sans antialiased"
            : "min-h-screen bg-slate-50 text-slate-900 font-sans antialiased";
    }

    @Override
    public Node render() {
        return header()
            .css(theme.darkMode()
                ? "sticky top-0 z-50 border-b border-slate-800 bg-slate-900 backdrop-blur-md"
                : "sticky top-0 z-50 border-b border-slate-200/80 bg-white/80 backdrop-blur-md")
            .child(
                div()
                    .css("max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 min-h-16 py-3 flex items-center justify-between gap-4")
                    .child(brand())
                    .child(navigation())
            );
    }

    private Node brand() {
        return div()
            .css("flex items-center gap-3 shrink-0")
            .child(
                a("UJFE")
                    .href(HOME)
                    .css("px-2.5 py-1 text-xs font-black bg-primary-600 text-white rounded shadow-sm")
            )
            .child(
                h1("Modern Reactive UI Framework")
                    .css(theme.darkMode()
                        ? "hidden md:block text-sm font-semibold text-slate-100"
                        : "hidden md:block text-sm font-semibold text-slate-900")
            );
    }

    private Node navigation() {
        return nav()
            .css("flex flex-wrap items-center justify-end gap-2")
            .child(navLink("Home", HOME))
            .child(navLink("Documentation", DOCS))
            .child(navLink("Forms", FORMS))
            .child(navLink("Signals", SIGNALS))
            .child(navLink("Lifecycle", LIFECYCLE))
            .child(navLink("Actions", ACTIONS))
            .child(featureToggle("Dark mode", theme.darkMode(), theme::toggleDarkMode))
            .child(featureToggle("UJFE Dev Preview", theme.devPreviewEnabled(), theme::toggleDevPreview))
            .child(span("JVM SSR").css(statusPillClass()));
    }

    private Node navLink(String label, String path) {
        return a(label)
            .href(path)
            .attr("aria-current", isActive(path) ? "page" : null)
            .css(navLinkClass(path));
    }

    private boolean isActive(String path) {
        return activePath.equals(path);
    }

    private String navLinkClass(String path) {
        if (isActive(path)) {
            return theme.darkMode()
                ? "text-sm font-semibold text-primary-100 bg-primary-950 border border-primary-700 rounded-md px-2 py-1 transition-colors"
                : "text-sm font-semibold text-primary-700 bg-primary-50 border border-primary-200 rounded-md px-2 py-1 transition-colors";
        }
        return theme.darkMode()
            ? "text-sm font-medium text-slate-100 hover:text-primary-200 border border-transparent rounded-md px-2 py-1 transition-colors"
            : "text-sm font-medium text-slate-600 hover:text-slate-900 border border-transparent rounded-md px-2 py-1 transition-colors";
    }

    private String statusPillClass() {
        return theme.darkMode()
            ? "inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-secondary-950 text-secondary-200 border border-secondary-700"
            : "inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium bg-secondary-50 text-secondary-700 border border-secondary-200";
    }

    private Node featureToggle(String label, boolean active, Runnable action) {
        String buttonClass = theme.darkMode()
            ? "inline-flex items-center gap-2 px-3 py-1 rounded-full border border-slate-800 bg-slate-900 text-slate-100 hover:bg-slate-800 text-xs font-semibold transition-colors"
            : "inline-flex items-center gap-2 px-3 py-1 rounded-full border border-slate-200 bg-white text-slate-700 hover:bg-primary-50 text-xs font-semibold transition-colors";
        String stateClass = active
            ? "px-2 py-1 rounded-full bg-primary-600 text-white"
            : (theme.darkMode()
               ? "px-2 py-1 rounded-full bg-slate-800 text-slate-300"
               : "px-2 py-1 rounded-full bg-slate-100 text-slate-500");
        return button()
            .attr("data-ujfe-dev-control", "true")
            .attr("aria-pressed", String.valueOf(active))
            .css(buttonClass)
            .child(span(label))
            .child(span(active ? "On" : "Off").css(stateClass))
            .onClick(action);
    }
}
