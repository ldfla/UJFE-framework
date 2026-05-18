package ujfe.spring;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import ujfe.core.Node;
import ujfe.live.LiveHttpPaths;
import ujfe.live.LiveSession;
import ujfe.live.LiveSessionConfig;
import ujfe.live.SecurityHeadersConfig;
import ujfe.router.Page;
import ujfe.router.Router;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
    void springPropertiesCanEnableDevelopmentErrorDetails() {
        UjfeSpringProperties properties = new UjfeSpringProperties();
        properties.setEnabled(true);

        LiveSessionConfig config = new UjfeSpringAutoConfiguration()
                .ujfeLiveSessionConfig(
                        properties,
                        new UjfeSpringSecurityHeadersProperties(),
                        new UjfeSpringClientStateProperties()
                );

        assertTrue(config.isDevelopmentErrorDetailsEnabled());
    }

    @Test
    void springPropertiesCanCustomizeAndDisableSecurityHeaders() {
        UjfeSpringSecurityHeadersProperties custom = new UjfeSpringSecurityHeadersProperties();
        custom.setEnabled(true);
        custom.setXContentTypeOptions("nosniff");
        custom.setXFrameOptions("SAMEORIGIN");
        custom.setReferrerPolicy("same-origin");
        custom.setContentSecurityPolicy("default-src 'self'");
        custom.setPermissionsPolicy("geolocation=()");

        LiveSessionConfig customConfig = new UjfeSpringAutoConfiguration()
                .ujfeLiveSessionConfig(
                        new UjfeSpringProperties(),
                        custom,
                        new UjfeSpringClientStateProperties()
                );

        assertTrue(custom.getEnabled());
        assertEquals("nosniff", custom.getXContentTypeOptions());
        assertEquals("SAMEORIGIN", custom.getXFrameOptions());
        assertEquals("same-origin", custom.getReferrerPolicy());
        assertEquals("default-src 'self'", custom.getContentSecurityPolicy());
        assertEquals("geolocation=()", custom.getPermissionsPolicy());
        assertEquals("same-origin", customConfig.securityHeaders().get(SecurityHeadersConfig.REFERRER_POLICY));
        assertEquals("default-src 'self'", customConfig.securityHeaders().get(SecurityHeadersConfig.CONTENT_SECURITY_POLICY));

        UjfeSpringSecurityHeadersProperties disabled = new UjfeSpringSecurityHeadersProperties();
        disabled.setEnabled(false);
        LiveSessionConfig disabledConfig = new UjfeSpringAutoConfiguration()
                .ujfeLiveSessionConfig(
                        new UjfeSpringProperties(),
                        disabled,
                        new UjfeSpringClientStateProperties()
                );

        assertFalse(disabledConfig.securityHeadersConfig().isEnabled());
        assertTrue(disabledConfig.securityHeaders().isEmpty());
    }

    @Test
    void springPropertiesCanConfigureClientStatePolicy() {
        UjfeSpringClientStateProperties clientState = new UjfeSpringClientStateProperties();
        clientState.setCookies(java.util.List.of("ujfe_demo"));
        clientState.setLocalStorageKeys(java.util.List.of("ujfe.theme"));
        clientState.setSessionStorageKeys(java.util.List.of("ujfe.tab"));

        LiveSessionConfig config = new UjfeSpringAutoConfiguration()
                .ujfeLiveSessionConfig(
                        new UjfeSpringProperties(),
                        new UjfeSpringSecurityHeadersProperties(),
                        clientState
                );

        assertEquals(java.util.Set.of("ujfe_demo"), config.clientStatePolicy().allowedCookies());
        assertEquals(java.util.Set.of("ujfe.theme"), config.clientStatePolicy().allowedLocalStorageKeys());
        assertEquals(java.util.Set.of("ujfe.tab"), config.clientStatePolicy().allowedSessionStorageKeys());
        assertEquals(java.util.List.of("ujfe_demo"), clientState.getCookies());
        assertEquals(java.util.List.of("ujfe.theme"), clientState.getLocalStorageKeys());
        assertEquals(java.util.List.of("ujfe.tab"), clientState.getSessionStorageKeys());
    }

    @Test
    void rendersUjfePageThroughServletResponse() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()), clientStateConfig())) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
            request.addHeader("Cookie", "ujfe_demo=ativo");
            MockHttpServletResponse response = new MockHttpServletResponse();

            handler.handleRequest(request, response);

            assertEquals(200, response.getStatus());
            assertTrue(response.getContentType().startsWith("text/html"));
            assertTrue(response.getContentAsString().contains("Home"));
            assertTrue(response.getContentAsString().contains("Cookie: ativo"));
            assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
            assertEquals("strict-origin-when-cross-origin", response.getHeader("Referrer-Policy"));
            assertTrue(response.getHeader("Content-Security-Policy").contains("default-src 'self'"));
        }
    }

    @Test
    void defaultClientStatePolicyDoesNotExposeCookiesFromPageRequests() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
            request.addHeader("Cookie", "ujfe_demo=ativo");
            MockHttpServletResponse response = new MockHttpServletResponse();

            handler.handleRequest(request, response);

            assertEquals(200, response.getStatus());
            assertTrue(response.getContentAsString().contains("Cookie: missing"));
        }
    }

    @Test
    void doesNotOverrideSecurityHeadersAlreadySetBySpringSecurity() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletResponse response = new MockHttpServletResponse();
            response.setHeader("Content-Security-Policy", "default-src 'none'");
            response.setHeader("X-Frame-Options", "SAMEORIGIN");

            handler.handleRequest(new MockHttpServletRequest("GET", "/"), response);

            assertEquals(200, response.getStatus());
            assertEquals("default-src 'none'", response.getHeader("Content-Security-Policy"));
            assertEquals("SAMEORIGIN", response.getHeader("X-Frame-Options"));
            assertEquals("nosniff", response.getHeader("X-Content-Type-Options"));
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
            String csrfToken = firstCsrfToken(page.getContentAsString());

            MockHttpServletResponse event = new MockHttpServletResponse();
            MockHttpServletRequest request = postJson(LiveHttpPaths.EVENT,
                    "{\"eventId\":\"" + eventId + "\",\"clientState\":{\"cookies\":\"ujfe_demo=ativo\","
                            + "\"localStorage\":{\"theme\":\"dark\"}}}");
            request.addHeader("X-UJFE-CSRF", csrfToken);
            request.addHeader("Origin", "http://localhost");
            request.addHeader("Host", "localhost");
            handler.handleRequest(request, event);

            assertEquals(200, event.getStatus());
            assertTrue(event.getContentType().startsWith("application/json"));
            assertTrue(event.getContentAsString().contains("\"html\""));
        }
    }

    @Test
    void rejectsLiveEventWithoutCsrfToken() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletResponse page = new MockHttpServletResponse();
            handler.handleRequest(new MockHttpServletRequest("GET", "/"), page);
            String eventId = firstEventId(page.getContentAsString());

            MockHttpServletRequest request = postJson(LiveHttpPaths.EVENT,
                    "{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}");
            request.addHeader("Origin", "http://localhost");
            request.addHeader("Host", "localhost");
            MockHttpServletResponse response = new MockHttpServletResponse();

            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                handler.handleRequest(request, response);
            }

            assertEquals(403, response.getStatus());
            assertError(response, 403, "UJFE_CSRF_VALIDATION_FAILED", "The request could not be verified.");
        }
    }

    @Test
    void rejectsLiveEventWithInvalidCsrfToken() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletResponse page = new MockHttpServletResponse();
            handler.handleRequest(new MockHttpServletRequest("GET", "/"), page);
            String eventId = firstEventId(page.getContentAsString());

            MockHttpServletRequest request = postJson(LiveHttpPaths.EVENT,
                    "{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}");
            request.addHeader("X-UJFE-CSRF", "invalid-token");
            request.addHeader("Origin", "http://localhost");
            request.addHeader("Host", "localhost");
            MockHttpServletResponse response = new MockHttpServletResponse();

            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                handler.handleRequest(request, response);
            }

            assertEquals(403, response.getStatus());
            assertError(response, 403, "UJFE_CSRF_VALIDATION_FAILED", "The request could not be verified.");
        }
    }

    @Test
    void rejectsCrossOriginLiveEvent() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletResponse page = new MockHttpServletResponse();
            handler.handleRequest(new MockHttpServletRequest("GET", "/"), page);
            String eventId = firstEventId(page.getContentAsString());
            String csrfToken = firstCsrfToken(page.getContentAsString());

            MockHttpServletRequest request = postJson(LiveHttpPaths.EVENT,
                    "{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}");
            request.addHeader("X-UJFE-CSRF", csrfToken);
            request.addHeader("Origin", "http://evil.test");
            request.addHeader("Host", "localhost");
            MockHttpServletResponse response = new MockHttpServletResponse();

            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                handler.handleRequest(request, response);
            }

            assertEquals(403, response.getStatus());
            assertError(response, 403, "UJFE_CSRF_VALIDATION_FAILED", "The request could not be verified.");
        }
    }

    @Test
    void developmentOverrideAllowsMissingCsrfToken() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()),
                ujfe.live.LiveSessionConfig.builder().disableCsrfProtectionForDevelopmentUnsafe().build())) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletResponse page = new MockHttpServletResponse();
            handler.handleRequest(new MockHttpServletRequest("GET", "/"), page);
            String eventId = firstEventId(page.getContentAsString());

            MockHttpServletResponse response = new MockHttpServletResponse();
            handler.handleRequest(postJson(LiveHttpPaths.EVENT,
                    "{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}"), response);

            assertEquals(200, response.getStatus());
            assertTrue(response.getContentAsString().contains("\"html\""));
            assertFalse(page.getContentAsString().contains("ujfe-csrf-token"));
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

            assertError(response, 400, "UJFE_BAD_REQUEST", "The request is invalid.");
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

            assertError(response, 413, "UJFE_BAD_REQUEST", "The request is invalid.");
        }
    }

    @Test
    void rateLimitsLiveEventEndpoint() throws Exception {
        LiveSessionConfig config = LiveSessionConfig.builder()
                .disableCsrfProtectionForDevelopmentUnsafe()
                .internalEndpointRateLimit(1, 1, Duration.ofMinutes(1))
                .build();
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()), config)) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletResponse page = new MockHttpServletResponse();
            handler.handleRequest(new MockHttpServletRequest("GET", "/"), page);
            String eventId = firstEventId(page.getContentAsString());

            MockHttpServletResponse first = new MockHttpServletResponse();
            handler.handleRequest(postJson(LiveHttpPaths.EVENT,
                    "{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}"), first);

            MockHttpServletResponse second = new MockHttpServletResponse();
            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                handler.handleRequest(postJson(LiveHttpPaths.EVENT,
                        "{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}"), second);
            }

            assertEquals(200, first.getStatus());
            assertError(second, 429, "UJFE_RATE_LIMITED", "Too many requests.");
            assertNotNull(second.getHeader("Retry-After"));
        }
    }

    @Test
    void rateLimitsStateEndpoint() throws Exception {
        LiveSessionConfig config = LiveSessionConfig.builder()
                .disableCsrfProtectionForDevelopmentUnsafe()
                .internalEndpointRateLimit(1, 1, Duration.ofMinutes(1))
                .build();
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()), config)) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            handler.handleRequest(new MockHttpServletRequest("GET", "/"), new MockHttpServletResponse());

            MockHttpServletResponse first = new MockHttpServletResponse();
            handler.handleRequest(postJson(LiveHttpPaths.STATE,
                    "{\"clientState\":{\"localStorage\":{}}}"), first);

            MockHttpServletResponse second = new MockHttpServletResponse();
            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                handler.handleRequest(postJson(LiveHttpPaths.STATE,
                        "{\"clientState\":{\"localStorage\":{}}}"), second);
            }

            assertEquals(200, first.getStatus());
            assertError(second, 429, "UJFE_RATE_LIMITED", "Too many requests.");
        }
    }

    @Test
    void renderFailureReturnsSafeJsonErrorResponse() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new FailingRenderPage()))) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
            request.addHeader("X-Request-Id", "req-render-1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                handler.handleRequest(request, response);
            }

            assertError(response, 500, "UJFE_RENDER_ERROR", "An error occurred while rendering the page.");
            assertTrue(response.getContentAsString().contains("\"requestId\":\"req-render-1\""));
            assertFalse(response.getContentAsString().contains("render-secret"));
            assertFalse(response.getContentAsString().contains("FailingRenderPage"));
        }
    }

    @Test
    void eventFailureReturnsSafeJsonErrorResponse() throws Exception {
        LiveSessionConfig config = LiveSessionConfig.builder()
                .disableCsrfProtectionForDevelopmentUnsafe()
                .build();
        try (LiveSession session = new LiveSession(new Router().register(new FailingEventPage()), config)) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletResponse page = new MockHttpServletResponse();
            handler.handleRequest(new MockHttpServletRequest("GET", "/"), page);
            String eventId = firstEventId(page.getContentAsString());
            MockHttpServletResponse response = new MockHttpServletResponse();

            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                handler.handleRequest(postJson(LiveHttpPaths.EVENT,
                        "{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}"), response);
            }

            assertError(response, 500, "UJFE_EVENT_HANDLER_ERROR", "An error occurred while handling the live event.");
            assertFalse(response.getContentAsString().contains("event-secret"));
            assertFalse(response.getContentAsString().contains("FailingEventPage"));
        }
    }

    @Test
    void missingRouteReturnsSafeJsonNotFoundResponse() throws Exception {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletResponse response = new MockHttpServletResponse();

            try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
                handler.handleRequest(new MockHttpServletRequest("GET", "/missing"), response);
            }

            assertError(response, 404, "UJFE_ROUTE_NOT_FOUND", "The requested route was not found.");
            assertFalse(response.getContentAsString().contains("No UJFE route registered"));
        }
    }

    @Test
    void publicPageRouteIsNotRateLimited() throws Exception {
        LiveSessionConfig config = LiveSessionConfig.builder()
                .internalEndpointRateLimit(1, 1, Duration.ofMinutes(1))
                .build();
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()), config)) {
            UjfeSpringHandler handler = new UjfeSpringHandler(session);
            MockHttpServletResponse first = new MockHttpServletResponse();
            MockHttpServletResponse second = new MockHttpServletResponse();

            handler.handleRequest(new MockHttpServletRequest("GET", "/"), first);
            handler.handleRequest(new MockHttpServletRequest("GET", "/"), second);

            assertEquals(200, first.getStatus());
            assertEquals(200, second.getStatus());
        }
    }

    private static MockHttpServletRequest postJson(String path, String body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        request.setContentType("application/json");
        return request;
    }

    private static LiveSessionConfig clientStateConfig() {
        return LiveSessionConfig.builder()
                .allowClientCookie("ujfe_demo")
                .allowLocalStorageKey("theme")
                .build();
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

    private static void assertError(MockHttpServletResponse response, int status, String code, String message)
            throws Exception {
        assertEquals(status, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        String body = response.getContentAsString();
        assertTrue(body.contains("\"code\":\"" + code + "\""), body);
        assertTrue(body.contains("\"message\":\"" + message + "\""), body);
        assertFalse(body.contains("Exception"), body);
        assertFalse(body.contains("LiveHttpCodec"), body);
        assertFalse(body.contains("/Users/"), body);
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
                    Logger.getLogger(ujfe.live.LiveHttpCodec.class.getName()),
                    Logger.getLogger(ujfe.live.ErrorResponseRenderer.class.getName())
            );
        }

        @Override
        public void close() {
            codecLogger.setUseParentHandlers(codecUseParentHandlers);
            errorLogger.setUseParentHandlers(errorUseParentHandlers);
        }
    }
}
