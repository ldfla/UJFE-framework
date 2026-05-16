package ujfe.live;

import org.junit.jupiter.api.Test;
import ujfe.core.ClientState;
import ujfe.core.Component;
import ujfe.core.Node;
import ujfe.router.PageRenderer;
import ujfe.router.Page;
import ujfe.router.Router;
import ujfe.runtime.action.RuntimeActionRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ujfe.html.UI.button;
import static ujfe.html.UI.div;
import static ujfe.html.UI.meta;
import static ujfe.html.UI.p;

final class LiveSessionActionTest {

    // --- Before/After Render ---

    @Test
    void beforeRenderIsCalledDuringRenderPath() {
        List<String> log = new ArrayList<>();
        RuntimeActionRegistry actions = RuntimeActionRegistry.builder()
                .beforeRender(ctx -> log.add("before:" + ctx.path()))
                .build();

        LiveSession session = sessionWithActions(actions);
        session.renderPath("/");

        assertEquals(1, log.size());
        assertTrue(log.get(0).startsWith("before:/"));
    }

    @Test
    void afterRenderReceivesRenderedHtml() {
        List<String> log = new ArrayList<>();
        RuntimeActionRegistry actions = RuntimeActionRegistry.builder()
                .afterRender(result -> log.add("html:" + (result.html().contains("Hello") ? "yes" : "no")))
                .build();

        LiveSession session = sessionWithActions(actions);
        session.renderPath("/");

        assertEquals(List.of("html:yes"), log);
    }

    @Test
    void afterRenderReceivesRenderDuration() {
        List<Boolean> durations = new ArrayList<>();
        RuntimeActionRegistry actions = RuntimeActionRegistry.builder()
                .afterRender(result -> durations.add(result.renderDuration() != null))
                .build();

        LiveSession session = sessionWithActions(actions);
        session.renderPath("/");

        assertEquals(List.of(true), durations);
    }

    // --- Before/After Event ---

    @Test
    void beforeEventIsCalledDuringHandleEvent() {
        List<String> log = new ArrayList<>();
        RuntimeActionRegistry actions = RuntimeActionRegistry.builder()
                .beforeEvent(ctx -> log.add("beforeEvent:" + ctx.eventId()))
                .build();

        LiveSession session = sessionWithActions(actions);
        String document = session.renderDocument("/", ClientState.empty());
        String eventId = extractEventId(document);

        session.handleEvent(eventId, ClientState.empty());

        assertFalse(log.isEmpty());
        assertTrue(log.get(0).startsWith("beforeEvent:"));
    }

    @Test
    void afterEventReceivesResult() {
        List<String> log = new ArrayList<>();
        RuntimeActionRegistry actions = RuntimeActionRegistry.builder()
                .afterEvent(result -> log.add("afterEvent:" + result.eventId()))
                .build();

        LiveSession session = sessionWithActions(actions);
        String document = session.renderDocument("/", ClientState.empty());
        String eventId = extractEventId(document);

        session.handleEvent(eventId, ClientState.empty());

        assertFalse(log.isEmpty());
        assertTrue(log.get(0).startsWith("afterEvent:"));
    }

    @Test
    void beforeEventCanBlockDispatch() {
        List<String> errors = new ArrayList<>();
        List<String> after = new ArrayList<>();
        RuntimeActionRegistry actions = RuntimeActionRegistry.builder()
                .beforeEvent(ctx -> { throw new SecurityException("blocked"); })
                .afterEvent(result -> after.add(result.eventId()))
                .onError(ctx -> errors.add(ctx.phase() + ":" + ctx.eventId() + ":" + ctx.exception().getMessage()))
                .build();

        LiveSession session = sessionWithActions(actions);
        String document = session.renderDocument("/", ClientState.empty());
        String eventId = extractEventId(document);

        assertThrows(SecurityException.class, () -> session.handleEvent(eventId, ClientState.empty()));
        assertEquals(List.of("EVENT:" + eventId + ":blocked"), errors);
        assertTrue(after.isEmpty());
    }

    // --- Error handling ---

    @Test
    void onErrorIsCalledOnRenderFailure() {
        List<String> errors = new ArrayList<>();
        RuntimeActionRegistry actions = RuntimeActionRegistry.builder()
                .onError(ctx -> errors.add(ctx.phase() + ":" + ctx.exception().getMessage()))
                .build();

        LiveSession session = sessionWithActions(actions);

        assertThrows(IllegalArgumentException.class, () -> session.renderPath("/nonexistent"));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).startsWith("RENDER:"));
    }

    @Test
    void onErrorIsCalledOnEventFailure() {
        List<String> errors = new ArrayList<>();
        RuntimeActionRegistry actions = RuntimeActionRegistry.builder()
                .onError(ctx -> errors.add(ctx.phase().name()))
                .build();

        LiveSession session = sessionWithActions(actions);

        assertThrows(IllegalArgumentException.class,
                () -> session.handleEvent("nonexistent-event", ClientState.empty()));
        assertTrue(errors.stream().anyMatch(e -> e.contains("EVENT") || e.contains("RENDER")));
    }

    // --- Head contributions ---

    @Test
    void contributeHeadAppearsInRenderDocument() {
        RuntimeActionRegistry actions = RuntimeActionRegistry.builder()
                .contributeHead(ctx -> ctx.add(
                        meta().attr("name", "robots").attr("content", "index,follow")))
                .build();

        LiveSession session = sessionWithActions(actions);
        String document = session.renderDocument("/", ClientState.empty());

        assertTrue(document.contains("name=\"robots\""));
        assertTrue(document.contains("content=\"index,follow\""));
    }

    @Test
    void multipleHeadContributionsPreserveOrder() {
        RuntimeActionRegistry actions = RuntimeActionRegistry.builder()
                .contributeHead(ctx -> ctx.add(meta().attr("name", "first")))
                .contributeHead(ctx -> ctx.add(meta().attr("name", "second")))
                .build();

        LiveSession session = sessionWithActions(actions);
        String document = session.renderDocument("/", ClientState.empty());

        int firstIndex = document.indexOf("name=\"first\"");
        int secondIndex = document.indexOf("name=\"second\"");
        assertTrue(firstIndex < secondIndex, "Head contributions must preserve insertion order");
    }

    @Test
    void configHeadCollectionOverloadAddsHeadNodes() {
        LiveSessionConfig config = LiveSessionConfig.builder()
                .head(List.of(meta().attr("name", "collection-head")))
                .build();
        LiveSession session = new LiveSession(new Router().register(new SimplePage()), config);

        String document = session.renderDocument("/", ClientState.empty());

        assertTrue(document.contains("name=\"collection-head\""));
    }

    // --- Empty registry ---

    @Test
    void emptyRegistryDoesNotAffectExistingBehavior() {
        LiveSession session = new LiveSession(new Router().register(new SimplePage()));

        String document = session.renderDocument("/", ClientState.empty());
        assertTrue(document.contains("Hello"));
        assertTrue(document.contains("<!doctype html>"));
    }

    @Test
    void legacyConstructorsRemainUsable() {
        Router router = new Router().register(new SimplePage());

        assertTrue(new LiveSession(router, ujfe.html.CssTheme::defaultTheme)
                .renderPath("/")
                .html()
                .contains("Hello"));
        assertTrue(new LiveSession(router, new PageRenderer(), new LiveEventRegistry())
                .renderPath("/")
                .html()
                .contains("Hello"));
        assertTrue(new LiveSession(router, new PageRenderer(), new LiveEventRegistry(), ujfe.html.CssTheme::defaultTheme)
                .renderPath("/")
                .html()
                .contains("Hello"));
        assertTrue(new LiveSession(router, new PageRenderer(), new LiveEventRegistry(), ujfe.html.CssTheme::defaultTheme, true)
                .renderPath("/")
                .html()
                .contains("Hello"));
    }

    // --- Helpers ---

    private static LiveSession sessionWithActions(RuntimeActionRegistry actions) {
        LiveSessionConfig config = LiveSessionConfig.builder()
                .runtimeActions(actions)
                .build();
        return new LiveSession(new Router().register(new ClickablePage()), config);
    }

    private static String extractEventId(String html) {
        Matcher matcher = Pattern.compile("data-ujfe-event=\"([^\"]+)\"").matcher(html);
        assertTrue(matcher.find());
        return matcher.group(1);
    }

    @Page("/")
    public static final class SimplePage implements Component {
        @Override
        public Node render() {
            return div().child(p("Hello"));
        }
    }

    @Page("/")
    public static final class ClickablePage implements Component {
        @Override
        public Node render() {
            return div()
                    .child(p("Hello"))
                    .child(button("Click").onClick(() -> {}));
        }
    }
}
