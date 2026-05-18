package ujfe.live;

import org.junit.jupiter.api.Test;
import ujfe.core.ClientState;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

final class LiveHttpCodecTest {
    @Test
    void parsesValidEventPayload() {
        LiveHttpEventPayload payload = LiveHttpCodec.parseEventPayload("{"
                + "\"eventId\":\"evt-42\","
                + "\"value\":\"Said Adla\","
                + "\"clientState\":{"
                + "\"cookies\":\"ujfe_demo=ativo; theme=dark\","
                + "\"localStorage\":{\"ujfe.theme\":\"dark\",\"escaped\":\"A\\nB\"},"
                + "\"sessionStorage\":{\"ujfe.tab\":\"docs\"}"
                + "}"
                + "}");

        assertEquals("evt-42", payload.eventId());
        assertEquals("Said Adla", payload.value());
        assertEquals("ativo", payload.clientState().cookie("ujfe_demo").orElseThrow());
        assertEquals("dark", payload.clientState().localStorage("ujfe.theme").orElseThrow());
        assertEquals("A\nB", payload.clientState().localStorage("escaped").orElseThrow());
        assertEquals("docs", payload.clientState().sessionStorage("ujfe.tab").orElseThrow());
    }

    @Test
    void eventPayloadDefaultsMissingOrNullValueToEmptyString() {
        assertEquals("", LiveHttpCodec.parseEventPayload("{\"eventId\":\"evt-42\"}").value());
        assertEquals("", LiveHttpCodec.parseEventPayload("{\"eventId\":\"evt-42\",\"value\":null}").value());
    }

    @Test
    void parsesStatePayload() {
        ClientState state = LiveHttpCodec.parseStatePayload("{"
                + "\"clientState\":{"
                + "\"cookies\":\"ujfe_demo=novo\","
                + "\"localStorage\":{\"theme\":\"light\"},"
                + "\"sessionStorage\":{\"tab\":\"settings\"}"
                + "}"
                + "}");

        assertEquals("novo", state.cookie("ujfe_demo").orElseThrow());
        assertEquals("light", state.localStorage("theme").orElseThrow());
        assertEquals("settings", state.sessionStorage("tab").orElseThrow());
    }

    @Test
    void rejectsInvalidJsonWithSafeError() {
        LiveHttpCodecException failure = assertThrows(LiveHttpCodecException.class,
                () -> LiveHttpCodec.parseEventPayload("{\"eventId\":}"));

        assertEquals(LiveHttpFailureCategory.INVALID_JSON, failure.category());
        assertEquals(400, failure.httpStatus());
        assertEquals("Invalid live JSON payload.", failure.safeMessage());
        assertFalse(failure.safeMessage().contains("StringIndexOutOfBounds"));
        assertFalse(failure.safeMessage().contains("LiveHttpCodec"));
    }

    @Test
    void invalidJsonKeepsConfiguredPayloadLimitForLoggingMetadata() {
        LiveHttpCodecException failure = assertThrows(LiveHttpCodecException.class,
                () -> LiveHttpCodec.parseEventPayload("{\"eventId\":}", 32));

        assertEquals(LiveHttpFailureCategory.INVALID_JSON, failure.category());
        assertEquals(32, failure.payloadLimitBytes());
    }

    @Test
    void rejectsStructurallyInvalidJsonEvenWhenEventIdAppearsValid() {
        assertInvalidJson("{\"eventId\":\"evt-42\",}");
        assertInvalidJson("{\"eventId\":\"evt-42\"} {}");
        assertInvalidJson("{\"eventId\":\"evt-42\",\"clientState\":{\"localStorage\":{,}}}");
    }

    @Test
    void rejectsEmptyRequestBodyWithSafeError() {
        LiveHttpCodecException failure = assertThrows(LiveHttpCodecException.class,
                () -> LiveHttpCodec.parseEventPayload("  "));

        assertEquals(LiveHttpFailureCategory.EMPTY_PAYLOAD, failure.category());
        assertEquals(400, failure.httpStatus());
        assertEquals("Live JSON payload is empty.", failure.safeMessage());
    }

    @Test
    void rejectsEmptyJsonObjectWithSafeError() {
        LiveHttpCodecException failure = assertThrows(LiveHttpCodecException.class,
                () -> LiveHttpCodec.parseEventPayload("{ }"));

        assertEquals(LiveHttpFailureCategory.EMPTY_JSON, failure.category());
        assertEquals(400, failure.httpStatus());
        assertEquals("Live JSON payload must not be empty.", failure.safeMessage());
    }

    @Test
    void rejectsEventPayloadWithoutEventId() {
        LiveHttpCodecException failure = assertThrows(LiveHttpCodecException.class,
                () -> LiveHttpCodec.parseEventPayload("{\"clientState\":{\"localStorage\":{}}}"));

        assertEquals(LiveHttpFailureCategory.MISSING_EVENT_ID, failure.category());
        assertEquals(400, failure.httpStatus());
        assertEquals("Live event payload is missing eventId.", failure.safeMessage());
    }

    @Test
    void rejectsStatePayloadWithoutClientState() {
        LiveHttpCodecException failure = assertThrows(LiveHttpCodecException.class,
                () -> LiveHttpCodec.parseStatePayload("{\"eventId\":\"evt-42\"}"));

        assertEquals(LiveHttpFailureCategory.MISSING_CLIENT_STATE, failure.category());
        assertEquals(400, failure.httpStatus());
        assertEquals("Live state payload is missing clientState.", failure.safeMessage());
    }

    @Test
    void statePayloadRequiresRealClientStateObjectField() {
        LiveHttpCodecException failure = assertThrows(LiveHttpCodecException.class,
                () -> LiveHttpCodec.parseStatePayload("{\"note\":\"\\\"clientState\\\":{}\"}"));

        assertEquals(LiveHttpFailureCategory.MISSING_CLIENT_STATE, failure.category());
        assertEquals("Live state payload is missing clientState.", failure.safeMessage());
    }

    @Test
    void rejectsPayloadAboveDefaultLimit() {
        String payload = "{\"eventId\":\"" + "x".repeat(LiveHttpCodec.DEFAULT_MAX_JSON_PAYLOAD_BYTES) + "\"}";

        LiveHttpCodecException failure = assertThrows(LiveHttpCodecException.class,
                () -> LiveHttpCodec.parseEventPayload(payload));

        assertEquals(LiveHttpFailureCategory.PAYLOAD_TOO_LARGE, failure.category());
        assertEquals(413, failure.httpStatus());
        assertEquals(LiveHttpCodec.DEFAULT_MAX_JSON_PAYLOAD_BYTES, failure.payloadLimitBytes());
        assertTrue(failure.payloadSizeBytes() > LiveHttpCodec.DEFAULT_MAX_JSON_PAYLOAD_BYTES);
        assertEquals("Live JSON payload exceeds maximum size.", failure.safeMessage());
    }

    @Test
    void rejectsPayloadAboveConfiguredLimit() {
        String payload = "{\"eventId\":\"evt-42\",\"clientState\":{\"localStorage\":{}}}";

        LiveHttpCodecException failure = assertThrows(LiveHttpCodecException.class,
                () -> LiveHttpCodec.parseEventPayload(payload, 32));

        assertEquals(LiveHttpFailureCategory.PAYLOAD_TOO_LARGE, failure.category());
        assertEquals(413, failure.httpStatus());
        assertEquals(32, failure.payloadLimitBytes());
    }

    @Test
    void acceptsPayloadWithinConfiguredLimit() {
        String payload = "{\"eventId\":\"evt-42\",\"clientState\":{\"localStorage\":{}}}";

        LiveHttpEventPayload parsed = LiveHttpCodec.parseEventPayload(payload, 128);

        assertEquals("evt-42", parsed.eventId());
        assertTrue(parsed.clientState().cookies().isEmpty());
        assertTrue(parsed.clientState().localStorage().isEmpty());
    }

    @Test
    void readsPayloadFromReaderWithSizeLimit() throws IOException {
        String payload = "{\"eventId\":\"evt-42\"}";

        String read = LiveHttpCodec.readPayload(new StringReader(payload), 64);

        assertEquals(payload, read);
    }

    @Test
    void rejectsReaderPayloadAboveConfiguredLimitBeforeParsing() {
        String payload = "{\"eventId\":\"evt-42\"}";

        LiveHttpCodecException failure = assertThrows(LiveHttpCodecException.class,
                () -> LiveHttpCodec.readPayload(new StringReader(payload), 8));

        assertEquals(LiveHttpFailureCategory.PAYLOAD_TOO_LARGE, failure.category());
        assertEquals(8, failure.payloadLimitBytes());
    }

    @Test
    void serializesLivePayloadAsExpectedJsonShape() {
        String payload = LiveHttpCodec.livePayload(new LiveRenderResult("<div>\"A\"</div>", ".a{color:\"red\";}"));

        assertEquals("{\"html\":\"<div>\\\"A\\\"</div>\",\"css\":\".a{color:\\\"red\\\";}\"}", payload);
    }

    @Test
    void parsesCookieHeaderAndCssClasses() {
        assertEquals(Map.of("ujfe_demo", "ativo", "theme", "dark"),
                LiveHttpCodec.parseCookies("ujfe_demo=ativo; theme=dark"));
        assertEquals(Set.of("p-10", "gap-10", "bg-primary-200"),
                LiveHttpCodec.parseCssClasses("p-10   gap-10 bg-primary-200"));
    }

    @Test
    void keepsLegacyExtractionMethodsDelegatingToSharedCodec() {
        assertEquals("evt-42", LiveHttpCodec.extractEventId("{\"eventId\":\"evt-42\"}"));
        assertEquals("dark", LiveHttpCodec.extractClientState("{"
                + "\"clientState\":{\"localStorage\":{\"theme\":\"dark\"}}"
                + "}").localStorage("theme").orElseThrow());
    }

    @Test
    void logsRejectedPayloadWithSafeMetadataOnly() {
        LiveHttpCodecException failure = new LiveHttpCodecException(
                LiveHttpFailureCategory.PAYLOAD_TOO_LARGE,
                "Live JSON payload exceeds maximum size.",
                413,
                16,
                128
        );

        try (LogCapture logs = LogCapture.attach()) {
            LiveHttpCodec.logRejectedPayload(failure, "servlet adapter", "trace-42\nsecret");

            String message = logs.singleMessage();
            assertTrue(message.contains("event=ujfe.live_http_payload_rejected"));
            assertTrue(message.contains("category=payload_too_large"));
            assertTrue(message.contains("adapter=servlet_adapter"));
            assertTrue(message.contains("payloadLimitBytes=16"));
            assertTrue(message.contains("payloadSizeBytes=128"));
            assertTrue(message.contains("traceId=trace-42_secret"));
            assertFalse(message.contains("{\"eventId\""));
            assertFalse(message.contains("password"));
            assertFalse(message.contains("cookie="));
        }
    }

    private static final class LogCapture implements AutoCloseable {
        private final Logger logger;
        private final Handler handler;
        private final List<String> messages = new CopyOnWriteArrayList<>();
        private final boolean useParentHandlers;

        private LogCapture(Logger logger) {
            this.logger = logger;
            this.useParentHandlers = logger.getUseParentHandlers();
            this.handler = new Handler() {
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
            this.logger.setUseParentHandlers(false);
            this.logger.addHandler(handler);
        }

        static LogCapture attach() {
            return new LogCapture(Logger.getLogger(LiveHttpCodec.class.getName()));
        }

        String singleMessage() {
            assertEquals(1, messages.size());
            return messages.get(0);
        }

        @Override
        public void close() {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(useParentHandlers);
        }
    }

    private static void assertInvalidJson(String json) {
        LiveHttpCodecException failure = assertThrows(LiveHttpCodecException.class,
                () -> LiveHttpCodec.parseEventPayload(json));

        assertEquals(LiveHttpFailureCategory.INVALID_JSON, failure.category());
        assertEquals("Invalid live JSON payload.", failure.safeMessage());
    }
}
