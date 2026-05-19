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
        assertTrue(html.contains("Signals are the state primitive"));
        assertTrue(html.contains("Signals.signal"));
        assertTrue(html.contains("Signals.computed"));
        assertTrue(html.contains("Computed signal contract"));
    }

    @Test
    void lifecyclePageExplainsRenderEventAndCleanupFlow() {
        String html = new LifecyclePage(new AppTheme()).render().render();

        assertTrue(html.contains("<main"));
        assertTrue(html.contains("Lifecycle behavior explains"));
        assertTrue(html.contains("event handlers update server state"));
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
    }

    private static void assertThemeCompatible(String html) {
        assertTrue(html.contains("bg-slate-950"));
        assertTrue(html.contains("bg-slate-900"));
        assertTrue(html.contains("text-slate-100") || html.contains("text-zinc-50"));
        assertTrue(html.contains("border-slate-800") || html.contains("border-zinc-800"));
    }
}
