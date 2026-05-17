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
import ujfe.router.Page;
import ujfe.router.Router;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
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

                assertEquals(HttpResponseStatus.FORBIDDEN, response.status());
                assertEquals("Missing CSRF token.", responseBody(response));
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

                assertEquals(HttpResponseStatus.FORBIDDEN, response.status());
                assertEquals("Invalid CSRF token.", responseBody(response));
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

                assertEquals(HttpResponseStatus.FORBIDDEN, response.status());
                assertEquals("Cross-origin request rejected.", responseBody(response));
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

                assertEquals(HttpResponseStatus.FORBIDDEN, response.status());
                assertEquals("Missing CSRF token.", responseBody(response));
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

                assertEquals(HttpResponseStatus.BAD_REQUEST, response.status());
                assertEquals("Invalid live JSON payload.", responseBody(response));
                assertFalse(responseBody(response).contains("Exception"));
                assertFalse(responseBody(response).contains("LiveHttpCodec"));
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

                assertEquals(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE, response.status());
                assertEquals("Live JSON payload exceeds maximum size.", responseBody(response));
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

                assertEquals(HttpResponseStatus.TOO_MANY_REQUESTS, rejected.status());
                assertEquals("Rate limit exceeded.", responseBody(rejected));
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

                assertEquals(HttpResponseStatus.TOO_MANY_REQUESTS, rejected.status());
                assertEquals("Rate limit exceeded.", responseBody(rejected));
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
        assertEquals("no-referrer", response.headers().get("Referrer-Policy"));
        assertEquals("geolocation=(), microphone=(), camera=()", response.headers().get("Permissions-Policy"));
        String csp = response.headers().get("Content-Security-Policy");
        assertTrue(csp.contains("default-src 'self'"));
        assertTrue(csp.contains("object-src 'none'"));
        assertTrue(csp.contains("frame-ancestors 'none'"));
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

    private static final class CodecLogSilencer implements AutoCloseable {
        private final Logger logger;
        private final boolean useParentHandlers;

        private CodecLogSilencer(Logger logger) {
            this.logger = logger;
            this.useParentHandlers = logger.getUseParentHandlers();
            this.logger.setUseParentHandlers(false);
        }

        static CodecLogSilencer attach() {
            return new CodecLogSilencer(Logger.getLogger(LiveHttpCodec.class.getName()));
        }

        @Override
        public void close() {
            logger.setUseParentHandlers(useParentHandlers);
        }
    }
}
