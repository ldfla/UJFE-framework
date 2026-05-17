package ujfe.servlet;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ujfe.core.ClientState;
import ujfe.live.LiveClientScript;
import ujfe.live.LiveDevToolsScript;
import ujfe.live.LiveHttpCodec;
import ujfe.live.LiveHttpCodecException;
import ujfe.live.LiveHttpEventPayload;
import ujfe.live.LiveHttpPaths;
import ujfe.live.LiveHttpSecurity;
import ujfe.live.LiveRenderResult;
import ujfe.live.LiveSession;
import ujfe.live.LiveSessionConfig;
import ujfe.router.Router;
import ujfe.router.source.ReflectionPageScanner;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public final class UjfeServlet extends HttpServlet {
    public static final String ROUTER_ATTRIBUTE = "ujfe.router";
    public static final String LIVE_SESSION_ATTRIBUTE = "ujfe.liveSession";

    private Router router;
    private LiveSession liveSession;
    private boolean ownsLiveSession;
    private int maxJsonPayloadBytes = LiveHttpCodec.DEFAULT_MAX_JSON_PAYLOAD_BYTES;

    public UjfeServlet() {
    }

    public UjfeServlet(Router router) {
        this(router, LiveSessionConfig.defaults());
    }

    public UjfeServlet(Router router, LiveSessionConfig config) {
        this(router, config, LiveHttpCodec.DEFAULT_MAX_JSON_PAYLOAD_BYTES);
    }

    public UjfeServlet(Router router, LiveSessionConfig config, int maxJsonPayloadBytes) {
        this(router, new LiveSession(router, config), true, maxJsonPayloadBytes);
    }

    public UjfeServlet(Router router, LiveSession liveSession) {
        this(router, liveSession, LiveHttpCodec.DEFAULT_MAX_JSON_PAYLOAD_BYTES);
    }

    public UjfeServlet(Router router, LiveSession liveSession, int maxJsonPayloadBytes) {
        this(router, liveSession, false, maxJsonPayloadBytes);
    }

    private UjfeServlet(Router router, LiveSession liveSession, boolean ownsLiveSession, int maxJsonPayloadBytes) {
        this.router = Objects.requireNonNull(router, "router");
        this.liveSession = Objects.requireNonNull(liveSession, "liveSession");
        this.ownsLiveSession = ownsLiveSession;
        LiveHttpCodec.requirePayloadSize(0, maxJsonPayloadBytes);
        this.maxJsonPayloadBytes = maxJsonPayloadBytes;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        if (liveSession != null && router != null) {
            return;
        }

        ServletContext context = config.getServletContext();
        Object configuredSession = context == null ? null : context.getAttribute(LIVE_SESSION_ATTRIBUTE);
        Object configuredRouter = context == null ? null : context.getAttribute(ROUTER_ATTRIBUTE);

        if (configuredSession instanceof LiveSession) {
            liveSession = (LiveSession) configuredSession;
            if (configuredRouter instanceof Router) {
                router = (Router) configuredRouter;
            } else {
                throw new ServletException("ServletContext attribute " + ROUTER_ATTRIBUTE
                        + " is required when " + LIVE_SESSION_ATTRIBUTE + " is provided");
            }
            ownsLiveSession = false;
            return;
        }

        UjfeServletSettings settings = UjfeServletSettings.from(config);
        maxJsonPayloadBytes = settings.maxJsonPayloadBytes();
        router = configuredRouter instanceof Router ? (Router) configuredRouter : routerFromSettings(settings);
        liveSession = new LiveSession(router, settings.toLiveSessionConfig());
        ownsLiveSession = true;
    }

    public boolean handles(String method, String path) {
        String normalizedPath = normalizePath(path);
        if (LiveHttpPaths.isInternalPath(normalizedPath)) {
            return true;
        }
        return "GET".equals(method) && router != null && router.resolve(normalizedPath).isPresent();
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ensureInitialized();
        String method = request.getMethod();
        String path = pathWithinApplication(request);

        try {
            if (LiveHttpPaths.isInternalPath(path)) {
                handleInternalEndpoint(method, path, request, response);
                return;
            }

            if (router.resolve(path).isEmpty()) {
                write(response, HttpServletResponse.SC_NOT_FOUND,
                        "text/plain; charset=utf-8",
                        "No UJFE route registered for " + path);
                return;
            }

            if (!"GET".equals(method)) {
                write(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                        "text/plain; charset=utf-8",
                        "Method not allowed");
                return;
            }

            Map<String, String> cookies = LiveHttpCodec.parseCookies(request.getHeader("Cookie"));
            String document = liveSession.renderDocument(path, ClientState.of(cookies, Map.of()));
            write(response, HttpServletResponse.SC_OK, "text/html; charset=utf-8", document);
        } catch (LiveHttpCodecException exception) {
            LiveHttpCodec.logRejectedPayload(exception, "servlet", correlationId(request));
            write(response, exception.httpStatus(), "text/plain; charset=utf-8", exception.safeMessage());
        } catch (IllegalArgumentException exception) {
            write(response, HttpServletResponse.SC_BAD_REQUEST, "text/plain; charset=utf-8", exception.getMessage());
        } catch (RuntimeException exception) {
            write(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "text/plain; charset=utf-8",
                    "Internal server error");
        }
    }

    @Override
    public void destroy() {
        if (ownsLiveSession && liveSession != null) {
            liveSession.close();
        }
        super.destroy();
    }

    private void handleInternalEndpoint(
            String method,
            String path,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        if ("GET".equals(method) && LiveHttpPaths.CLIENT_SCRIPT.equals(path)) {
            write(response, HttpServletResponse.SC_OK, "application/javascript; charset=utf-8", LiveClientScript.script());
            return;
        }

        if ("GET".equals(method) && LiveHttpPaths.DEV_SCRIPT.equals(path)) {
            write(response, HttpServletResponse.SC_OK, "application/javascript; charset=utf-8", LiveDevToolsScript.script());
            return;
        }

        if ("GET".equals(method) && LiveHttpPaths.CSS.equals(path)) {
            String classes = request.getParameter("classes");
            write(response, HttpServletResponse.SC_OK,
                    "text/css; charset=utf-8",
                    liveSession.renderCss(LiveHttpCodec.parseCssClasses(classes)));
            return;
        }

        if ("POST".equals(method) && LiveHttpPaths.EVENT.equals(path)) {
            LiveHttpEventPayload payload = LiveHttpCodec.parseEventPayload(
                    readBody(request),
                    maxJsonPayloadBytes
            );
            LiveRenderResult result = liveSession.handleEvent(payload.eventId(), payload.clientState());
            write(response, HttpServletResponse.SC_OK,
                    "application/json; charset=utf-8",
                    LiveHttpCodec.livePayload(result));
            return;
        }

        if ("POST".equals(method) && LiveHttpPaths.STATE.equals(path)) {
            LiveRenderResult result = liveSession.updateClientState(
                    LiveHttpCodec.parseStatePayload(readBody(request), maxJsonPayloadBytes)
            );
            write(response, HttpServletResponse.SC_OK,
                    "application/json; charset=utf-8",
                    LiveHttpCodec.livePayload(result));
            return;
        }

        write(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                "text/plain; charset=utf-8",
                "Method not allowed");
    }

    private Router routerFromSettings(UjfeServletSettings settings) throws ServletException {
        if (settings.routePackages().isEmpty()) {
            throw new ServletException("No UJFE router configured. Provide ServletContext attribute "
                    + ROUTER_ATTRIBUTE
                    + " or configure ujfe.routes.packages in application.properties/application.yml.");
        }
        return new Router().register(ReflectionPageScanner.forPackages(
                settings.routePackages().toArray(new String[0])));
    }

    private void ensureInitialized() {
        if (router == null || liveSession == null) {
            throw new IllegalStateException("UjfeServlet is not initialized");
        }
    }

    private String readBody(HttpServletRequest request) throws IOException {
        LiveHttpCodec.requirePayloadSize(request.getContentLengthLong(), maxJsonPayloadBytes);
        try (BufferedReader reader = request.getReader()) {
            return LiveHttpCodec.readPayload(reader, maxJsonPayloadBytes);
        }
    }

    private static void write(HttpServletResponse response, int status, String contentType, String content)
            throws IOException {
        response.setStatus(status);
        response.setContentType(contentType);
        response.setCharacterEncoding("UTF-8");
        LiveHttpSecurity.securityHeaders().forEach(response::setHeader);
        response.getWriter().write(content);
    }

    private static String correlationId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId != null && !requestId.isBlank()) {
            return requestId;
        }
        return request.getHeader("X-Correlation-Id");
    }

    static String pathWithinApplication(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (requestUri == null || requestUri.isBlank()) {
            requestUri = joinServletPath(request.getServletPath(), request.getPathInfo());
        }

        String contextPath = request.getContextPath();
        String path = requestUri == null ? "/" : requestUri;
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        return normalizePath(path);
    }

    private static String joinServletPath(String servletPath, String pathInfo) {
        String first = servletPath == null ? "" : servletPath;
        String second = pathInfo == null ? "" : pathInfo;
        return first + second;
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        return path.startsWith("/") ? path : "/" + path;
    }
}
