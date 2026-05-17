package ujfe.servlet;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import ujfe.core.Node;
import ujfe.live.LiveHttpPaths;
import ujfe.live.LiveSession;
import ujfe.live.LiveSessionConfig;
import ujfe.router.Page;
import ujfe.router.Router;

import java.io.*;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static ujfe.html.UI.*;

final class UjfeServletTest {
    @Test
    void rendersUjfePageThroughJakartaServletResponse() throws Exception {
        UjfeServlet servlet = new UjfeServlet(new Router().register(new HomePage()));
        TestResponse response = service(servlet, TestRequest.get("/app/", "/app"));

        assertEquals(200, response.status());
        assertTrue(response.contentType().startsWith("text/html"));
        assertEquals("UTF-8", response.characterEncoding());
        assertTrue(response.body().contains("Home"));
        assertTrue(response.body().contains("Cookie: ativo"));
        assertEquals("nosniff", response.header("X-Content-Type-Options"));
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
        UjfeServlet servlet = new UjfeServlet(new Router().register(new HomePage()));
        TestResponse page = service(servlet, TestRequest.get("/"));
        String eventId = firstEventId(page.body());

        TestResponse event = service(servlet, TestRequest.post(LiveHttpPaths.EVENT)
                .body("{\"eventId\":\"" + eventId + "\",\"clientState\":{\"cookies\":\"ujfe_demo=ativo\","
                        + "\"localStorage\":{\"theme\":\"dark\"}}}"));
        TestResponse state = service(servlet, TestRequest.post(LiveHttpPaths.STATE)
                .body("{\"clientState\":{\"cookies\":\"ujfe_demo=novo\",\"localStorage\":{}}}"));

        assertEquals(200, event.status());
        assertTrue(event.contentType().startsWith("application/json"));
        assertTrue(event.body().contains("\"html\""));
        assertEquals(200, state.status());
        assertTrue(state.body().contains("Cookie: novo"));
    }

    @Test
    void routeClaimingIsExplicitAndDoesNotClaimUnrelatedRoutes() throws Exception {
        UjfeServlet servlet = new UjfeServlet(new Router().register(new HomePage()));

        assertTrue(servlet.handles("GET", "/"));
        assertTrue(servlet.handles("GET", LiveHttpPaths.CLIENT_SCRIPT));
        assertFalse(servlet.handles("GET", "/assets/app.css"));
        assertFalse(servlet.handles("POST", "/"));

        TestResponse response = service(servlet, TestRequest.get("/assets/app.css"));

        assertEquals(404, response.status());
        assertEquals("No UJFE route registered for /assets/app.css", response.body());
    }

    @Test
    void rejectsWrongMethodsOnKnownRoutesAndInternalEndpoints() throws Exception {
        UjfeServlet servlet = new UjfeServlet(new Router().register(new HomePage()));

        TestResponse pagePost = service(servlet, TestRequest.post("/"));
        TestResponse clientPost = service(servlet, TestRequest.post(LiveHttpPaths.CLIENT_SCRIPT));

        assertEquals(405, pagePost.status());
        assertEquals(405, clientPost.status());
    }

    @Test
    void constructorsSupportExplicitLiveSessionConfigAndExternalSession() throws Exception {
        Router configuredRouter = new Router().register(new HomePage());
        UjfeServlet configuredServlet = new UjfeServlet(configuredRouter, LiveSessionConfig.builder()
                .title("Configured")
                .build());

        TestResponse configuredResponse = service(configuredServlet, TestRequest.get("/"));

        assertTrue(configuredResponse.body().contains("<title>Configured</title>"));

        Router externalRouter = new Router().register(new HomePage());
        try (LiveSession externalSession = new LiveSession(externalRouter)) {
            UjfeServlet externalServlet = new UjfeServlet(externalRouter, externalSession);
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
        Map<String, Object> attributes = Map.of(
                UjfeServlet.LIVE_SESSION_ATTRIBUTE,
                new LiveSession(new Router().register(new HomePage()))
        );
        UjfeServlet servlet = new UjfeServlet();

        ServletException failure = assertThrows(ServletException.class,
                () -> servlet.init(servletConfig(attributes, Map.of(), Map.of())));

        assertTrue(failure.getMessage().contains(UjfeServlet.ROUTER_ATTRIBUTE));
    }

    @Test
    void servletSettingsReadApplicationPropertiesModel() {
        Properties properties = new Properties();
        properties.setProperty(UjfeServletSettings.ROUTE_PACKAGES, "app.pages, app.admin");
        properties.setProperty(UjfeServletSettings.TITLE, "Servlet App");
        properties.setProperty(UjfeServletSettings.LANG, "pt-BR");
        properties.setProperty(UjfeServletSettings.DEV_TOOLS_ENABLED, "true");
        properties.setProperty(UjfeServletSettings.CSS_MODE, "external");

        UjfeServletSettings settings = UjfeServletSettings.fromProperties(properties);
        LiveSessionConfig liveConfig = settings.toLiveSessionConfig();

        assertEquals(java.util.List.of("app.pages", "app.admin"), settings.routePackages());
        assertEquals("Servlet App", liveConfigTitle(liveConfig));
    }

    @Test
    void servletSettingsReadApplicationYamlModel() {
        UjfeServletSettings settings = UjfeServletSettings.fromYaml(""
                + "ujfe:\n"
                + "  routes:\n"
                + "    packages: app.pages,app.admin\n"
                + "  live:\n"
                + "    title: 'YAML App'\n"
                + "    lang: en\n"
                + "    dev-tools-enabled: true\n"
                + "    css-mode: internal\n");

        assertEquals(java.util.List.of("app.pages", "app.admin"), settings.routePackages());
        assertEquals("YAML App", liveConfigTitle(settings.toLiveSessionConfig()));
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

    private static String liveConfigTitle(LiveSessionConfig config) {
        try (LiveSession session = new LiveSession(new Router().register(new HomePage()), config)) {
            return session.renderDocument("/", ujfe.core.ClientState.empty())
                    .replaceFirst("(?s).*<title>", "")
                    .replaceFirst("</title>.*", "");
        }
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

    private static final class TestRequest {
        private final String method;
        private final Map<String, String> parameters = new LinkedHashMap<>();
        private final Map<String, String> headers = new LinkedHashMap<>();
        private String requestUri;
        private String contextPath = "";
        private String servletPath = "";
        private String pathInfo;
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
                if ("getReader".equals(name)) {
                    return new BufferedReader(new StringReader(body));
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
}
