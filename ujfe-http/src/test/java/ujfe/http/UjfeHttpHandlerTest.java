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
import ujfe.router.Page;
import ujfe.router.Router;

import java.nio.charset.StandardCharsets;
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

            FullHttpResponse event = send(channel, request(HttpMethod.POST, LiveHttpPaths.EVENT,
                    "{\"eventId\":\"" + eventId + "\",\"clientState\":{\"cookies\":\"ujfe_demo=ativo\","
                            + "\"localStorage\":{\"theme\":\"dark\"}}}"));

            assertEquals(HttpResponseStatus.OK, event.status());
            assertTrue(event.headers().get(HttpHeaderNames.CONTENT_TYPE).startsWith("application/json"));
            assertTrue(responseBody(event).contains("\"html\""));
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
