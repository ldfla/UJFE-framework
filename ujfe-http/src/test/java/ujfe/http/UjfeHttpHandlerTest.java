package ujfe.http;

import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.jupiter.api.Test;
import ujfe.core.Node;
import ujfe.live.LiveHttpCodec;
import ujfe.live.LiveHttpPaths;
import ujfe.live.LiveSession;
import ujfe.live.LiveSessionConfig;
import ujfe.live.SecurityHeadersConfig;
import ujfe.router.Page;
import ujfe.router.Router;
import ujfe.runtime.action.RuntimeActionRegistry;
import ujfe.runtime.action.RuntimeErrorContext;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static ujfe.html.UI.*;

final class UjfeHttpHandlerTest {
    static {
        System.setProperty("io.netty.noUnsafe", "true");
    }

    @Test
    void handlesLiveEventWithSharedCodecPayload() {
        Router router = new Router().register(new HomePage());
        try (LiveSession session = new LiveSession(router)) {
            EmbeddedChannel channel = new EmbeddedChannel(new UjfeHttpHandler(session));

            FullHttpResponse page = send(channel, request(HttpMethod.GET, "/", ""));
            String eventId = firstEventId(responseBody(page));
            String csrfToken = firstCsrfToken(responseBody(page));

            FullHttpRequest eventRequest = request(HttpMethod.POST, LiveHttpPaths.EVENT,
                    "{\"eventId\":\"" + eventId + "\",\"clientState\":{\"cookies\":\"ujfe_demo=ativo\","
                            + "\"localStorage\":{\"theme\":\"dark\"}}}");
            eventRequest.headers().set("X-UJFE-CSRF", csrfToken);
            eventRequest.headers().set("Origin", "http://localhost");
            eventRequest.headers().set("Host", "localhost");

            FullHttpResponse event = send(channel, eventRequest);

            assertEquals(HttpResponseStatus.OK, event.status());
            assertTrue(event.headers().get(HttpHeaderNames.CONTENT_TYPE).startsWith("application/json"));
            assertTrue(responseBody(event).contains("\"html\""));
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void rejectsLiveEventWithoutCsrfToken() {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            EmbeddedChannel channel = new EmbeddedChannel(new UjfeHttpHandler(session));
            FullHttpResponse page = send(channel, request(HttpMethod.GET, "/", ""));
            String eventId = firstEventId(responseBody(page));
            FullHttpRequest eventRequest = request(HttpMethod.POST, LiveHttpPaths.EVENT,
                    "{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}");
            eventRequest.headers().set("Origin", "http://localhost");
            eventRequest.headers().set("Host", "localhost");

            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                FullHttpResponse response = send(channel, eventRequest);

                assertError(response, HttpResponseStatus.FORBIDDEN,
                        "UJFE_CSRF_VALIDATION_FAILED", "The request could not be verified.");
            }
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void rejectsLiveEventWithInvalidCsrfToken() {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            EmbeddedChannel channel = new EmbeddedChannel(new UjfeHttpHandler(session));
            FullHttpResponse page = send(channel, request(HttpMethod.GET, "/", ""));
            String eventId = firstEventId(responseBody(page));
            FullHttpRequest eventRequest = request(HttpMethod.POST, LiveHttpPaths.EVENT,
                    "{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}");
            eventRequest.headers().set("X-UJFE-CSRF", "invalid-token");
            eventRequest.headers().set("Origin", "http://localhost");
            eventRequest.headers().set("Host", "localhost");

            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                FullHttpResponse response = send(channel, eventRequest);

                assertError(response, HttpResponseStatus.FORBIDDEN,
                        "UJFE_CSRF_VALIDATION_FAILED", "The request could not be verified.");
            }
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void rejectsCrossOriginLiveEvent() {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            EmbeddedChannel channel = new EmbeddedChannel(new UjfeHttpHandler(session));
            FullHttpResponse page = send(channel, request(HttpMethod.GET, "/", ""));
            String eventId = firstEventId(responseBody(page));
            String csrfToken = firstCsrfToken(responseBody(page));
            FullHttpRequest eventRequest = request(HttpMethod.POST, LiveHttpPaths.EVENT,
                    "{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}");
            eventRequest.headers().set("X-UJFE-CSRF", csrfToken);
            eventRequest.headers().set("Origin", "http://evil.test");
            eventRequest.headers().set("Host", "localhost");

            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                FullHttpResponse response = send(channel, eventRequest);

                assertError(response, HttpResponseStatus.FORBIDDEN,
                        "UJFE_CSRF_VALIDATION_FAILED", "The request could not be verified.");
            }
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void rejectsStateEndpointWithoutCsrfToken() {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            EmbeddedChannel channel = new EmbeddedChannel(new UjfeHttpHandler(session));
            FullHttpRequest stateRequest = request(HttpMethod.POST, LiveHttpPaths.STATE,
                    "{\"clientState\":{\"localStorage\":{}}}");
            stateRequest.headers().set("Origin", "http://localhost");
            stateRequest.headers().set("Host", "localhost");

            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                FullHttpResponse response = send(channel, stateRequest);

                assertError(response, HttpResponseStatus.FORBIDDEN,
                        "UJFE_CSRF_VALIDATION_FAILED", "The request could not be verified.");
            }
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void developmentOverrideAllowsMissingCsrfToken() {
        LiveSessionConfig config = LiveSessionConfig.builder().disableCsrfProtectionForDevelopmentUnsafe().build();
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()), config)) {
            EmbeddedChannel channel = new EmbeddedChannel(new UjfeHttpHandler(session));
            FullHttpResponse page = send(channel, request(HttpMethod.GET, "/", ""));
            String pageBody = responseBody(page);
            String eventId = firstEventId(pageBody);

            FullHttpResponse event = send(channel, request(HttpMethod.POST, LiveHttpPaths.EVENT,
                    "{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}"));

            assertEquals(HttpResponseStatus.OK, event.status());
            assertTrue(responseBody(event).contains("\"html\""));
            assertFalse(pageBody.contains("ujfe-csrf-token"));
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void rejectsMalformedLiveJsonWithSafeCodecError() {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            EmbeddedChannel channel = new EmbeddedChannel(new UjfeHttpHandler(session));
            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {

                FullHttpResponse response = send(channel, request(HttpMethod.POST, LiveHttpPaths.EVENT, "{\"eventId\":}"));

                assertError(response, HttpResponseStatus.BAD_REQUEST,
                        "UJFE_BAD_REQUEST", "The request is invalid.");
            }
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void rejectsOversizedLiveJsonWithConfiguredLimit() {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            EmbeddedChannel channel = new EmbeddedChannel(new UjfeHttpHandler(session, 32));
            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {

                FullHttpResponse response = send(channel, request(HttpMethod.POST, LiveHttpPaths.EVENT,
                        "{\"eventId\":\"evt-42\",\"clientState\":{\"localStorage\":{}}}"));

                assertError(response, HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE,
                        "UJFE_BAD_REQUEST", "The request is invalid.");
            }
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void rateLimitsLiveEventEndpoint() {
        LiveSessionConfig config = LiveSessionConfig.builder()
                .disableCsrfProtectionForDevelopmentUnsafe()
                .internalEndpointRateLimit(1, 1, Duration.ofMinutes(1))
                .build();
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()), config)) {
            EmbeddedChannel channel = new EmbeddedChannel(new UjfeHttpHandler(session));
            FullHttpResponse page = send(channel, request(HttpMethod.GET, "/", ""));
            String eventId = firstEventId(responseBody(page));
            FullHttpRequest firstRequest = request(HttpMethod.POST, LiveHttpPaths.EVENT,
                    "{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}");
            FullHttpRequest secondRequest = request(HttpMethod.POST, LiveHttpPaths.EVENT,
                    "{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}");

            assertEquals(HttpResponseStatus.OK, send(channel, firstRequest).status());
            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                FullHttpResponse rejected = send(channel, secondRequest);

                assertError(rejected, HttpResponseStatus.TOO_MANY_REQUESTS,
                        "UJFE_RATE_LIMITED", "Too many requests.");
                assertNotNull(rejected.headers().get("Retry-After"));
            }
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void rateLimitsStateEndpoint() {
        LiveSessionConfig config = LiveSessionConfig.builder()
                .disableCsrfProtectionForDevelopmentUnsafe()
                .internalEndpointRateLimit(1, 1, Duration.ofMinutes(1))
                .build();
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()), config)) {
            EmbeddedChannel channel = new EmbeddedChannel(new UjfeHttpHandler(session));
            send(channel, request(HttpMethod.GET, "/", ""));
            FullHttpRequest firstRequest = request(HttpMethod.POST, LiveHttpPaths.STATE,
                    "{\"clientState\":{\"localStorage\":{}}}");
            FullHttpRequest secondRequest = request(HttpMethod.POST, LiveHttpPaths.STATE,
                    "{\"clientState\":{\"localStorage\":{}}}");

            assertEquals(HttpResponseStatus.OK, send(channel, firstRequest).status());
            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                FullHttpResponse rejected = send(channel, secondRequest);

                assertError(rejected, HttpResponseStatus.TOO_MANY_REQUESTS,
                        "UJFE_RATE_LIMITED", "Too many requests.");
            }
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void renderFailureReturnsSafeJsonErrorResponse() {
        try (LiveSession session = new LiveSession(new Router().register(new FailingRenderPage()))) {
            EmbeddedChannel channel = new EmbeddedChannel(new UjfeHttpHandler(session));
            FullHttpRequest request = request(HttpMethod.GET, "/", "");
            request.headers().set("X-Request-Id", "req-render-1");

            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                FullHttpResponse response = send(channel, request);

                assertError(response, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                        "UJFE_RENDER_ERROR", "An error occurred while rendering the page.");
                assertTrue(responseBody(response).contains("\"requestId\":\"req-render-1\""));
                assertFalse(responseBody(response).contains("render-secret"));
                assertFalse(responseBody(response).contains("FailingRenderPage"));
            }
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void liveEventFailureReturnsSafeJsonErrorResponse() {
        LiveSessionConfig config = LiveSessionConfig.builder()
                .disableCsrfProtectionForDevelopmentUnsafe()
                .build();
        try (LiveSession session = new LiveSession(new Router().register(new FailingEventPage()), config)) {
            EmbeddedChannel channel = new EmbeddedChannel(new UjfeHttpHandler(session));
            FullHttpResponse page = send(channel, request(HttpMethod.GET, "/", ""));
            String eventId = firstEventId(responseBody(page));

            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                FullHttpResponse response = send(channel, request(HttpMethod.POST, LiveHttpPaths.EVENT,
                        "{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}"));

                assertError(response, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                        "UJFE_EVENT_HANDLER_ERROR", "An error occurred while handling the live event.");
                assertFalse(responseBody(response).contains("event-secret"));
                assertFalse(responseBody(response).contains("FailingEventPage"));
            }
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void missingRouteReturnsSafeJsonNotFoundResponse() {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            EmbeddedChannel channel = new EmbeddedChannel(new UjfeHttpHandler(session));

            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                FullHttpResponse response = send(channel, request(HttpMethod.GET, "/missing", ""));

                assertError(response, HttpResponseStatus.NOT_FOUND,
                        "UJFE_ROUTE_NOT_FOUND", "The requested route was not found.");
                assertFalse(responseBody(response).contains("No UJFE route registered"));
            }
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void runtimeOnErrorObservesSafeStructuredErrorMetadata() {
        AtomicReference<RuntimeErrorContext> observed = new AtomicReference<>();
        LiveSessionConfig config = LiveSessionConfig.builder()
                .runtimeActions(RuntimeActionRegistry.builder()
                        .onError(observed::set)
                        .build())
                .build();
        try (LiveSession session = new LiveSession(new Router().register(new FailingRenderPage()), config)) {
            EmbeddedChannel channel = new EmbeddedChannel(new UjfeHttpHandler(session));

            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                FullHttpResponse response = send(channel, request(HttpMethod.GET, "/", ""));

                assertError(response, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                        "UJFE_RENDER_ERROR", "An error occurred while rendering the page.");
                assertNotNull(observed.get());
                assertEquals("UJFE_RENDER_ERROR", observed.get().runtimeMetadata().get("errorCode"));
                assertEquals(500, observed.get().runtimeMetadata().get("httpStatus"));
                assertEquals("/", observed.get().path());
            }
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void onErrorHookFailureDoesNotLeakDetailsToClient() {
        LiveSessionConfig config = LiveSessionConfig.builder()
                .runtimeActions(RuntimeActionRegistry.builder()
                        .onError(context -> {
                            throw new IllegalStateException("on-error-secret");
                        })
                        .build())
                .build();
        try (LiveSession session = new LiveSession(new Router().register(new FailingRenderPage()), config)) {
            EmbeddedChannel channel = new EmbeddedChannel(new UjfeHttpHandler(session));

            try (CodecLogSilencer ignored = CodecLogSilencer.attach();
                 StderrCapture stderr = StderrCapture.attach()) {
                FullHttpResponse response = send(channel, request(HttpMethod.GET, "/", ""));

                assertError(response, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                        "UJFE_RENDER_ERROR", "An error occurred while rendering the page.");
                assertFalse(responseBody(response).contains("on-error-secret"));
                assertTrue(stderr.content().contains("on-error-secret"));
            }
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void publicPageRouteIsNotRateLimited() {
        LiveSessionConfig config = LiveSessionConfig.builder()
                .internalEndpointRateLimit(1, 1, Duration.ofMinutes(1))
                .build();
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()), config)) {
            EmbeddedChannel channel = new EmbeddedChannel(new UjfeHttpHandler(session));

            assertEquals(HttpResponseStatus.OK, send(channel, request(HttpMethod.GET, "/", "")).status());
            assertEquals(HttpResponseStatus.OK, send(channel, request(HttpMethod.GET, "/", "")).status());
            assertEquals(0, session.rateLimitMetrics().allowedRequests());
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void configuresDefaultAndCustomJsonPayloadLimits() {
        assertEquals(LiveHttpCodec.DEFAULT_MAX_JSON_PAYLOAD_BYTES,
                UjfeServerConfig.builder().build().maxJsonPayloadBytes());
        assertEquals(4096, UjfeServerConfig.builder()
                .maxJsonPayloadBytes(4096)
                .build()
                .maxJsonPayloadBytes());
        assertThrows(IllegalArgumentException.class, () -> UjfeServerConfig.builder().maxJsonPayloadBytes(0));
    }

    @Test
    void appliesSecurityHeaders() {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.EMPTY_BUFFER
        );

        UjfeHttpHandler.applySecurityHeaders(response);

        assertEquals("nosniff", response.headers().get("X-Content-Type-Options"));
        assertEquals("DENY", response.headers().get("X-Frame-Options"));
        assertEquals("strict-origin-when-cross-origin", response.headers().get("Referrer-Policy"));
        assertEquals("geolocation=(), microphone=(), camera=()", response.headers().get("Permissions-Policy"));
        String csp = response.headers().get("Content-Security-Policy");
        assertTrue(csp.contains("default-src 'self'"));
        assertTrue(csp.contains("script-src 'self'"));
        assertTrue(csp.contains("object-src 'none'"));
        assertTrue(csp.contains("frame-ancestors 'none'"));
    }

    @Test
    void appliesCustomAndDisabledSecurityHeaders() {
        FullHttpResponse custom = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.EMPTY_BUFFER
        );
        SecurityHeadersConfig customConfig = SecurityHeadersConfig.builder()
                .header(SecurityHeadersConfig.REFERRER_POLICY, "same-origin")
                .header(SecurityHeadersConfig.CONTENT_SECURITY_POLICY, "default-src 'self'")
                .build();

        UjfeHttpHandler.applySecurityHeaders(custom, customConfig);

        assertEquals("same-origin", custom.headers().get("Referrer-Policy"));
        assertEquals("default-src 'self'", custom.headers().get("Content-Security-Policy"));
        assertEquals("nosniff", custom.headers().get("X-Content-Type-Options"));

        FullHttpResponse disabled = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK,
                Unpooled.EMPTY_BUFFER
        );

        UjfeHttpHandler.applySecurityHeaders(disabled, SecurityHeadersConfig.disabled());

        assertNull(disabled.headers().get("X-Content-Type-Options"));
        assertNull(disabled.headers().get("Content-Security-Policy"));
    }

    private static FullHttpResponse send(EmbeddedChannel channel, FullHttpRequest request) {
        channel.writeInbound(request);
        FullHttpResponse response = channel.readOutbound();
        assertNotNull(response);
        return response;
    }

    private static FullHttpRequest request(HttpMethod method, String uri, String body) {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1,
                method,
                uri,
                Unpooled.wrappedBuffer(bytes)
        );
        request.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, bytes.length);
        return request;
    }

    private static String responseBody(FullHttpResponse response) {
        return response.content().toString(StandardCharsets.UTF_8);
    }

    private static void assertError(
            FullHttpResponse response,
            HttpResponseStatus status,
            String code,
            String message
    ) {
        assertEquals(status, response.status());
        assertTrue(response.headers().get(HttpHeaderNames.CONTENT_TYPE).startsWith("application/json"));
        String body = responseBody(response);
        assertTrue(body.contains("\"code\":\"" + code + "\""), body);
        assertTrue(body.contains("\"message\":\"" + message + "\""), body);
        assertFalse(body.contains("Exception"), body);
        assertFalse(body.contains("LiveHttpCodec"), body);
        assertFalse(body.contains("/Users/"), body);
    }

    private static String firstEventId(String html) {
        Matcher matcher = Pattern.compile("data-ujfe-event-click=\"([^\"]+)\"").matcher(html);
        assertTrue(matcher.find(), "Expected rendered page to contain a click event id");
        return matcher.group(1);
    }

    private static String firstCsrfToken(String html) {
        Matcher matcher = Pattern.compile("meta name=\"ujfe-csrf-token\" content=\"([^\"]+)\"").matcher(html);
        assertTrue(matcher.find(), "Expected rendered page to contain a csrf token");
        return matcher.group(1);
    }

    @Page("/")
    public static final class HomePage {
        private int clicks;

        public Node render() {
            return div()
                    .child(p("Home"))
                    .child(p(() -> "Clicks: " + clicks))
                    .child(button("Click").onClick(() -> clicks++));
        }
    }

    @Page("/")
    public static final class FailingRenderPage {
        public Node render() {
            throw new IllegalStateException("render-secret from /Users/leandrof/Dev/ujfe/FailingRenderPage.java");
        }
    }

    @Page("/")
    public static final class FailingEventPage {
        public Node render() {
            return button("Fail").onClick(() -> {
                throw new IllegalStateException("event-secret from FailingEventPage");
            });
        }
    }

    private static final class CodecLogSilencer implements AutoCloseable {
        private final Logger codecLogger;
        private final Logger errorLogger;
        private final boolean codecUseParentHandlers;
        private final boolean errorUseParentHandlers;

        private CodecLogSilencer(Logger codecLogger, Logger errorLogger) {
            this.codecLogger = codecLogger;
            this.errorLogger = errorLogger;
            this.codecUseParentHandlers = codecLogger.getUseParentHandlers();
            this.errorUseParentHandlers = errorLogger.getUseParentHandlers();
            this.codecLogger.setUseParentHandlers(false);
            this.errorLogger.setUseParentHandlers(false);
        }

        static CodecLogSilencer attach() {
            return new CodecLogSilencer(
                    Logger.getLogger(LiveHttpCodec.class.getName()),
                    Logger.getLogger(ujfe.live.ErrorResponseRenderer.class.getName())
            );
        }

        @Override
        public void close() {
            codecLogger.setUseParentHandlers(codecUseParentHandlers);
            errorLogger.setUseParentHandlers(errorUseParentHandlers);
        }
    }

    private static final class StderrCapture implements AutoCloseable {
        private final PrintStream original = System.err;
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();

        private StderrCapture() {
            System.setErr(new PrintStream(output));
        }

        static StderrCapture attach() {
            return new StderrCapture();
        }

        String content() {
            return output.toString(StandardCharsets.UTF_8);
        }

        @Override
        public void close() {
            System.setErr(original);
        }
    }
}
