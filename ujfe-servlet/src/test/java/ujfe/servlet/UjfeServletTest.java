package ujfe.servlet;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import ujfe.core.Node;
import ujfe.live.LiveHttpCodec;
import ujfe.live.LiveHttpPaths;
import ujfe.live.LiveSession;
import ujfe.live.LiveSessionConfig;
import ujfe.live.SecurityHeadersConfig;
import ujfe.router.Page;
import ujfe.router.Router;

import java.io.*;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static ujfe.html.UI.*;

final class UjfeServletTest {
    @Test
    void rendersUjfePageThroughJakartaServletResponse() throws Exception {
        UjfeServlet servlet = new UjfeServlet(new Router().register(new HomePage()), clientStateConfig());
        TestResponse response = service(servlet, TestRequest.get("/app/", "/app"));

        assertEquals(200, response.status());
        assertTrue(response.contentType().startsWith("text/html"));
        assertEquals("UTF-8", response.characterEncoding());
        assertTrue(response.body().contains("Home"));
        assertTrue(response.body().contains("Cookie: ativo"));
        assertEquals("nosniff", response.header("X-Content-Type-Options"));
        assertEquals("strict-origin-when-cross-origin", response.header("Referrer-Policy"));
        assertTrue(response.header("Content-Security-Policy").contains("default-src 'self'"));
    }

    @Test
    void servesClientDevAndCssInternalEndpoints() throws Exception {
        UjfeServlet servlet = new UjfeServlet(new Router().register(new HomePage()));

        assertTrue(LiveHttpPaths.internalPaths().contains(LiveHttpPaths.CLIENT_SCRIPT));

        TestResponse client = service(servlet, TestRequest.get(LiveHttpPaths.CLIENT_SCRIPT));
        TestResponse dev = service(servlet, TestRequest.get(LiveHttpPaths.DEV_SCRIPT));
        TestResponse css = service(servlet, TestRequest.get(LiveHttpPaths.CSS)
                .parameter("classes", "p-4 text-slate-900"));

        assertEquals(200, client.status());
        assertTrue(client.contentType().startsWith("application/javascript"));
        assertTrue(client.body().contains(LiveHttpPaths.EVENT));
        assertEquals(200, dev.status());
        assertTrue(dev.body().contains("ujfe-dev-preview"));
        assertEquals(200, css.status());
        assertTrue(css.contentType().startsWith("text/css"));
        assertTrue(css.body().contains("padding"));
    }

    @Test
    void eventAndStateEndpointsReturnLiveJsonPayloads() throws Exception {
        UjfeServlet servlet = new UjfeServlet(new Router().register(new HomePage()), clientStateConfig());
        TestResponse page = service(servlet, TestRequest.get("/"));
        String eventId = firstEventId(page.body());
        String csrfToken = firstCsrfToken(page.body());

        TestResponse event = service(servlet, TestRequest.post(LiveHttpPaths.EVENT)
                .header("X-UJFE-CSRF", csrfToken)
                .header("Origin", "http://localhost")
                .header("Host", "localhost")
                .body("{\"eventId\":\"" + eventId + "\",\"clientState\":{\"cookies\":\"ujfe_demo=ativo\","
                        + "\"localStorage\":{\"theme\":\"dark\"}}}"));
        TestResponse state = service(servlet, TestRequest.post(LiveHttpPaths.STATE)
                .header("X-UJFE-CSRF", csrfToken)
                .header("Origin", "http://localhost")
                .header("Host", "localhost")
                .body("{\"clientState\":{\"cookies\":\"ujfe_demo=novo\",\"localStorage\":{}}}"));

        assertEquals(200, event.status());
        assertTrue(event.contentType().startsWith("application/json"));
        assertTrue(event.body().contains("\"html\""));
        assertEquals(200, state.status());
        assertTrue(state.body().contains("Cookie: novo"));
    }

    @Test
    void defaultClientStatePolicyDoesNotExposeCookiesFromPageRequests() throws Exception {
        UjfeServlet servlet = new UjfeServlet(new Router().register(new StrictCookiePage()));

        TestResponse response = service(servlet, TestRequest.get("/"));

        assertEquals(200, response.status());
        assertTrue(response.body().contains("Cookie: missing"));
    }

    @Test
    void rejectsEventWithoutCsrfToken() throws Exception {
        UjfeServlet servlet = new UjfeServlet(new Router().register(new HomePage()));
        TestResponse page = service(servlet, TestRequest.get("/"));
        String eventId = firstEventId(page.body());

        TestResponse response;
        try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
            response = service(servlet, TestRequest.post(LiveHttpPaths.EVENT)
                    .header("Origin", "http://localhost")
                    .header("Host", "localhost")
                    .body("{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}"));
        }

        assertEquals(403, response.status());
        assertError(response, 403, "UJFE_CSRF_VALIDATION_FAILED", "The request could not be verified.");
    }

    @Test
    void rejectsEventWithInvalidCsrfToken() throws Exception {
        UjfeServlet servlet = new UjfeServlet(new Router().register(new HomePage()));
        TestResponse page = service(servlet, TestRequest.get("/"));
        String eventId = firstEventId(page.body());

        TestResponse response;
        try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
            response = service(servlet, TestRequest.post(LiveHttpPaths.EVENT)
                    .header("X-UJFE-CSRF", "invalid-token")
                    .header("Origin", "http://localhost")
                    .header("Host", "localhost")
                    .body("{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}"));
        }

        assertEquals(403, response.status());
        assertError(response, 403, "UJFE_CSRF_VALIDATION_FAILED", "The request could not be verified.");
    }

    @Test
    void rejectsCrossOriginLivePost() throws Exception {
        UjfeServlet servlet = new UjfeServlet(new Router().register(new HomePage()));
        TestResponse page = service(servlet, TestRequest.get("/"));
        String eventId = firstEventId(page.body());
        String csrfToken = firstCsrfToken(page.body());

        TestResponse response;
        try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
            response = service(servlet, TestRequest.post(LiveHttpPaths.EVENT)
                    .header("X-UJFE-CSRF", csrfToken)
                    .header("Origin", "http://evil.test")
                    .header("Host", "localhost")
                    .body("{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}"));
        }

        assertEquals(403, response.status());
        assertError(response, 403, "UJFE_CSRF_VALIDATION_FAILED", "The request could not be verified.");
    }

    @Test
    void rejectsStateEndpointWithoutCsrfToken() throws Exception {
        UjfeServlet servlet = new UjfeServlet(new Router().register(new HomePage()));

        TestResponse response;
        try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
            response = service(servlet, TestRequest.post(LiveHttpPaths.STATE)
                    .header("Origin", "http://localhost")
                    .header("Host", "localhost")
                    .body("{\"clientState\":{\"localStorage\":{}}}"));
        }

        assertEquals(403, response.status());
        assertError(response, 403, "UJFE_CSRF_VALIDATION_FAILED", "The request could not be verified.");
    }

    @Test
    void developmentOverrideAllowsMissingCsrfToken() throws Exception {
        UjfeServlet servlet = new UjfeServlet(new Router().register(new HomePage()),
                LiveSessionConfig.builder().disableCsrfProtectionForDevelopmentUnsafe().build());
        TestResponse page = service(servlet, TestRequest.get("/"));
        String eventId = firstEventId(page.body());

        TestResponse response = service(servlet, TestRequest.post(LiveHttpPaths.EVENT)
                .body("{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}"));

        assertEquals(200, response.status());
        assertTrue(response.body().contains("\"html\""));
        assertFalse(page.body().contains("ujfe-csrf-token"));
    }

    @Test
    void rejectsMalformedLiveJsonWithSafeCodecError() throws Exception {
        UjfeServlet servlet = new UjfeServlet(new Router().register(new HomePage()));

        TestResponse response;
        try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
            response = service(servlet, TestRequest.post(LiveHttpPaths.EVENT)
                    .body("{\"eventId\":}"));
        }

        assertError(response, 400, "UJFE_BAD_REQUEST", "The request is invalid.");
    }

    @Test
    void rejectsOversizedLiveJsonWithConfiguredLimit() throws Exception {
        UjfeServlet servlet = new UjfeServlet(new Router().register(new HomePage()),
                LiveSessionConfig.defaults(),
                32);

        TestResponse response;
        try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
            response = service(servlet, TestRequest.post(LiveHttpPaths.EVENT)
                    .body("{\"eventId\":\"evt-42\",\"clientState\":{\"localStorage\":{}}}"));
        }

        assertError(response, 413, "UJFE_BAD_REQUEST", "The request is invalid.");
    }

    @Test
    void rateLimitsEventEndpoint() throws Exception {
        LiveSessionConfig config = LiveSessionConfig.builder()
                .disableCsrfProtectionForDevelopmentUnsafe()
                .internalEndpointRateLimit(1, 1, Duration.ofMinutes(1))
                .build();
        UjfeServlet servlet = new UjfeServlet(new Router().register(new HomePage()), config);
        TestResponse page = service(servlet, TestRequest.get("/"));
        String eventId = firstEventId(page.body());

        TestResponse first = service(servlet, TestRequest.post(LiveHttpPaths.EVENT)
                .body("{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}"));
        TestResponse second;
        try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
            second = service(servlet, TestRequest.post(LiveHttpPaths.EVENT)
                    .body("{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}"));
        }

        assertEquals(200, first.status());
        assertError(second, 429, "UJFE_RATE_LIMITED", "Too many requests.");
        assertNotNull(second.header("Retry-After"));
    }

    @Test
    void rateLimitsStateEndpoint() throws Exception {
        LiveSessionConfig config = LiveSessionConfig.builder()
                .disableCsrfProtectionForDevelopmentUnsafe()
                .internalEndpointRateLimit(1, 1, Duration.ofMinutes(1))
                .build();
        UjfeServlet servlet = new UjfeServlet(new Router().register(new HomePage()), config);
        service(servlet, TestRequest.get("/"));

        TestResponse first = service(servlet, TestRequest.post(LiveHttpPaths.STATE)
                .body("{\"clientState\":{\"localStorage\":{}}}"));
        TestResponse second;
        try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
            second = service(servlet, TestRequest.post(LiveHttpPaths.STATE)
                    .body("{\"clientState\":{\"localStorage\":{}}}"));
        }

        assertEquals(200, first.status());
        assertError(second, 429, "UJFE_RATE_LIMITED", "Too many requests.");
    }

    @Test
    void publicPageRouteIsNotRateLimited() throws Exception {
        LiveSessionConfig config = LiveSessionConfig.builder()
                .internalEndpointRateLimit(1, 1, Duration.ofMinutes(1))
                .build();
        UjfeServlet servlet = new UjfeServlet(new Router().register(new HomePage()), config);

        assertEquals(200, service(servlet, TestRequest.get("/")).status());
        assertEquals(200, service(servlet, TestRequest.get("/")).status());
    }

    @Test
    void routeClaimingIsExplicitAndDoesNotClaimUnrelatedRoutes() throws Exception {
        UjfeServlet servlet = new UjfeServlet(new Router().register(new HomePage()));

        assertTrue(servlet.handles("GET", "/"));
        assertTrue(servlet.handles("GET", LiveHttpPaths.CLIENT_SCRIPT));
        assertFalse(servlet.handles("GET", "/assets/app.css"));
        assertFalse(servlet.handles("POST", "/"));

        TestResponse response = service(servlet, TestRequest.get("/assets/app.css"));

        assertError(response, 404, "UJFE_ROUTE_NOT_FOUND", "The requested route was not found.");
        assertFalse(response.body().contains("/assets/app.css"));
    }

    @Test
    void rejectsWrongMethodsOnKnownRoutesAndInternalEndpoints() throws Exception {
        UjfeServlet servlet = new UjfeServlet(new Router().register(new HomePage()));

        TestResponse pagePost = service(servlet, TestRequest.post("/"));
        TestResponse clientPost = service(servlet, TestRequest.post(LiveHttpPaths.CLIENT_SCRIPT));

        assertEquals(405, pagePost.status());
        assertEquals(405, clientPost.status());
        assertError(pagePost, 405, "UJFE_INVALID_REQUEST", "The request is invalid.");
        assertError(clientPost, 405, "UJFE_INVALID_REQUEST", "The request is invalid.");
    }

    @Test
    void customAndDisabledSecurityHeadersAreApplied() throws Exception {
        LiveSessionConfig customConfig = LiveSessionConfig.builder()
                .securityHeaders(SecurityHeadersConfig.builder()
                        .header(SecurityHeadersConfig.REFERRER_POLICY, "same-origin")
                        .header(SecurityHeadersConfig.CONTENT_SECURITY_POLICY, "default-src 'self'")
                        .build())
                .build();
        UjfeServlet customServlet = new UjfeServlet(new Router().register(new HomePage()), customConfig);

        TestResponse custom = service(customServlet, TestRequest.get("/"));

        assertEquals("same-origin", custom.header("Referrer-Policy"));
        assertEquals("default-src 'self'", custom.header("Content-Security-Policy"));
        assertEquals("nosniff", custom.header("X-Content-Type-Options"));

        UjfeServlet disabledServlet = new UjfeServlet(new Router().register(new HomePage()),
                LiveSessionConfig.builder().disableSecurityHeaders().build());

        TestResponse disabled = service(disabledServlet, TestRequest.get("/"));

        assertNull(disabled.header("Referrer-Policy"));
        assertNull(disabled.header("Content-Security-Policy"));
        assertNull(disabled.header("X-Content-Type-Options"));
    }

    @Test
    void renderFailureReturnsSafeJsonErrorResponse() throws Exception {
        UjfeServlet servlet = new UjfeServlet(new Router().register(new FailingRenderPage()));

        TestResponse response;
        try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
            response = service(servlet, TestRequest.get("/")
                    .header("X-Request-Id", "req-render-1"));
        }

        assertError(response, 500, "UJFE_RENDER_ERROR", "An error occurred while rendering the page.");
        assertTrue(response.body().contains("\"requestId\":\"req-render-1\""));
        assertFalse(response.body().contains("render-secret"));
        assertFalse(response.body().contains("FailingRenderPage"));
    }

    @Test
    void eventFailureReturnsSafeJsonErrorResponse() throws Exception {
        LiveSessionConfig config = LiveSessionConfig.builder()
                .disableCsrfProtectionForDevelopmentUnsafe()
                .build();
        UjfeServlet servlet = new UjfeServlet(new Router().register(new FailingEventPage()), config);
        TestResponse page = service(servlet, TestRequest.get("/"));
        String eventId = firstEventId(page.body());

        TestResponse response;
        try (CodecLogSilencer ignored = CodecLogSilencer.attach()) {
            response = service(servlet, TestRequest.post(LiveHttpPaths.EVENT)
                    .body("{\"eventId\":\"" + eventId + "\",\"clientState\":{\"localStorage\":{}}}"));
        }

        assertError(response, 500, "UJFE_EVENT_HANDLER_ERROR", "An error occurred while handling the live event.");
        assertFalse(response.body().contains("event-secret"));
        assertFalse(response.body().contains("FailingEventPage"));
    }

    @Test
    void constructorsSupportExplicitLiveSessionConfigAndExternalSession() throws Exception {
        Router configuredRouter = new Router().register(new HomePage());
        UjfeServlet configuredServlet = new UjfeServlet(configuredRouter, LiveSessionConfig.builder()
                .title("Configured")
                .build(),
                4096);

        TestResponse configuredResponse = service(configuredServlet, TestRequest.get("/"));

        assertTrue(configuredResponse.body().contains("<title>Configured</title>"));

        Router externalRouter = new Router().register(new HomePage());
        try (LiveSession externalSession = new LiveSession(externalRouter)) {
            UjfeServlet externalServlet = new UjfeServlet(externalRouter, externalSession, 4096);
            TestResponse externalResponse = service(externalServlet, TestRequest.get("/"));

            assertEquals(200, externalResponse.status());
            assertTrue(externalServlet.handles("GET", "/"));
        }
    }

    @Test
    void canInitializeFromServletContextRouterAttribute() throws Exception {
        Map<String, Object> attributes = Map.of(UjfeServlet.ROUTER_ATTRIBUTE, new Router().register(new HomePage()));
        UjfeServlet servlet = new UjfeServlet();

        servlet.init(servletConfig(attributes, Map.of(), Map.of()));
        TestResponse response = service(servlet, TestRequest.get("/"));

        assertEquals(200, response.status());
        assertTrue(servlet.handles("GET", "/"));
    }

    @Test
    void canInitializeRoutesAndLiveConfigFromServletInitParameters() throws Exception {
        UjfeServlet servlet = new UjfeServlet();
        Map<String, String> servletParameters = Map.of(
                UjfeServletSettings.ROUTE_PACKAGES, "ujfe.servlet.fixtures",
                UjfeServletSettings.TITLE, "Init Param App"
        );

        servlet.init(servletConfig(Map.of(), Map.of(), servletParameters));
        TestResponse response = service(servlet, TestRequest.get("/scanned"));

        assertEquals(200, response.status());
        assertTrue(response.body().contains("<title>Init Param App</title>"));
        assertTrue(response.body().contains("Scanned servlet page"));
    }

    @Test
    void liveSessionContextAttributeRequiresRouterForSafeRouteClaiming() {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()))) {
            Map<String, Object> attributes = Map.of(UjfeServlet.LIVE_SESSION_ATTRIBUTE, session);
            UjfeServlet servlet = new UjfeServlet();

            ServletException failure = assertThrows(ServletException.class,
                    () -> servlet.init(servletConfig(attributes, Map.of(), Map.of())));

            assertTrue(failure.getMessage().contains(UjfeServlet.ROUTER_ATTRIBUTE));
        }
    }

    @Test
    void servletSettingsReadApplicationPropertiesModel() {
        Properties properties = new Properties();
        properties.setProperty(UjfeServletSettings.ROUTE_PACKAGES, "app.pages, app.admin");
        properties.setProperty(UjfeServletSettings.TITLE, "Servlet App");
        properties.setProperty(UjfeServletSettings.LANG, "pt-BR");
        properties.setProperty(UjfeServletSettings.DEV_TOOLS_ENABLED, "true");
        properties.setProperty(UjfeServletSettings.CSS_MODE, "external");
        properties.setProperty(UjfeServletSettings.MAX_JSON_PAYLOAD_BYTES, "2048");
        properties.setProperty(UjfeServletSettings.DEVELOPMENT_ERROR_DETAILS_ENABLED, "true");
        properties.setProperty(UjfeServletSettings.SECURITY_HEADER_REFERRER_POLICY, "same-origin");
        properties.setProperty(UjfeServletSettings.SECURITY_HEADER_CONTENT_SECURITY_POLICY, "default-src 'self'");
        properties.setProperty(UjfeServletSettings.RATE_LIMIT_ENABLED, "true");
        properties.setProperty(UjfeServletSettings.RATE_LIMIT_CAPACITY, "50");
        properties.setProperty(UjfeServletSettings.RATE_LIMIT_REFILL_TOKENS, "25");
        properties.setProperty(UjfeServletSettings.RATE_LIMIT_REFILL_PERIOD_MS, "30000");
        properties.setProperty(UjfeServletSettings.TRUSTED_PROXIES, "10.0.0.1,10.0.0.2");
        properties.setProperty(UjfeServletSettings.CLIENT_STATE_COOKIES, "ujfe_demo,theme");
        properties.setProperty(UjfeServletSettings.CLIENT_STATE_LOCAL_STORAGE_KEYS, "ujfe.theme");
        properties.setProperty(UjfeServletSettings.CLIENT_STATE_SESSION_STORAGE_KEYS, "ujfe.tab");

        UjfeServletSettings settings = UjfeServletSettings.fromProperties(properties);
        LiveSessionConfig liveConfig = settings.toLiveSessionConfig();

        assertEquals(java.util.List.of("app.pages", "app.admin"), settings.routePackages());
        assertEquals("Servlet App", liveConfigTitle(liveConfig));
        assertEquals(2048, settings.maxJsonPayloadBytes());
        assertTrue(liveConfig.isDevelopmentErrorDetailsEnabled());
        assertEquals("same-origin", liveConfig.securityHeaders().get(SecurityHeadersConfig.REFERRER_POLICY));
        assertEquals("default-src 'self'", liveConfig.securityHeaders().get(SecurityHeadersConfig.CONTENT_SECURITY_POLICY));
        assertTrue(liveConfig.isInternalEndpointRateLimitingEnabled());
        assertEquals(50, liveConfig.internalEndpointRateLimitCapacity());
        assertEquals(25, liveConfig.internalEndpointRateLimitRefillTokens());
        assertEquals(Duration.ofSeconds(30), liveConfig.internalEndpointRateLimitRefillPeriod());
        assertEquals(java.util.Set.of("10.0.0.1", "10.0.0.2"), liveConfig.trustedProxyAddresses());
        assertEquals(java.util.Set.of("ujfe_demo", "theme"), liveConfig.clientStatePolicy().allowedCookies());
        assertEquals(java.util.Set.of("ujfe.theme"), liveConfig.clientStatePolicy().allowedLocalStorageKeys());
        assertEquals(java.util.Set.of("ujfe.tab"), liveConfig.clientStatePolicy().allowedSessionStorageKeys());
    }

    @Test
    void servletSettingsReadApplicationYamlModel() {
        UjfeServletSettings settings = UjfeServletSettings
                .fromYaml("ujfe:\n"
                + "  routes:\n"
                + "    packages: app.pages,app.admin\n"
                + "  errors:\n"
                + "    development-details:\n"
                + "      enabled: true\n"
                + "  live:\n"
                + "    title: 'YAML App'\n"
                + "    lang: en\n"
                + "    dev-tools-enabled: true\n"
                + "    max-json-payload-bytes: 4096\n"
                + "    css-mode: internal\n"
                + "    rate-limit:\n"
                + "      enabled: false\n"
                + "      capacity: 20\n"
                + "      refill-tokens: 10\n"
                + "      refill-period-ms: 15000\n"
                + "    trusted-proxies: 10.0.0.1\n"
                + "  client-state:\n"
                + "    cookies: ujfe_demo\n"
                + "    local-storage-keys: ujfe.theme\n"
                + "    session-storage-keys: ujfe.tab\n"
                + "  security:\n"
                + "    headers:\n"
                + "      enabled: false\n");

        assertEquals(java.util.List.of("app.pages", "app.admin"), settings.routePackages());
        LiveSessionConfig liveConfig = settings.toLiveSessionConfig();
        assertEquals("YAML App", liveConfigTitle(liveConfig));
        assertEquals(4096, settings.maxJsonPayloadBytes());
        assertTrue(liveConfig.isDevelopmentErrorDetailsEnabled());
        assertTrue(liveConfig.securityHeaders().isEmpty());
        assertFalse(liveConfig.isInternalEndpointRateLimitingEnabled());
        assertEquals(20, liveConfig.internalEndpointRateLimitCapacity());
        assertEquals(10, liveConfig.internalEndpointRateLimitRefillTokens());
        assertEquals(Duration.ofSeconds(15), liveConfig.internalEndpointRateLimitRefillPeriod());
        assertEquals(java.util.Set.of("10.0.0.1"), liveConfig.trustedProxyAddresses());
        assertEquals(java.util.Set.of("ujfe_demo"), liveConfig.clientStatePolicy().allowedCookies());
        assertEquals(java.util.Set.of("ujfe.theme"), liveConfig.clientStatePolicy().allowedLocalStorageKeys());
        assertEquals(java.util.Set.of("ujfe.tab"), liveConfig.clientStatePolicy().allowedSessionStorageKeys());
    }

    @Test
    void servletSettingsDefaultToSharedCodecPayloadLimit() {
        UjfeServletSettings settings = UjfeServletSettings.fromProperties(new Properties());

        assertEquals(LiveHttpCodec.DEFAULT_MAX_JSON_PAYLOAD_BYTES, settings.maxJsonPayloadBytes());
    }

    @Test
    void pathWithinApplicationFallsBackToServletPathAndPathInfo() {
        HttpServletRequest request = TestRequest.get(null)
                .contextPath("/app")
                .servletPath("/docs")
                .pathInfo("/intro")
                .toRequest();

        assertEquals("/docs/intro", UjfeServlet.pathWithinApplication(request));
    }

    private static TestResponse service(UjfeServlet servlet, TestRequest request) throws IOException {
        TestResponse response = new TestResponse();
        servlet.service(request.toRequest(), response.toResponse());
        return response;
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

    private static void assertError(TestResponse response, int status, String code, String message) {
        assertEquals(status, response.status());
        assertTrue(response.contentType().startsWith("application/json"));
        assertTrue(response.body().contains("\"code\":\"" + code + "\""), response.body());
        assertTrue(response.body().contains("\"message\":\"" + message + "\""), response.body());
        assertFalse(response.body().contains("Exception"), response.body());
        assertFalse(response.body().contains("LiveHttpCodec"), response.body());
        assertFalse(response.body().contains("/Users/"), response.body());
    }

    private static String liveConfigTitle(LiveSessionConfig config) {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()), config)) {
            return session.renderDocument("/", ujfe.core.ClientState.empty())
                    .replaceFirst("(?s).*<title>", "")
                    .replaceFirst("</title>.*", "");
        }
    }

    private static LiveSessionConfig clientStateConfig() {
        return LiveSessionConfig.builder()
                .allowClientCookie("ujfe_demo")
                .allowLocalStorageKey("theme")
                .build();
    }

    private static ServletConfig servletConfig(
            Map<String, Object> attributes,
            Map<String, String> contextParameters,
            Map<String, String> servletParameters
    ) {
        ServletContext context = proxy(ServletContext.class, (name, args, returnType) -> {
            if ("getAttribute".equals(name)) {
                return attributes.get(args[0]);
            }
            if ("getInitParameter".equals(name)) {
                return contextParameters.get(args[0]);
            }
            if ("getInitParameterNames".equals(name)) {
                return Collections.enumeration(contextParameters.keySet());
            }
            return defaultValue(returnType);
        });

        return proxy(ServletConfig.class, (name, args, returnType) -> {
            if ("getServletContext".equals(name)) {
                return context;
            }
            if ("getInitParameter".equals(name)) {
                return servletParameters.get(args[0]);
            }
            if ("getInitParameterNames".equals(name)) {
                return Collections.enumeration(servletParameters.keySet());
            }
            if ("getServletName".equals(name)) {
                return "ujfe";
            }
            return defaultValue(returnType);
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, MethodHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) ->
                handler.invoke(method.getName(), args == null ? new Object[0] : args, method.getReturnType()));
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (boolean.class.equals(returnType)) {
            return false;
        }
        if (long.class.equals(returnType)) {
            return 0L;
        }
        if (int.class.equals(returnType)) {
            return 0;
        }
        if (void.class.equals(returnType)) {
            return null;
        }
        return 0;
    }

    @Page("/")
    public static final class HomePage {
        private int clicks;

        public Node render() {
            return div()
                    .child(p("Home"))
                    .child(p(() -> "Cookie: " + ujfe.core.Ujfe.cookie("ujfe_demo").orElse("ativo")))
                    .child(p(() -> "Clicks: " + clicks))
                    .child(button("Click").onClick(() -> clicks++));
        }
    }

    @Page("/")
    public static final class StrictCookiePage {
        public Node render() {
            return p(() -> "Cookie: " + ujfe.core.Ujfe.cookie("ujfe_demo").orElse("missing"));
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

    private static final class TestRequest {
        private final String method;
        private final Map<String, String> parameters = new LinkedHashMap<>();
        private final Map<String, String> headers = new LinkedHashMap<>();
        private String requestUri;
        private String contextPath = "";
        private String servletPath = "";
        private String pathInfo;
        private String scheme = "http";
        private String remoteAddr = "127.0.0.1";
        private String body = "";

        private TestRequest(String method, String requestUri) {
            this.method = method;
            this.requestUri = requestUri;
            headers.put("Cookie", "ujfe_demo=ativo");
        }

        static TestRequest get(String requestUri) {
            return new TestRequest("GET", requestUri);
        }

        static TestRequest get(String requestUri, String contextPath) {
            return get(requestUri).contextPath(contextPath);
        }

        static TestRequest post(String requestUri) {
            return new TestRequest("POST", requestUri);
        }

        TestRequest parameter(String name, String value) {
            parameters.put(name, value);
            return this;
        }

        TestRequest header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        TestRequest body(String body) {
            this.body = body;
            return this;
        }

        TestRequest contextPath(String contextPath) {
            this.contextPath = contextPath;
            return this;
        }

        TestRequest servletPath(String servletPath) {
            this.servletPath = servletPath;
            return this;
        }

        TestRequest pathInfo(String pathInfo) {
            this.pathInfo = pathInfo;
            return this;
        }

        HttpServletRequest toRequest() {
            return proxy(HttpServletRequest.class, (name, args, returnType) -> {
                if ("getMethod".equals(name)) {
                    return method;
                }
                if ("getRequestURI".equals(name)) {
                    return requestUri;
                }
                if ("getContextPath".equals(name)) {
                    return contextPath;
                }
                if ("getServletPath".equals(name)) {
                    return servletPath;
                }
                if ("getPathInfo".equals(name)) {
                    return pathInfo;
                }
                if ("getParameter".equals(name)) {
                    return parameters.get(args[0]);
                }
                if ("getHeader".equals(name)) {
                    return headers.get(args[0]);
                }
                if ("getScheme".equals(name)) {
                    return scheme;
                }
                if ("getRemoteAddr".equals(name)) {
                    return remoteAddr;
                }
                if ("getReader".equals(name)) {
                    return new BufferedReader(new StringReader(body));
                }
                if ("getContentLengthLong".equals(name)) {
                    return (long) body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                }
                if ("getContentLength".equals(name)) {
                    return body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
                }
                return defaultValue(returnType);
            });
        }
    }

    private static final class TestResponse {
        private final StringWriter body = new StringWriter();
        private final Map<String, String> headers = new LinkedHashMap<>();
        private int status;
        private String contentType = "";
        private String characterEncoding = "";

        HttpServletResponse toResponse() {
            PrintWriter writer = new PrintWriter(body);
            return proxy(HttpServletResponse.class, (name, args, returnType) -> {
                if ("setStatus".equals(name)) {
                    status = (int) args[0];
                    return null;
                }
                if ("setContentType".equals(name)) {
                    contentType = (String) args[0];
                    return null;
                }
                if ("setCharacterEncoding".equals(name)) {
                    characterEncoding = (String) args[0];
                    return null;
                }
                if ("setHeader".equals(name)) {
                    headers.put((String) args[0], (String) args[1]);
                    return null;
                }
                if ("getWriter".equals(name)) {
                    return writer;
                }
                return defaultValue(returnType);
            });
        }

        int status() {
            return status;
        }

        String contentType() {
            return contentType;
        }

        String body() {
            return body.toString();
        }

        String header(String name) {
            return headers.get(name);
        }

        String characterEncoding() {
            return characterEncoding;
        }
    }

    private interface MethodHandler {
        Object invoke(String name, Object[] args, Class<?> returnType) throws Throwable;
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
}
