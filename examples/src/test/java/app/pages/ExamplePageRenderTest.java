package app.pages;

import app.AppTheme;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class ExamplePageRenderTest {
    @Test
    void mainPageRendersSemanticExampleShell() {
        String html = new MainPage(new AppTheme()).render().render();

        assertTrue(html.contains("<main"));
        assertTrue(html.contains("Reactive server-rendered UI"));
        assertTrue(html.contains("Read the docs"));
        assertFalse(html.contains("lorem"));
    }

    @Test
    void documentationPageExplainsCoreDocumentationTopics() {
        String html = new DocumentationPage(new AppTheme()).render().render();

        assertTrue(html.contains("<main"));
        assertTrue(html.contains("How to use UJFE documentation"));
        assertTrue(html.contains("@Page"));
        assertTrue(html.contains("Router"));
        assertTrue(html.contains("CSS modes"));
        assertTrue(html.contains("LiveSession"));
    }

    @Test
    void signalsPageExplainsSignalUsageAndCurrentApi() {
        String html = new SignalsPage(new AppTheme()).render().render();

        assertTrue(html.contains("<main"));
        assertTrue(html.contains("Signals for checkout state"));
        assertTrue(html.contains("Checkout estimator"));
        assertTrue(html.contains("cart, filters, form drafts, live previews"));
        assertTrue(html.contains("Signals.signal"));
        assertTrue(html.contains("Signals.computed"));
        assertTrue(html.contains("Computed signal contract"));
    }

    @Test
    void lifecyclePageExplainsRenderEventAndCleanupFlow() {
        String html = new LifecyclePage(new AppTheme()).render().render();

        assertTrue(html.contains("<main"));
        assertTrue(html.contains("Lifecycle behavior explains"));
        assertTrue(html.contains("live order page"));
        assertTrue(html.contains("subscribe to order updates"));
        assertTrue(html.contains("onUnmount"));
        assertTrue(html.contains("Route transition"));
    }

    @Test
    void documentationSignalsAndLifecycleRenderDarkModeCompatibleSurfaces() {
        AppTheme theme = new AppTheme();
        theme.toggleDarkMode();

        String documentation = new DocumentationPage(theme).render().render();
        String signals = new SignalsPage(theme).render().render();
        String lifecycle = new LifecyclePage(theme).render().render();

        assertThemeCompatible(documentation);
        assertThemeCompatible(signals);
        assertThemeCompatible(lifecycle);
        assertFalse(documentation.contains("bg-white p-4 shadow-sm"));
        assertFalse(signals.contains("bg-white p-6 shadow-sm"));
        assertFalse(lifecycle.contains("bg-white p-6 shadow-sm"));
        assertDocumentationDarkModeCompatible(documentation);
    }

    private static void assertThemeCompatible(String html) {
        assertTrue(html.contains("bg-slate-950"));
        assertTrue(html.contains("bg-slate-900"));
        assertTrue(html.contains("text-slate-100") || html.contains("text-zinc-50"));
        assertTrue(html.contains("border-slate-800") || html.contains("border-zinc-800"));
    }

    private static void assertDocumentationDarkModeCompatible(String html) {
        assertTrue(html.contains("bg-slate-950 p-2 text-sm text-slate-100"));
        assertTrue(html.contains("border-zinc-800 bg-zinc-950 text-zinc-50"));
        assertTrue(html.contains("text-primary-200") || html.contains("text-secondary-200"));
        assertFalse(html.matches("(?s).*class=\"[^\"]*bg-white[^\"]*\".*"));
        assertFalse(html.matches("(?s).*class=\"[^\"]*text-slate-700[^\"]*\".*"));
        assertFalse(html.contains("bg-primary-50 p-6 shadow-sm"));
        assertFalse(html.contains("bg-rose-50 p-6 shadow-sm"));
    }
}
