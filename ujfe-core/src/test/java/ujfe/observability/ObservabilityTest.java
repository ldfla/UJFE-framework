package ujfe.observability;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class ObservabilityTest {
    private static final Instant STARTED_AT = Instant.parse("2026-05-19T00:00:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(STARTED_AT, ZoneOffset.UTC);

    @Test
    void renderTraceExposesSafeOperationalFields() {
        RenderTrace trace = RenderTrace.builder()
            .traceId("trace-1")
            .requestId("req-1")
            .route("/docs")
            .httpMethod("GET")
            .httpStatus(200)
            .adapterName("netty")
            .duration(Duration.ofMillis(12))
            .responseSizeBytes(1024)
            .status(TraceStatus.SUCCESS)
            .startedAt(STARTED_AT)
            .source("render")
            .build();

        assertEquals("trace-1", trace.traceId());
        assertEquals("req-1", trace.requestId());
        assertEquals("/docs", trace.route());
        assertEquals("GET", trace.httpMethod());
        assertEquals(200, trace.httpStatus());
        assertEquals("netty", trace.adapterName());
        assertEquals(Duration.ofMillis(12), trace.duration());
        assertEquals(1024, trace.responseSizeBytes());
        assertEquals(TraceStatus.SUCCESS, trace.status());
        assertEquals(STARTED_AT, trace.startedAt());
        assertEquals("render", trace.source());
        assertNull(trace.errorCode());
        assertNull(trace.errorType());
    }

    @Test
    void renderTraceNormalizesBlankFieldsAndNegativeSize() {
        RenderTrace trace = RenderTrace.builder()
            .traceId(" ")
            .requestId("")
            .route(null)
            .httpMethod(" ")
            .adapterName("")
            .errorCode(" ")
            .errorType("")
            .source(" ")
            .responseSizeBytes(-10)
            .build();

        assertNull(trace.traceId());
        assertNull(trace.requestId());
        assertNull(trace.route());
        assertNull(trace.httpMethod());
        assertNull(trace.adapterName());
        assertNull(trace.errorCode());
        assertNull(trace.errorType());
        assertNull(trace.source());
        assertEquals(0, trace.responseSizeBytes());
        assertEquals(Duration.ZERO, trace.duration());
        assertEquals(TraceStatus.SUCCESS, trace.status());
        assertEquals(Instant.EPOCH, trace.startedAt());
    }

    @Test
    void eventTraceExposesSafeOperationalFields() {
        EventTrace trace = EventTrace.builder()
            .traceId("trace-2")
            .requestId("req-2")
            .route("/forms")
            .eventId("evt-opaque")
            .httpMethod("POST")
            .httpStatus(200)
            .adapterName("servlet")
            .duration(Duration.ofMillis(8))
            .responseSizeBytes(512)
            .status(TraceStatus.SUCCESS)
            .startedAt(STARTED_AT)
            .handlerFound(true)
            .handlerCompleted(true)
            .build();

        assertEquals("trace-2", trace.traceId());
        assertEquals("req-2", trace.requestId());
        assertEquals("/forms", trace.route());
        assertEquals("evt-opaque", trace.eventId());
        assertEquals("POST", trace.httpMethod());
        assertEquals(200, trace.httpStatus());
        assertEquals("servlet", trace.adapterName());
        assertEquals(Duration.ofMillis(8), trace.duration());
        assertEquals(512, trace.responseSizeBytes());
        assertEquals(TraceStatus.SUCCESS, trace.status());
        assertEquals(STARTED_AT, trace.startedAt());
        assertTrue(trace.handlerFound());
        assertTrue(trace.handlerCompleted());
        assertNull(trace.errorCode());
        assertNull(trace.errorType());
    }

    @Test
    void eventTraceNormalizesBlankFieldsAndNegativeSize() {
        EventTrace trace = EventTrace.builder()
            .traceId(" ")
            .requestId("")
            .route(null)
            .eventId(" ")
            .httpMethod("")
            .adapterName(" ")
            .errorCode("")
            .errorType(" ")
            .responseSizeBytes(-1)
            .build();

        assertNull(trace.traceId());
        assertNull(trace.requestId());
        assertNull(trace.route());
        assertNull(trace.eventId());
        assertNull(trace.httpMethod());
        assertNull(trace.adapterName());
        assertNull(trace.errorCode());
        assertNull(trace.errorType());
        assertEquals(0, trace.responseSizeBytes());
        assertEquals(Duration.ZERO, trace.duration());
        assertEquals(TraceStatus.SUCCESS, trace.status());
        assertEquals(Instant.EPOCH, trace.startedAt());
        assertFalse(trace.handlerFound());
        assertFalse(trace.handlerCompleted());
    }

    @Test
    void observabilityConfigDefaultsToNoopSinkAndEnabledTraces() {
        ObservabilityConfig config = ObservabilityConfig.defaults();

        assertTrue(config.renderTracesEnabled());
        assertTrue(config.eventTracesEnabled());
        assertSame(NoopTraceSink.INSTANCE, config.traceSink());
        assertNotNull(config.clock());
        assertDoesNotThrow(() -> config.traceSink()
            .onRenderTrace(RenderTrace.builder().build()));
        assertDoesNotThrow(() -> config.traceSink()
            .onEventTrace(EventTrace.builder().build()));
    }

    @Test
    void observabilityConfigAcceptsCustomSinkClockAndFlags() {
        RecordingSink sink = new RecordingSink();

        ObservabilityConfig config = ObservabilityConfig.builder()
            .renderTracesEnabled(false)
            .eventTracesEnabled(false)
            .traceSink(sink)
            .clock(FIXED_CLOCK)
            .build();

        assertFalse(config.renderTracesEnabled());
        assertFalse(config.eventTracesEnabled());
        assertSame(FIXED_CLOCK, config.clock());

        config.traceSink().onRenderTrace(RenderTrace.builder().build());
        config.traceSink().onEventTrace(EventTrace.builder().build());

        assertEquals(1, sink.renderCount);
        assertEquals(1, sink.eventCount);
    }

    @Test
    void compositeTraceSinkDispatchesInRegistrationOrder() {
        List<String> calls = new ArrayList<>();
        TraceSink sink = CompositeTraceSink.of(List.of(
            new NamedSink("first", calls),
            new NamedSink("second", calls)
        ));

        sink.onRenderTrace(RenderTrace.builder().build());
        sink.onEventTrace(EventTrace.builder().build());

        assertEquals(List.of("first:render", "second:render", "first:event", "second:event"), calls);
    }

    @Test
    void traceStatusContainsStableOperationalStatuses() {
        assertEquals(TraceStatus.SUCCESS, TraceStatus.valueOf("SUCCESS"));
        assertEquals(TraceStatus.CLIENT_ERROR, TraceStatus.valueOf("CLIENT_ERROR"));
        assertEquals(TraceStatus.SERVER_ERROR, TraceStatus.valueOf("SERVER_ERROR"));
        assertEquals(TraceStatus.NOT_FOUND, TraceStatus.valueOf("NOT_FOUND"));
        assertEquals(TraceStatus.FORBIDDEN, TraceStatus.valueOf("FORBIDDEN"));
        assertEquals(TraceStatus.RATE_LIMITED, TraceStatus.valueOf("RATE_LIMITED"));
        assertEquals(TraceStatus.VALIDATION_FAILED, TraceStatus.valueOf("VALIDATION_FAILED"));
        assertEquals(TraceStatus.ERROR, TraceStatus.valueOf("ERROR"));
    }

    private static final class RecordingSink implements TraceSink {
        private int renderCount;
        private int eventCount;

        @Override
        public void onRenderTrace(RenderTrace trace) {
            renderCount++;
        }

        @Override
        public void onEventTrace(EventTrace trace) {
            eventCount++;
        }
    }

    private static final class NamedSink implements TraceSink {
        private final String name;
        private final List<String> calls;

        private NamedSink(String name, List<String> calls) {
            this.name = name;
            this.calls = calls;
        }

        @Override
        public void onRenderTrace(RenderTrace trace) {
            calls.add(name + ":render");
        }

        @Override
        public void onEventTrace(EventTrace trace) {
            calls.add(name + ":event");
        }
    }
}
