package ujfe.live;

import org.junit.jupiter.api.Test;
import ujfe.core.ClientState;
import ujfe.core.Node;
import ujfe.core.Ujfe;
import ujfe.router.Page;
import ujfe.router.Router;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ujfe.html.UI.button;
import static ujfe.html.UI.div;
import static ujfe.html.UI.h1;
import static ujfe.html.UI.p;

final class LiveSessionTest {
    @Test
    void handlesEventAndReturnsUpdatedHtml() {
        try (LiveSession session = new LiveSession(new Router().register(new CounterPage()))) {
            String document = session.renderDocument("/", ClientState.empty());
            assertTrue(document.contains("Counter: 0"));
            assertTrue(document.contains("data-ujfe-event="));
            assertTrue(document.contains(".bg-blue-600"));

            String eventId = extractEventId(document);
            LiveRenderResult result = session.handleEvent(eventId, ClientState.empty());

            assertTrue(result.html().contains("Counter: 1"));
            assertTrue(result.html().contains("data-ujfe-event="));
        }
    }

    @Test
    void exposesClientStateDuringRender() {
        ClientState clientState = ClientState.of(
                Map.of("ujfe_demo", "ativo"),
                Map.of("ujfe.theme", "dark")
        );

        String document;
        try (LiveSession session = new LiveSession(new Router().register(new ClientStatePage()))) {
            document = session.renderDocument("/", clientState);
        }

        assertTrue(document.contains("Cookie: ativo"));
        assertTrue(document.contains("Theme: dark"));
    }

    @Test
    void includesDevToolsScriptWhenEnabled() {
        String document;
        try (LiveSession session = new LiveSession(
                new Router().register(new CounterPage()), ujfe.html.CssTheme::defaultTheme, true)) {
            document = session.renderDocument("/", ClientState.empty());
        }

        assertTrue(document.contains("<script src=\"/_ujfe/dev.js\"></script>"));
    }

    @Test
    void rendersCssForDevPreviewClasses() {
        String css;
        try (LiveSession session = new LiveSession(new Router().register(new CounterPage()))) {
            css = session.renderCss(Set.of("p-10", "gap-10", "border-primary-300", "bg-primary-200"));
        }

        assertTrue(css.contains(".p-10{padding:2.5rem;}"));
        assertTrue(css.contains(".gap-10{gap:2.5rem;}"));
        assertTrue(css.contains(".border-primary-300{border-color:var(--ujfe-primary-300);}"));
        assertTrue(css.contains(".bg-primary-200{background-color:var(--ujfe-primary-200);}"));
    }

    @Test
    void supportsExternalCssAndDocumentHeadConfiguration() {
        LiveSessionConfig config = LiveSessionConfig.builder()
                .cssMode(CssMode.EXTERNAL)
                .lang("pt-BR")
                .title("UJFE <External>")
                .externalStylesheet("/app.css")
                .build();

        String document;
        String css;
        try (LiveSession session = new LiveSession(new Router().register(new CounterPage()), config)) {
            document = session.renderDocument("/", ClientState.empty());
            css = session.renderCss(Set.of("bg-blue-600"));
        }

        assertTrue(document.contains("<html lang=\"pt-BR\">"));
        assertTrue(document.contains("<title>UJFE &lt;External&gt;</title>"));
        assertTrue(document.contains("<link rel=\"stylesheet\" href=\"/app.css\">"));
        assertFalse(document.contains("data-ujfe-css"));
        assertFalse(document.contains(".bg-blue-600"));
        assertTrue(css.isEmpty());
    }

    private static String extractEventId(String html) {
        Matcher matcher = Pattern.compile("data-ujfe-event=\"([^\"]+)\"").matcher(html);
        assertTrue(matcher.find());
        return matcher.group(1);
    }

    @Page("/")
    public static final class CounterPage {
        private final AtomicInteger count = new AtomicInteger();

        public Node render() {
            return div()
                    .css("min-h-screen p-8 flex flex-col gap-4")
                    .child(h1("UJFE").css("text-3xl font-bold"))
                    .child(p(() -> "Counter: " + count.get()))
                    .child(
                            button("Incrementar")
                                    .css("px-4 py-2 rounded bg-blue-600 text-white")
                                    .onClick(count::incrementAndGet)
                    );
        }
    }

    @Page("/")
    public static final class ClientStatePage {
        public Node render() {
            return div()
                    .child(p(() -> "Cookie: " + Ujfe.cookie("ujfe_demo").orElse("missing")))
                    .child(p(() -> "Theme: " + Ujfe.localStorage("ujfe.theme").orElse("missing")));
        }
    }
}
