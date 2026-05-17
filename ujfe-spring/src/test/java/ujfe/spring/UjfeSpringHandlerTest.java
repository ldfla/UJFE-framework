package ujfe.spring;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import ujfe.core.Node;
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

final class UjfeSpringHandlerTest {
    @Test
    void mapsOnlyUjfeRoutesAndInternalEndpoints() throws Exception {
        Router router = new Router().register(new HomePage());
        try (LiveSession session = new LiveSession(router)) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            UjfeSpringHandlerMapping mapping = new UjfeSpringHandlerMapping(router, handler);

            assertNotNull(mapping.getHandler(new MockHttpServletRequest("GET", "/")));
            assertNotNull(mapping.getHandler(new MockHttpServletRequest("GET", "/_ujfe/client.js")));
            assertNull(mapping.getHandler(new MockHttpServletRequest("GET", "/assets/app.css")));
            assertNull(mapping.getHandler(new MockHttpServletRequest("POST", "/")));
        }
    }

    @Test
    void rendersUjfePageThroughServletResponse() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
            request.addHeader("Cookie", "ujfe_demo=ativo");
            MockHttpServletResponse response = new MockHttpServletResponse();

            handler.handleRequest(request, response);

            assertEquals(200, response.getStatus());
            assertTrue(response.getContentType().startsWith("text/html"));
            assertTrue(response.getContentAsString().contains("Home"));
            assertTrue(response.getContentAsString().contains("Cookie: ativo"));
            assertTrue(response.getHeader("Content-Security-Policy").contains("default-src 'self'"));
        }
    }

    @Test
    void servesLiveClientScriptOnSameMvcPipeline() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletResponse response = new MockHttpServletResponse();

            handler.handleRequest(new MockHttpServletRequest("GET", "/_ujfe/client.js"), response);

            assertEquals(200, response.getStatus());
            assertTrue(response.getContentType().startsWith("application/javascript"));
            assertTrue(response.getContentAsString().contains("/_ujfe/event"));
        }
    }

    @Test
    void handlesLiveEventWithSharedCodecPayload() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletResponse page = new MockHttpServletResponse();
            handler.handleRequest(new MockHttpServletRequest("GET", "/"), page);
            String eventId = firstEventId(page.getContentAsString());

            MockHttpServletResponse event = new MockHttpServletResponse();
            handler.handleRequest(postJson(LiveHttpPaths.EVENT,
                    "{\"eventId\":\"" + eventId + "\",\"clientState\":{\"cookies\":\"ujfe_demo=ativo\","
                            + "\"localStorage\":{\"theme\":\"dark\"}}}"), event);

            assertEquals(200, event.getStatus());
            assertTrue(event.getContentType().startsWith("application/json"));
            assertTrue(event.getContentAsString().contains("\"html\""));
        }
    }

    @Test
    void rejectsMalformedJsonWithSafeCodecError() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletResponse response = new MockHttpServletResponse();

            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                handler.handleRequest(postJson(LiveHttpPaths.EVENT, "{\"eventId\":}"), response);
            }

            assertEquals(400, response.getStatus());
            assertEquals("Invalid live JSON payload.", response.getContentAsString());
            assertFalse(response.getContentAsString().contains("Exception"));
            assertFalse(response.getContentAsString().contains("LiveHttpCodec"));
        }
    }

    @Test
    void rejectsOversizedPayloadWithConfiguredLimit() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session, 32);
            MockHttpServletResponse response = new MockHttpServletResponse();

            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                handler.handleRequest(postJson(LiveHttpPaths.EVENT,
                        "{\"eventId\":\"evt-42\",\"clientState\":{\"localStorage\":{}}}"), response);
            }

            assertEquals(413, response.getStatus());
            assertEquals("Live JSON payload exceeds maximum size.", response.getContentAsString());
        }
    }

    private static MockHttpServletRequest postJson(String path, String body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        request.setContentType("application/json");
        return request;
    }

    private static String firstEventId(String html) {
        Matcher matcher = Pattern.compile("data-ujfe-event-click=\"([^\"]+)\"").matcher(html);
        assertTrue(matcher.find(), "Expected rendered page to contain a click event id");
        return matcher.group(1);
    }

    @Page("/")
    public static final class HomePage {
        public Node render() {
            return div()
                    .child(p("Home"))
                    .child(p(() -> "Cookie: " + ujfe.core.Ujfe.cookie("ujfe_demo").orElse("missing")))
                    .child(button("Click").onClick(() -> {
                    }));
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
            return new CodecLogSilencer(Logger.getLogger(ujfe.live.LiveHttpCodec.class.getName()));
        }

        @Override
        public void close() {
            logger.setUseParentHandlers(useParentHandlers);
        }
    }
}
