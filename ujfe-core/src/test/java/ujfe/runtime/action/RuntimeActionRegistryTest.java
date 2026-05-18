package ujfe.runtime.action;

import org.junit.jupiter.api.Test;
import ujfe.core.ClientState;
import ujfe.core.Node;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

final class RuntimeActionRegistryTest {

    private static RenderContext renderContext() {
        return new RenderContext("/", ClientState.empty(), Instant.now(), "trace-1", Map.of());
    }

    private static RenderResult renderResult() {
        return new RenderResult("<p>OK</p>", "", "/", Duration.ZERO, "trace-1", Map.of());
    }

    private static LiveEventContext eventContext() {
        return new LiveEventContext("evt-1", ClientState.empty(), "trace-1", Map.of());
    }

    private static LiveEventResult eventResult() {
        return new LiveEventResult("<p>OK</p>", "evt-1", Duration.ZERO, "trace-1", Map.of());
    }

    private static Node node(String html) {
        return context -> html;
    }

    // --- Basic execution ---

    @Test
    void beforeRenderExecutes() {
        List<String> log = new ArrayList<>();
        RuntimeActionRegistry registry = RuntimeActionRegistry.builder()
            .beforeRender(ctx -> log.add("beforeRender:" + ctx.path()))
            .build();

        registry.executeBeforeRender(renderContext());
        assertEquals(List.of("beforeRender:/"), log);
    }

    @Test
    void afterRenderExecutes() {
        List<String> log = new ArrayList<>();
        RuntimeActionRegistry registry = RuntimeActionRegistry.builder()
            .afterRender(result -> log.add("afterRender:" + result.html()))
            .build();

        registry.executeAfterRender(renderResult());
        assertEquals(List.of("afterRender:<p>OK</p>"), log);
    }

    @Test
    void beforeEventExecutes() {
        List<String> log = new ArrayList<>();
        RuntimeActionRegistry registry = RuntimeActionRegistry.builder()
            .beforeEvent(ctx -> log.add("beforeEvent:" + ctx.eventId()))
            .build();

        registry.executeBeforeEvent(eventContext());
        assertEquals(List.of("beforeEvent:evt-1"), log);
    }

    @Test
    void afterEventExecutes() {
        List<String> log = new ArrayList<>();
        RuntimeActionRegistry registry = RuntimeActionRegistry.builder()
            .afterEvent(result -> log.add("afterEvent:" + result.eventId()))
            .build();

        registry.executeAfterEvent(eventResult());
        assertEquals(List.of("afterEvent:evt-1"), log);
    }

    @Test
    void onErrorExecutes() {
        List<String> log = new ArrayList<>();
        RuntimeActionRegistry registry = RuntimeActionRegistry.builder()
            .onError(ctx -> log.add("onError:" + ctx.exception()
                .getMessage()))
            .build();

        RuntimeErrorContext errorContext = new RuntimeErrorContext(
            new RuntimeException("boom"), RuntimePhase.RENDER,
            "/", null, "trace-1", null);
        registry.executeOnError(errorContext);

        assertEquals(List.of("onError:boom"), log);
    }

    @Test
    void contributeHeadInjectsMetaNode() {
        RuntimeActionRegistry registry = RuntimeActionRegistry.builder()
            .contributeHead(ctx -> ctx.add(
                node("<meta name=\"robots\" content=\"index,follow\">")))
            .build();

        var nodes = registry.executeHeadContributions();
        assertEquals(1, nodes.size());

        ujfe.core.UjfeContext ujfeCtx = ujfe.core.UjfeContext.create();
        String html = nodes.get(0)
            .render(ujfeCtx);
        assertTrue(html.contains("name=\"robots\""));
        assertTrue(html.contains("content=\"index,follow\""));
    }

    // --- Ordering ---

    @Test
    void multipleActionsExecuteInConfiguredOrder() {
        List<String> log = new ArrayList<>();
        RuntimeActionRegistry registry = RuntimeActionRegistry.builder()
            .beforeRender(ActionOrder.LATE, ctx -> log.add("C"))
            .beforeRender(ActionOrder.FIRST, ctx -> log.add("A"))
            .beforeRender(ActionOrder.NORMAL, ctx -> log.add("B"))
            .build();

        registry.executeBeforeRender(renderContext());
        assertEquals(List.of("A", "B", "C"), log);
    }

    @Test
    void equalOrderPreservesRegistrationOrder() {
        List<String> log = new ArrayList<>();
        RuntimeActionRegistry registry = RuntimeActionRegistry.builder()
            .beforeRender(ActionOrder.NORMAL, ctx -> log.add("first"))
            .beforeRender(ActionOrder.NORMAL, ctx -> log.add("second"))
            .beforeRender(ActionOrder.NORMAL, ctx -> log.add("third"))
            .build();

        registry.executeBeforeRender(renderContext());
        assertEquals(List.of("first", "second", "third"), log);
    }

    @Test
    void customOrderValuesWorkCorrectly() {
        List<String> log = new ArrayList<>();
        RuntimeActionRegistry registry = RuntimeActionRegistry.builder()
            .beforeRender(ActionOrder.of(100), ctx -> log.add("100"))
            .beforeRender(ActionOrder.of(50), ctx -> log.add("50"))
            .beforeRender(ActionOrder.of(200), ctx -> log.add("200"))
            .build();

        registry.executeBeforeRender(renderContext());
        assertEquals(List.of("50", "100", "200"), log);
    }

    // --- Empty registry ---

    @Test
    void emptyRegistryBehavesCorrectly() {
        RuntimeActionRegistry registry = RuntimeActionRegistry.empty();

        assertTrue(registry.isEmpty());
        assertDoesNotThrow(() -> registry.executeBeforeRender(renderContext()));
        assertDoesNotThrow(() -> registry.executeAfterRender(renderResult()));
        assertDoesNotThrow(() -> registry.executeBeforeEvent(eventContext()));
        assertDoesNotThrow(() -> registry.executeAfterEvent(eventResult()));
        assertDoesNotThrow(() -> registry.executeOnError(new RuntimeErrorContext(
            new RuntimeException(), RuntimePhase.INTERNAL, null, null, "t", null)));
        assertTrue(registry.executeHeadContributions()
            .isEmpty());
    }

    @Test
    void registryWithActionsIsNotEmpty() {
        RuntimeActionRegistry registry = RuntimeActionRegistry.builder()
            .beforeRender(ctx -> {
            })
            .build();

        assertFalse(registry.isEmpty());
    }

    // --- Error routing ---

    @Test
    void exceptionInActionRoutesToOnError() {
        List<String> errors = new ArrayList<>();
        RuntimeActionRegistry registry = RuntimeActionRegistry.builder()
            .beforeRender(ctx -> {
                throw new RuntimeException("action-failed");
            })
            .onError(ctx -> errors.add(ctx.exception()
                .getMessage()))
            .build();

        assertThrows(RuntimeException.class, () -> registry.executeBeforeRender(renderContext()));
        assertEquals(List.of("action-failed"), errors);
    }

    @Test
    void exceptionInOnErrorDoesNotCauseRecursion() {
        PrintStream originalErr = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setErr(new PrintStream(captured));
        try {
            RuntimeActionRegistry registry = RuntimeActionRegistry.builder()
                .onError(ctx -> {
                    throw new RuntimeException("onError-failed");
                })
                .build();

            RuntimeErrorContext errorContext = new RuntimeErrorContext(
                new RuntimeException("original"), RuntimePhase.RENDER,
                "/", null, "trace-1", null);

            assertDoesNotThrow(() -> registry.executeOnError(errorContext));
            assertTrue(captured.toString()
                .contains("onError-failed"));
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    void exceptionInAfterRenderRoutesToOnError() {
        List<String> errors = new ArrayList<>();
        RuntimeActionRegistry registry = RuntimeActionRegistry.builder()
            .afterRender(result -> {
                throw new RuntimeException("after-boom");
            })
            .onError(ctx -> errors.add(ctx.phase() + ":" + ctx.exception()
                .getMessage()))
            .build();

        assertThrows(RuntimeException.class, () -> registry.executeAfterRender(renderResult()));
        assertEquals(List.of("RENDER:after-boom"), errors);
    }

    @Test
    void exceptionInBeforeEventRoutesToOnError() {
        List<String> errors = new ArrayList<>();
        RuntimeActionRegistry registry = RuntimeActionRegistry.builder()
            .beforeEvent(ctx -> {
                throw new RuntimeException("before-evt-boom");
            })
            .onError(ctx -> errors.add(ctx.phase() + ":" + ctx.eventId()))
            .build();

        assertThrows(RuntimeException.class, () -> registry.executeBeforeEvent(eventContext()));
        assertEquals(List.of("EVENT:evt-1"), errors);
    }

    @Test
    void exceptionInHeadContributionRoutesToOnError() {
        List<String> errors = new ArrayList<>();
        RuntimeActionRegistry registry = RuntimeActionRegistry.builder()
            .contributeHead(ctx -> {
                throw new RuntimeException("head-boom");
            })
            .onError(ctx -> errors.add(ctx.phase()
                .name()))
            .build();

        assertThrows(RuntimeException.class, registry::executeHeadContributions);
        assertEquals(List.of("HEAD_CONTRIBUTION"), errors);
    }

    // --- Thread safety ---

    @Test
    void concurrentExecutionRemainsDeterministic() throws InterruptedException {
        CopyOnWriteArrayList<String> log = new CopyOnWriteArrayList<>();
        RuntimeActionRegistry registry = RuntimeActionRegistry.builder()
            .beforeRender(ctx -> log.add(Thread.currentThread()
                .getName()))
            .build();

        int threadCount = 8;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    registry.executeBeforeRender(renderContext());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        assertEquals(threadCount, log.size());
    }

    // --- ActionOrder ---

    @Test
    void actionOrderConstants() {
        assertTrue(ActionOrder.FIRST.priority() < ActionOrder.EARLY.priority());
        assertTrue(ActionOrder.EARLY.priority() < ActionOrder.NORMAL.priority());
        assertTrue(ActionOrder.NORMAL.priority() < ActionOrder.LATE.priority());
        assertTrue(ActionOrder.LATE.priority() < ActionOrder.LAST.priority());
    }

    @Test
    void actionOrderEquality() {
        assertEquals(ActionOrder.NORMAL, ActionOrder.of(500));
        assertEquals(ActionOrder.FIRST, ActionOrder.of(0));
    }

    // --- Context objects ---

    @Test
    void renderContextExposesFields() {
        RenderContext ctx = renderContext();
        assertEquals("/", ctx.path());
        assertEquals("trace-1", ctx.traceId());
        assertTrue(ctx.renderTimestamp()
            .isBefore(Instant.now()
                .plusSeconds(1)));
        assertTrue(ctx.metadata()
            .isEmpty());
    }

    @Test
    void renderContextExposesRuntimeExtensionFields() {
        Object page = new Object();
        Object session = new Object();
        RenderContext ctx = new RenderContext(
            "/dashboard", page, session,
            Map.of("method", "GET"),
            ClientState.empty(),
            Instant.EPOCH,
            "trace-2",
            Map.of("runtime", "pure"));

        assertEquals(page, ctx.page());
        assertEquals(session, ctx.session());
        assertEquals(Instant.EPOCH, ctx.renderTimestamp());
        assertEquals("GET", ctx.requestMetadata()
            .get("method"));
        assertEquals("pure", ctx.runtimeMetadata()
            .get("runtime"));
    }

    @Test
    void contextMetadataUsesDefensiveCopy() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("phase", "initial");

        RenderContext ctx = new RenderContext("/", ClientState.empty(), Instant.now(), "trace-1", metadata);
        metadata.put("phase", "changed");

        assertEquals("initial", ctx.metadata()
            .get("phase"));
    }

    @Test
    void renderResultExposesHeadAndMetadata() {
        Node head = node("<meta name=\"robots\">");
        RenderResult result = new RenderResult(
            "<p>OK</p>", ".x{}", "/", Duration.ofMillis(4), "trace-3",
            List.of(head), Map.of("route", "home"), Map.of("mode", "internal"), Map.of("runtime", "live"));

        assertEquals(List.of(head), result.headNodes());
        assertEquals("home", result.routeMetadata()
            .get("route"));
        assertEquals("internal", result.cssMetadata()
            .get("mode"));
        assertEquals("live", result.runtimeMetadata()
            .get("runtime"));
        assertEquals("live", result.metadata()
            .get("runtime"));
    }

    @Test
    void liveEventContextExposesEventFields() {
        Object session = new Object();
        Object target = new Object();
        LiveEventContext ctx = new LiveEventContext(
            "evt-9", "click", session, Map.of("method", "POST"),
            ClientState.empty(), target, Map.of("name", "Ana"),
            "trace-9", Map.of("runtime", "netty"));

        assertEquals("click", ctx.eventType());
        assertEquals(session, ctx.session());
        assertEquals(target, ctx.target());
        assertEquals("POST", ctx.requestMetadata()
            .get("method"));
        assertEquals("Ana", ctx.submittedValues()
            .get("name"));
        assertEquals("netty", ctx.runtimeMetadata()
            .get("runtime"));
        assertEquals("netty", ctx.metadata()
            .get("runtime"));
    }

    @Test
    void liveEventResultExposesMetadataGroups() {
        LiveEventResult result = new LiveEventResult(
            "<p>OK</p>", "evt-10", Duration.ofMillis(3), "trace-10",
            Map.of("type", "click"), Map.of("path", "/"), Map.of("cookies", 1), Map.of("runtime", "live"));

        assertEquals("click", result.eventMetadata()
            .get("type"));
        assertEquals(Duration.ofMillis(3), result.eventDuration());
        assertEquals("/", result.reRenderMetadata()
            .get("path"));
        assertEquals(1, result.clientStateMetadata()
            .get("cookies"));
        assertEquals("live", result.runtimeMetadata()
            .get("runtime"));
        assertEquals("live", result.metadata()
            .get("runtime"));
    }

    @Test
    void runtimeErrorContextExposesPhase() {
        RuntimeErrorContext ctx = new RuntimeErrorContext(
            new RuntimeException("test"), RuntimePhase.EVENT,
            "/page", "evt-42", "trace-1", Map.of("key", "value"));

        assertEquals(RuntimePhase.EVENT, ctx.phase());
        assertEquals("/page", ctx.path());
        assertEquals("evt-42", ctx.eventId());
        assertEquals("trace-1", ctx.traceId());
        assertEquals("value", ctx.metadata()
            .get("key"));
    }

    @Test
    void runtimeErrorContextExposesMetadataGroups() {
        RuntimeErrorContext ctx = new RuntimeErrorContext(
            new RuntimeException("test"), RuntimePhase.EVENT,
            "/page", "evt-42", "trace-1",
            Map.of("route", "home"),
            Map.of("event", "click"),
            Map.of("method", "POST"),
            Map.of("session", "s1"),
            Map.of("runtime", "live"));

        assertEquals("home", ctx.routeMetadata()
            .get("route"));
        assertEquals("click", ctx.eventMetadata()
            .get("event"));
        assertEquals("POST", ctx.requestMetadata()
            .get("method"));
        assertEquals("s1", ctx.sessionMetadata()
            .get("session"));
        assertEquals("live", ctx.runtimeMetadata()
            .get("runtime"));
    }

    @Test
    void topLevelRegistryBuilderIsAvailable() {
        RuntimeActionRegistryBuilder builder = RuntimeActionRegistry.builder();
        assertTrue(builder.build()
            .isEmpty());
    }

    @Test
    void headContributionContextPreservesInsertionOrder() {
        HeadContributionContext ctx = new HeadContributionContext();
        Node first = node("first");
        Node second = node("second");
        Node third = node("third");

        ctx.add(first);
        ctx.add(second);
        ctx.add(third);

        assertEquals(List.of(first, second, third), ctx.nodes());
    }
}
