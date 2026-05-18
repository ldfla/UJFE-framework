package ujfe.live;

import org.junit.jupiter.api.Test;
import ujfe.runtime.action.RuntimePhase;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

final class ErrorResponseRendererTest {
    @Test
    void productionResponseHidesExceptionDetailsAndIncludesStableCode() {
        RuntimeException failure = new RuntimeException(
            "render-secret at /Users/leandrof/Dev/ujfe/FailingPage.java via ujfe.internal.Renderer"
        );
        ErrorResponseContext context = ErrorResponseContext.builder()
            .adapter("test")
            .method("GET")
            .path("/")
            .requestId("req-42")
            .phase(RuntimePhase.RENDER)
            .build();

        UjfeErrorResponse response;
        try (LogCapture logs = LogCapture.attach()) {
            response = ErrorResponseRenderer.create(false)
                .render(failure, context);

            assertSame(failure, logs.thrown());
            assertTrue(logs.message()
                .contains("code=UJFE_RENDER_ERROR"));
            assertTrue(logs.message()
                .contains("requestId=req-42"));
        }

        assertEquals(500, response.httpStatus());
        assertEquals(UjfeErrorCode.UJFE_RENDER_ERROR, response.code());
        assertJsonContains(response.body(), "\"code\":\"UJFE_RENDER_ERROR\"");
        assertJsonContains(response.body(), "\"message\":\"An error occurred while rendering the page.\"");
        assertJsonContains(response.body(), "\"requestId\":\"req-42\"");
        assertFalse(response.body()
            .contains("render-secret"));
        assertFalse(response.body()
            .contains("FailingPage.java"));
        assertFalse(response.body()
            .contains("ujfe.internal"));
        assertFalse(response.body()
            .contains("RuntimeException"));
    }

    @Test
    void developmentResponseIncludesControlledSanitizedDetailsOnlyWhenEnabled() {
        RuntimeException failure = new RuntimeException(
            "token=abc123 cookie=sessionid authorization=Bearer password=hunter2 expected detail"
        );
        ErrorResponseContext context = ErrorResponseContext.builder()
            .adapter("test")
            .method("POST")
            .path("/_ujfe/event")
            .phase(RuntimePhase.EVENT)
            .build();

        UjfeErrorResponse production;
        UjfeErrorResponse development;
        try (LogCapture ignored = LogCapture.attach()) {
            production = ErrorResponseRenderer.create(false)
                .render(failure, context);
            development = ErrorResponseRenderer.create(true)
                .render(failure, context);
        }

        assertFalse(production.body()
            .contains("details"));
        assertJsonContains(development.body(), "\"details\":\"Development diagnostics only:");
        assertJsonContains(development.body(), "expected detail");
        assertFalse(development.body()
            .contains("abc123"));
        assertFalse(development.body()
            .contains("sessionid"));
        assertFalse(development.body()
            .contains("hunter2"));
    }

    @Test
    void mapsSecurityAndValidationExceptionsToStableCodes() {
        ErrorResponseContext eventContext = ErrorResponseContext.builder()
            .adapter("test")
            .method("POST")
            .path("/_ujfe/event")
            .phase(RuntimePhase.EVENT)
            .build();

        UjfeErrorResponse csrf;
        UjfeErrorResponse payload;
        UjfeErrorResponse rateLimit;
        try (LogCapture ignored = LogCapture.attach()) {
            csrf = ErrorResponseRenderer.create(false)
                .render(
                    new LiveCsrfException(LiveHttpFailureCategory.MISSING_CSRF_TOKEN, "Missing CSRF token.", false),
                    eventContext
                );
            payload = ErrorResponseRenderer.create(false)
                .render(
                    new LiveHttpCodecException(
                        LiveHttpFailureCategory.INVALID_JSON,
                        "Invalid live JSON payload.",
                        400,
                        128,
                        12
                    ),
                    eventContext
                );
            rateLimit = ErrorResponseRenderer.create(false)
                .render(
                    new LiveRateLimitException("/_ujfe/event", RateLimitKeyType.SESSION, java.time.Duration.ofSeconds(2)),
                    eventContext
                );
        }

        assertEquals(403, csrf.httpStatus());
        assertEquals(UjfeErrorCode.UJFE_CSRF_VALIDATION_FAILED, csrf.code());
        assertEquals(400, payload.httpStatus());
        assertEquals(UjfeErrorCode.UJFE_BAD_REQUEST, payload.code());
        assertEquals(429, rateLimit.httpStatus());
        assertEquals(UjfeErrorCode.UJFE_RATE_LIMITED, rateLimit.code());
        assertEquals(2, rateLimit.retryAfterSeconds()
            .orElseThrow());
    }

    @Test
    void liveSessionConfigDisablesDevelopmentDetailsByDefault() {
        assertFalse(LiveSessionConfig.defaults()
            .isDevelopmentErrorDetailsEnabled());
        assertTrue(LiveSessionConfig.builder()
            .enableDevelopmentErrorDetailsUnsafe()
            .build()
            .isDevelopmentErrorDetailsEnabled());
    }

    private static void assertJsonContains(String json, String expected) {
        assertTrue(json.contains(expected), () -> "Expected JSON to contain " + expected + " but was " + json);
    }

    private static final class LogCapture implements AutoCloseable {
        private final Logger logger = Logger.getLogger(ErrorResponseRenderer.class.getName());
        private final boolean useParentHandlers = logger.getUseParentHandlers();
        private final CaptureHandler handler = new CaptureHandler();

        private LogCapture() {
            logger.setUseParentHandlers(false);
            logger.addHandler(handler);
        }

        static LogCapture attach() {
            return new LogCapture();
        }

        String message() {
            return handler.record == null ? "" : handler.record.getMessage();
        }

        Throwable thrown() {
            return handler.record == null ? null : handler.record.getThrown();
        }

        @Override
        public void close() {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(useParentHandlers);
        }

        private static final class CaptureHandler extends Handler {
            private LogRecord record;

            @Override
            public void publish(LogRecord record) {
                this.record = record;
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        }
    }
}
