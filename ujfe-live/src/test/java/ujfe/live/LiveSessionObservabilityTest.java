package ujfe.live;

import org.junit.jupiter.api.Test;
import ujfe.core.ClientState;
import ujfe.core.Component;
import ujfe.core.Node;
import ujfe.observability.EventTrace;
import ujfe.observability.ObservabilityConfig;
import ujfe.observability.RenderTrace;
import ujfe.observability.TraceSink;
import ujfe.observability.TraceStatus;
import ujfe.router.Page;
import ujfe.router.Router;
import ujfe.runtime.action.RuntimeActionRegistry;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static ujfe.core.UI.*;

final class LiveSessionObservabilityTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-05-19T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void emitsRenderTraceForSuccessfulRender() {
        RecordingTraceSink sink = new RecordingTraceSink();
        LiveSessionConfig config = config(sink);

        try (LiveSession session = new LiveSession(new Router().register(new CounterPage()), config)) {
            session.renderDocument("/", ClientState.empty(), metadata(session, "GET", "req-render"));
        }

        assertEquals(1, sink.renderTraces.size());
        RenderTrace trace = sink.renderTraces.get(0);
        assertEquals("/", trace.route());
        assertEquals("GET", trace.httpMethod());
        assertEquals("netty", trace.adapterName());
        assertEquals("req-render", trace.requestId());
        assertEquals(200, trace.httpStatus());
        assertEquals(TraceStatus.SUCCESS, trace.status());
        assertTrue(trace.responseSizeBytes() > 0);
        assertEquals(FIXED_CLOCK.instant(), trace.startedAt());
        assertNull(trace.errorCode());
    }

    @Test
    void emitsEventTraceForSuccessfulEventWithoutPayloadOrBrowserState() {
        RecordingTraceSink sink = new RecordingTraceSink();
        LiveSessionConfig config = LiveSessionConfig.builder()
            .observability(ObservabilityConfig.builder()
                .clock(FIXED_CLOCK)
                .traceSink(sink)
                .build())
            .allowClientCookie("ujfe_demo")
            .build();

        try (LiveSession session = new LiveSession(new Router().register(new CounterPage()), config)) {
            String document = session.renderDocument("/", ClientState.empty(), metadata(session, "GET", "req-render"));
            String eventId = extractEventId(document);

            session.handleEvent(
                eventId,
                "secret-form-value",
                ClientState.of(Map.of("ujfe_demo", "secret-cookie"), Map.of("secret", "secret-storage")),
                metadata(session, "POST", "req-event")
            );
        }

        assertEquals(1, sink.eventTraces.size());
        EventTrace trace = sink.eventTraces.get(0);
        assertEquals("/", trace.route());
        assertEquals("POST", trace.httpMethod());
        assertEquals("netty", trace.adapterName());
        assertEquals("req-event", trace.requestId());
        assertEquals(200, trace.httpStatus());
        assertEquals(TraceStatus.SUCCESS, trace.status());
        assertTrue(trace.handlerFound());
        assertTrue(trace.handlerCompleted());
        assertTrue(trace.responseSizeBytes() > 0);
        assertNotEquals("secret-form-value", trace.eventId());
        assertNotEquals("secret-cookie", trace.requestId());
        assertNull(trace.errorCode());
    }

    @Test
    void emitsSafeRenderTraceForRouteNotFound() {
        RecordingTraceSink sink = new RecordingTraceSink();

        try (LiveSession session = new LiveSession(new Router().register(new CounterPage()), config(sink))) {
            assertThrows(IllegalArgumentException.class, () -> session.renderPath("/missing"));
        }

        assertEquals(1, sink.renderTraces.size());
        RenderTrace trace = sink.renderTraces.get(0);
        assertEquals("/missing", trace.route());
        assertEquals(404, trace.httpStatus());
        assertEquals(TraceStatus.NOT_FOUND, trace.status());
        assertEquals(UjfeErrorCode.UJFE_ROUTE_NOT_FOUND.name(), trace.errorCode());
        assertNotNull(trace.errorType());
    }

    @Test
    void emitsSafeRenderTraceForRenderErrorWithoutRawMessage() {
        RecordingTraceSink sink = new RecordingTraceSink();

        try (LiveSession session = new LiveSession(new Router().register(new FailingPage()), config(sink))) {
            assertThrows(IllegalStateException.class, () -> session.renderPath("/"));
        }

        assertEquals(1, sink.renderTraces.size());
        RenderTrace trace = sink.renderTraces.get(0);
        assertEquals(TraceStatus.SERVER_ERROR, trace.status());
        assertEquals(UjfeErrorCode.UJFE_RENDER_ERROR.name(), trace.errorCode());
        assertEquals(IllegalStateException.class.getName(), trace.errorType());
        assertFalse(trace.errorType().contains("secret"));
    }

    @Test
    void emitsSafeEventTraceForHandlerFailureWithoutRawMessage() {
        RecordingTraceSink sink = new RecordingTraceSink();

        try (LiveSession session = new LiveSession(new Router().register(new FailingEventPage()), config(sink))) {
            String document = session.renderDocument("/", ClientState.empty(), metadata(session, "GET", "req-render"));
            String eventId = extractEventId(document);

            assertThrows(IllegalStateException.class,
                () -> session.handleEvent(eventId, ClientState.empty(), metadata(session, "POST", "req-event")));
        }

        assertEquals(1, sink.eventTraces.size());
        EventTrace trace = sink.eventTraces.get(0);
        assertEquals(TraceStatus.SERVER_ERROR, trace.status());
        assertEquals(UjfeErrorCode.UJFE_EVENT_HANDLER_ERROR.name(), trace.errorCode());
        assertEquals(IllegalStateException.class.getName(), trace.errorType());
        assertFalse(trace.errorType().contains("secret"));
    }

    @Test
    void emitsTraceForMissingEventHandlerWithoutThrowing() {
        RecordingTraceSink sink = new RecordingTraceSink();

        try (LiveSession session = new LiveSession(new Router().register(new CounterPage()), config(sink))) {
            session.renderDocument("/", ClientState.empty(), metadata(session, "GET", "req-render"));
            session.handleEvent("evt-stale", ClientState.empty(), metadata(session, "POST", "req-event"));
        }

        assertEquals(1, sink.eventTraces.size());
        EventTrace trace = sink.eventTraces.get(0);
        assertEquals("evt-stale", trace.eventId());
        assertEquals(TraceStatus.NOT_FOUND, trace.status());
        assertFalse(trace.handlerFound());
        assertFalse(trace.handlerCompleted());
    }

    @Test
    void traceSinkFailureDoesNotBreakRenderOrEventHandling() {
        TraceSink failingSink = new TraceSink() {
            @Override
            public void onRenderTrace(RenderTrace trace) {
                throw new IllegalStateException("sink failed");
            }

            @Override
            public void onEventTrace(EventTrace trace) {
                throw new IllegalStateException("sink failed");
            }
        };

        PrintStream originalErr = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setErr(new PrintStream(captured));
        try (LiveSession session = new LiveSession(new Router().register(new CounterPage()), config(failingSink))) {
            String document = session.renderDocument("/", ClientState.empty(), metadata(session, "GET", "req-render"));
            String eventId = extractEventId(document);
            LiveRenderResult result = session.handleEvent(eventId, ClientState.empty(), metadata(session, "POST", "req-event"));

            assertTrue(result.html().contains("Count: 1"));
            assertTrue(captured.toString().contains("trace sink failed"));
        } finally {
            System.setErr(originalErr);
        }
    }

    @Test
    void runtimeActionsReceiveCompletedTraces() {
        List<RenderTrace> renderTraces = new ArrayList<>();
        List<EventTrace> eventTraces = new ArrayList<>();
        RuntimeActionRegistry actions = RuntimeActionRegistry.builder()
            .renderTrace(renderTraces::add)
            .eventTrace(eventTraces::add)
            .build();
        LiveSessionConfig config = LiveSessionConfig.builder()
            .runtimeActions(actions)
            .observability(ObservabilityConfig.builder()
                .clock(FIXED_CLOCK)
                .build())
            .build();

        try (LiveSession session = new LiveSession(new Router().register(new CounterPage()), config)) {
            String document = session.renderDocument("/", ClientState.empty(), metadata(session, "GET", "req-render"));
            session.handleEvent(extractEventId(document), ClientState.empty(), metadata(session, "POST", "req-event"));
        }

        assertFalse(renderTraces.isEmpty());
        assertEquals(1, eventTraces.size());
    }

    private static LiveSessionConfig config(TraceSink sink) {
        return LiveSessionConfig.builder()
            .observability(ObservabilityConfig.builder()
                .clock(FIXED_CLOCK)
                .traceSink(sink)
                .build())
            .build();
    }

    private static LiveHttpRequestMetadata metadata(LiveSession session, String method, String requestId) {
        return new LiveHttpRequestMetadata(
            session.csrfToken(),
            "http://localhost",
            null,
            "localhost",
            "http",
            "127.0.0.1",
            null,
            null,
            null,
            "netty",
            method,
            requestId
        );
    }

    private static String extractEventId(String html) {
        Matcher matcher = Pattern.compile("data-ujfe-event=\"([^\"]+)\"")
            .matcher(html);
        assertTrue(matcher.find());
        return matcher.group(1);
    }

    private static final class RecordingTraceSink implements TraceSink {
        private final List<RenderTrace> renderTraces = new ArrayList<>();
        private final List<EventTrace> eventTraces = new ArrayList<>();

        @Override
        public void onRenderTrace(RenderTrace trace) {
            renderTraces.add(trace);
        }

        @Override
        public void onEventTrace(EventTrace trace) {
            eventTraces.add(trace);
        }
    }

    @Page("/")
    public static final class CounterPage implements Component {
        private int count;

        @Override
        public Node render() {
            return div()
                .child(p(() -> "Count: " + count))
                .child(button("Increment").onClick(() -> count++));
        }
    }

    @Page("/")
    public static final class FailingPage implements Component {
        @Override
        public Node render() {
            throw new IllegalStateException("secret render message");
        }
    }

    @Page("/")
    public static final class FailingEventPage implements Component {
        @Override
        public Node render() {
            return button("Fail").onClick(() -> {
                throw new IllegalStateException("secret event message");
            });
        }
    }
}
