package ujfe.live;

import org.junit.jupiter.api.Test;
import ujfe.core.ClientState;
import ujfe.core.Component;
import ujfe.core.Node;
import ujfe.core.Ujfe;
import ujfe.router.Page;
import ujfe.router.Router;
import ujfe.validation.ValidationException;
import ujfe.validation.ValidationMode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static ujfe.core.UI.*;

final class LiveSessionTest {
    @Test
    void handlesEventAndReturnsUpdatedHtml() {
        try (LiveSession session = new LiveSession(new Router().register(new CounterPage()))) {
            String document = session.renderDocument("/", ClientState.empty());
            assertTrue(document.contains("Counter: 0"));
            assertTrue(document.contains("data-ujfe-event="));
            assertTrue(document.contains(".bg-blue-600"));

            String eventId = extractEventId(document);
            LiveRenderResult result = session.handleEvent(eventId, ClientState.empty(), new LiveHttpRequestMetadata(session.csrfToken(), "http://localhost", null, "localhost", "http"));

            assertTrue(result.html()
                .contains("Counter: 1"));
            assertTrue(result.html()
                .contains("data-ujfe-event="));
        }
    }

    @Test
    void rejectsEventDispatchWithoutHttpMetadataWhenCsrfIsEnabled() {
        try (LiveSession session = new LiveSession(new Router().register(new CounterPage()))) {
            String document = session.renderDocument("/", ClientState.empty());
            String eventId = extractEventId(document);

            LiveCsrfException exception = assertThrows(LiveCsrfException.class,
                () -> session.handleEvent(eventId, ClientState.empty()));

            assertEquals(LiveHttpFailureCategory.MISSING_CSRF_TOKEN, exception.category());
            assertEquals("Missing CSRF token.", exception.safeMessage());
        }
    }

    @Test
    void rejectsClientStateUpdateWithoutHttpMetadataWhenCsrfIsEnabled() {
        try (LiveSession session = new LiveSession(new Router().register(new CounterPage()))) {
            session.renderDocument("/", ClientState.empty());

            LiveCsrfException exception = assertThrows(LiveCsrfException.class,
                () -> session.updateClientState(ClientState.empty()));

            assertEquals(LiveHttpFailureCategory.MISSING_CSRF_TOKEN, exception.category());
            assertEquals("Missing CSRF token.", exception.safeMessage());
        }
    }

    @Test
    void exposesClientStateDuringRender() {
        ClientState clientState = ClientState.of(
            Map.of("ujfe_demo", "ativo"),
            Map.of("ujfe.theme", "dark"),
            Map.of("ujfe.tab", "docs")
        );

        String document;
        LiveSessionConfig config = LiveSessionConfig.builder()
            .allowClientCookie("ujfe_demo")
            .allowLocalStorageKey("ujfe.theme")
            .allowSessionStorageKey("ujfe.tab")
            .build();
        try (LiveSession session = new LiveSession(new Router().register(new ClientStatePage()), config)) {
            document = session.renderDocument("/", clientState);
        }

        assertTrue(document.contains("Cookie: ativo"));
        assertTrue(document.contains("Theme: dark"));
        assertTrue(document.contains("Tab: docs"));
        assertTrue(document.contains("data-ujfe-client-state-cookies=\"[&quot;ujfe_demo&quot;]\""));
        assertTrue(document.contains("data-ujfe-local-storage-keys=\"[&quot;ujfe.theme&quot;]\""));
        assertTrue(document.contains("data-ujfe-session-storage-keys=\"[&quot;ujfe.tab&quot;]\""));
    }

    @Test
    void clientStatePolicyBlocksUnauthorizedStateBeforeRender() {
        ClientState clientState = ClientState.of(
            Map.of("ujfe_demo", "ativo", "secret", "hidden"),
            Map.of("ujfe.theme", "dark", "token", "hidden"),
            Map.of("ujfe.tab", "docs", "draft", "hidden")
        );
        LiveSessionConfig config = LiveSessionConfig.builder()
            .allowClientCookie("ujfe_demo")
            .allowLocalStorageKey("ujfe.theme")
            .allowSessionStorageKey("ujfe.tab")
            .build();

        String document;
        try (LiveSession session = new LiveSession(new Router().register(new ClientStatePage()), config)) {
            document = session.renderDocument("/", clientState);
        }

        assertTrue(document.contains("Cookie: ativo"));
        assertTrue(document.contains("Theme: dark"));
        assertTrue(document.contains("Tab: docs"));
        assertTrue(document.contains("Secret: missing"));
        assertTrue(document.contains("Token: missing"));
        assertTrue(document.contains("Draft: missing"));
    }

    @Test
    void allowedClientStateReachesLiveEventHandler() {
        LiveSessionConfig config = LiveSessionConfig.builder()
            .allowClientCookie("ujfe_demo")
            .allowLocalStorageKey("ujfe.theme")
            .allowSessionStorageKey("ujfe.tab")
            .build();
        try (LiveSession session = new LiveSession(new Router().register(new ClientStateEventPage()), config)) {
            String document = session.renderDocument("/", ClientState.empty());
            String eventId = extractEventId(document);
            ClientState nextClientState = ClientState.of(
                Map.of("ujfe_demo", "ativo", "secret", "hidden"),
                Map.of("ujfe.theme", "dark", "token", "hidden"),
                Map.of("ujfe.tab", "docs", "draft", "hidden")
            );

            LiveRenderResult result = session.handleEvent(
                eventId,
                nextClientState,
                new LiveHttpRequestMetadata(session.csrfToken(), "http://localhost", null, "localhost", "http")
            );

            assertTrue(result.html()
                .contains("Seen: ativo/dark/docs"));
            assertTrue(result.html()
                .contains("Blocked: none/none/none"));
        }
    }

    @Test
    void includesDevToolsScriptWhenEnabled() {
        String document;
        try (LiveSession session = new LiveSession(
            new Router().register(new CounterPage()), ujfe.core.CssTheme::defaultTheme, true)) {
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
        assertTrue(document.contains("bg-blue-600"));
        assertTrue(css.isEmpty());
    }

    @Test
    void supportsNoneCssModeWithoutRemovingClassAttributes() {
        LiveSessionConfig config = LiveSessionConfig.builder()
            .cssMode(CssMode.NONE)
            .externalStylesheet("/app.css")
            .build();

        String document;
        String css;
        try (LiveSession session = new LiveSession(new Router().register(new CounterPage()), config)) {
            document = session.renderDocument("/", ClientState.empty());
            css = session.renderCss(Set.of("bg-blue-600"));
        }

        assertFalse(document.contains("data-ujfe-css"));
        assertFalse(document.contains("<link rel=\"stylesheet\" href=\"/app.css\">"));
        assertTrue(document.contains("class=\"min-h-screen p-8 flex flex-col gap-4\""));
        assertTrue(css.isEmpty());
    }

    @Test
    void validationIsOffByDefaultDuringDocumentRendering() {
        String document;
        try (LiveSession session = new LiveSession(new Router().register(new MissingMainPage()))) {
            document = session.renderDocument("/", ClientState.empty());
        }

        assertTrue(document.contains("<h1>Missing main</h1>"));
    }

    @Test
    void warningValidationModeDoesNotFailDocumentRendering() {
        LiveSessionConfig config = LiveSessionConfig.builder()
            .validationMode(ValidationMode.WARN)
            .accessibilityValidationEnabled(true)
            .build();

        String document;
        try (ValidationLogCapture logs = ValidationLogCapture.attach();
             LiveSession session = new LiveSession(new Router().register(new MissingMainPage()), config)) {
            document = session.renderDocument("/", ClientState.empty());
            assertTrue(logs.messages()
                .stream()
                .anyMatch(message -> message.contains("accessibility.document.single-main")));
        }

        assertTrue(document.contains("<h1>Missing main</h1>"));
    }

    @Test
    void strictValidationModeFailsDocumentRendering() {
        LiveSessionConfig config = LiveSessionConfig.builder()
            .validationMode(ValidationMode.STRICT)
            .accessibilityValidationEnabled(true)
            .build();

        try (LiveSession session = new LiveSession(new Router().register(new MissingMainPage()), config)) {
            ValidationException exception = assertThrows(ValidationException.class,
                () -> session.renderDocument("/", ClientState.empty()));

            assertTrue(exception.getMessage()
                .contains("accessibility.document.single-main"));
        }
    }

    @Test
    void internalCssIsScopedToClassesSeenDuringCurrentPageRender() {
        Router router = new Router()
            .register(new FirstCssPage())
            .register(new SecondCssPage());

        String first;
        String second;
        try (LiveSession session = new LiveSession(router)) {
            first = session.renderDocument("/one", ClientState.empty());
            second = session.renderDocument("/two", ClientState.empty());
        }

        assertTrue(first.contains(".p-4{padding:1rem;}"));
        assertFalse(first.contains(".m-4{margin:1rem;}"));
        assertTrue(second.contains(".m-4{margin:1rem;}"));
        assertFalse(second.contains(".p-4{padding:1rem;}"));
    }

    @Test
    void exposesRoutePresenceWithoutRenderingPage() {
        try (LiveSession session = new LiveSession(new Router().register(new CounterPage()))) {
            assertTrue(session.hasRoute("/"));
            assertFalse(session.hasRoute("/poster.png"));
        }
    }

    @Test
    void handlesInputEventWithValuePayload() {
        try (LiveSession session = new LiveSession(new Router().register(new TypedEventPage()))) {
            String document = session.renderDocument("/", ClientState.empty());
            String eventId = extractNamedEventId(document, "input");

            LiveRenderResult result = session.handleEvent(
                eventId,
                "Ada",
                ClientState.empty(),
                new LiveHttpRequestMetadata(session.csrfToken(), "http://localhost", null, "localhost", "http")
            );

            assertTrue(result.html()
                .contains("Input: Ada"));
        }
    }

    @Test
    void handlesChangeEventWithValuePayload() {
        try (LiveSession session = new LiveSession(new Router().register(new TypedEventPage()))) {
            String document = session.renderDocument("/", ClientState.empty());
            String eventId = extractNamedEventId(document, "change");

            LiveRenderResult result = session.handleEvent(
                eventId,
                "backend",
                ClientState.empty(),
                new LiveHttpRequestMetadata(session.csrfToken(), "http://localhost", null, "localhost", "http")
            );

            assertTrue(result.html()
                .contains("Change: backend"));
        }
    }

    @Test
    void handlesSubmitEventWithoutFullPageReload() {
        try (LiveSession session = new LiveSession(new Router().register(new TypedEventPage()))) {
            String document = session.renderDocument("/", ClientState.empty());
            String eventId = extractNamedEventId(document, "submit");

            LiveRenderResult result = session.handleEvent(
                eventId,
                "name=Ada",
                ClientState.empty(),
                new LiveHttpRequestMetadata(session.csrfToken(), "http://localhost", null, "localhost", "http")
            );

            assertTrue(result.html()
                .contains("Submits: 1"));
        }
    }

    @Test
    void typedHandlerFailurePropagatesSafely() {
        try (LiveSession session = new LiveSession(new Router().register(new FailingTypedEventPage()))) {
            String document = session.renderDocument("/", ClientState.empty());
            String eventId = extractNamedEventId(document, "input");

            IllegalStateException exception = assertThrows(IllegalStateException.class, () -> session.handleEvent(
                eventId,
                "danger",
                ClientState.empty(),
                new LiveHttpRequestMetadata(session.csrfToken(), "http://localhost", null, "localhost", "http")
            ));

            assertEquals("typed failure", exception.getMessage());
        }
    }

    private static String extractEventId(String html) {
        Matcher matcher = Pattern.compile("data-ujfe-event=\"([^\"]+)\"")
            .matcher(html);
        assertTrue(matcher.find());
        return matcher.group(1);
    }

    private static String extractNamedEventId(String html, String eventName) {
        Matcher matcher = Pattern.compile("data-ujfe-event-" + eventName + "=\"([^\"]+)\"")
            .matcher(html);
        assertTrue(matcher.find());
        return matcher.group(1);
    }

    private static final class ValidationLogCapture implements AutoCloseable {
        private final Logger logger = Logger.getLogger(LiveSession.class.getName());
        private final boolean useParentHandlers = logger.getUseParentHandlers();
        private final Level level = logger.getLevel();
        private final List<String> messages = new ArrayList<>();
        private final Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                messages.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };

        private ValidationLogCapture() {
            logger.setUseParentHandlers(false);
            logger.setLevel(Level.ALL);
            logger.addHandler(handler);
        }

        static ValidationLogCapture attach() {
            return new ValidationLogCapture();
        }

        List<String> messages() {
            return messages;
        }

        @Override
        public void close() {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(useParentHandlers);
            logger.setLevel(level);
        }
    }

    @Page("/")
    public static final class CounterPage implements Component {
        private final AtomicInteger count = new AtomicInteger();

        @Override
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
    public static final class MissingMainPage implements Component {
        @Override
        public Node render() {
            return div()
                .child(h1("Missing main"));
        }
    }

    @Page("/one")
    public static final class FirstCssPage implements Component {
        @Override
        public Node render() {
            return div().css("p-4")
                .child("One");
        }
    }

    @Page("/two")
    public static final class SecondCssPage implements Component {
        @Override
        public Node render() {
            return div().css("m-4")
                .child("Two");
        }
    }

    @Page("/")
    public static final class ClientStatePage implements Component {
        @Override
        public Node render() {
            return div()
                .child(p(() -> "Cookie: " + Ujfe.cookie("ujfe_demo")
                    .orElse("missing")))
                .child(p(() -> "Theme: " + Ujfe.localStorage("ujfe.theme")
                    .orElse("missing")))
                .child(p(() -> "Tab: " + Ujfe.sessionStorage("ujfe.tab")
                    .orElse("missing")))
                .child(p(() -> "Secret: " + Ujfe.cookie("secret")
                    .orElse("missing")))
                .child(p(() -> "Token: " + Ujfe.localStorage("token")
                    .orElse("missing")))
                .child(p(() -> "Draft: " + Ujfe.sessionStorage("draft")
                    .orElse("missing")));
        }
    }

    @Page("/")
    public static final class ClientStateEventPage implements Component {
        private final AtomicReference<String> seen = new AtomicReference<>("none");
        private final AtomicReference<String> blocked = new AtomicReference<>("none");

        @Override
        public Node render() {
            return div()
                .child(p(() -> "Seen: " + seen.get()))
                .child(p(() -> "Blocked: " + blocked.get()))
                .child(button("Read").onClick(() -> {
                    seen.set(Ujfe.cookie("ujfe_demo")
                        .orElse("none")
                        + "/" + Ujfe.localStorage("ujfe.theme")
                        .orElse("none")
                        + "/" + Ujfe.sessionStorage("ujfe.tab")
                        .orElse("none"));
                    blocked.set(Ujfe.cookie("secret")
                        .orElse("none")
                        + "/" + Ujfe.localStorage("token")
                        .orElse("none")
                        + "/" + Ujfe.sessionStorage("draft")
                        .orElse("none"));
                }));
        }
    }

    @Page("/")
    public static final class TypedEventPage implements Component {
        private final AtomicReference<String> inputValue = new AtomicReference<>("");
        private final AtomicReference<String> changeValue = new AtomicReference<>("");
        private final AtomicInteger submits = new AtomicInteger();

        @Override
        public Node render() {
            return div()
                .child(p(() -> "Input: " + inputValue.get()))
                .child(p(() -> "Change: " + changeValue.get()))
                .child(p(() -> "Submits: " + submits.get()))
                .child(inputText().onInput(inputValue::set))
                .child(select()
                    .onChange(changeValue::set)
                    .child(option("Backend").value("backend")))
                .child(form()
                    .onSubmit(submits::incrementAndGet)
                    .child(inputText().name("name")));
        }
    }

    @Page("/")
    public static final class FailingTypedEventPage implements Component {
        @Override
        public Node render() {
            return inputText().onInput(value -> {
                throw new IllegalStateException("typed failure");
            });
        }
    }
}
