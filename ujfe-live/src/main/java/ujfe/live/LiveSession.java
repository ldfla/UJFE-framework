package ujfe.live;

import ujfe.core.*;
import ujfe.html.CssTheme;
import ujfe.router.PageRenderer;
import ujfe.router.RouteDefinition;
import ujfe.router.Router;
import ujfe.runtime.action.*;
import ujfe.runtime.lifecycle.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class LiveSession implements AutoCloseable {
    private final Router router;
    private final PageRenderer pageRenderer;
    private final LiveEventRegistry eventRegistry;
    private final LiveComponentRenderer componentRenderer;
    private final LiveSessionConfig config;
    private final RuntimeActionRegistry runtimeActions;
    private final LifecycleRuntime lifecycleRuntime;
    private final String csrfToken;
    private final String sessionId;
    private final RateLimiter rateLimiter;
    private final ReentrantReadWriteLock sessionLock = new ReentrantReadWriteLock(true);
    private final Lock readLock = sessionLock.readLock();
    private final Lock writeLock = sessionLock.writeLock();
    private String currentPath = "/";
    private Object currentPage;
    private ClientState clientState = ClientState.empty();

    public LiveSession(Router router) {
        this(router, LiveSessionConfig.defaults());
    }

    public LiveSession(Router router, Supplier<CssTheme> themeSupplier) {
        this(router, LiveSessionConfig.builder()
                .themeSupplier(themeSupplier)
                .build());
    }

    public LiveSession(Router router, Supplier<CssTheme> themeSupplier, boolean devToolsEnabled) {
        this(router, LiveSessionConfig.builder()
                .themeSupplier(themeSupplier)
                .devToolsEnabled(devToolsEnabled)
                .build());
    }

    public LiveSession(Router router, LiveSessionConfig config) {
        this(router, new PageRenderer(), new LiveEventRegistry(), config);
    }

    public LiveSession(Router router, PageRenderer pageRenderer, LiveEventRegistry eventRegistry) {
        this(router, pageRenderer, eventRegistry, LiveSessionConfig.defaults());
    }

    public LiveSession(
            Router router,
            PageRenderer pageRenderer,
            LiveEventRegistry eventRegistry,
            Supplier<CssTheme> themeSupplier
    ) {
        this(router, pageRenderer, eventRegistry, LiveSessionConfig.builder()
                .themeSupplier(themeSupplier)
                .build());
    }

    public LiveSession(
            Router router,
            PageRenderer pageRenderer,
            LiveEventRegistry eventRegistry,
            Supplier<CssTheme> themeSupplier,
            boolean devToolsEnabled
    ) {
        this(router, pageRenderer, eventRegistry, LiveSessionConfig.builder()
                .themeSupplier(themeSupplier)
                .devToolsEnabled(devToolsEnabled)
                .build());
    }

    public LiveSession(
            Router router,
            PageRenderer pageRenderer,
            LiveEventRegistry eventRegistry,
            LiveSessionConfig config
    ) {
        this.router = Objects.requireNonNull(router, "router");
        this.pageRenderer = Objects.requireNonNull(pageRenderer, "pageRenderer");
        this.eventRegistry = Objects.requireNonNull(eventRegistry, "eventRegistry");
        this.config = Objects.requireNonNull(config, "config");
        this.runtimeActions = config.runtimeActions();
        this.lifecycleRuntime = LifecycleRuntime.create();
        this.componentRenderer = new LiveComponentRenderer(
                eventRegistry,
                ElementIdGenerator.sequential(),
                config.themeSupplier(),
                config.cssMode()
        );
        this.csrfToken = LiveHttpSecurity.generateCsrfToken();
        this.sessionId = UUID.randomUUID().toString();
        this.rateLimiter = new TokenBucketRateLimiter(config);
    }

    public String csrfToken() {
        return csrfToken;
    }

    public String sessionId() {
        return sessionId;
    }

    public RateLimitMetrics rateLimitMetrics() {
        return rateLimiter.metrics();
    }

    public boolean isDevelopmentErrorDetailsEnabled() {
        return config.isDevelopmentErrorDetailsEnabled();
    }

    public SecurityHeadersConfig securityHeadersConfig() {
        return config.securityHeadersConfig();
    }

    public Map<String, String> securityHeaders() {
        return config.securityHeaders();
    }

    public void checkInternalEndpointRateLimit(String endpointPath, LiveHttpRequestMetadata metadata) {
        RateLimitDecision decision = rateLimiter.allow(new RateLimitRequest(endpointPath, sessionId, metadata));
        if (!decision.allowed()) {
            LiveRateLimitException exception = new LiveRateLimitException(
                    endpointPath,
                    decision.keyType(),
                    decision.retryAfter().orElse(null)
            );
            reportHttpError(
                    exception,
                    RuntimePhase.INTERNAL,
                    endpointPath,
                    null,
                    nextTraceId(),
                    Map.of("path", endpointPath),
                    errorMetadata(UjfeErrorCode.UJFE_RATE_LIMITED, 429, "rate_limit")
            );
            throw exception;
        }
    }

    public LiveRenderResult renderPath(String path) {
        writeLock.lock();
        try {
            return renderPathLocked(path, null);
        } finally {
            writeLock.unlock();
        }
    }

    private LiveRenderResult renderPathLocked(String path, String eventId) {
        String traceId = nextTraceId();
        Instant start = Instant.now();
        RouteDefinition route;
        Object page;

        try {
            route = router.resolve(path)
                    .orElseThrow(() -> new IllegalArgumentException("No UJFE route registered for " + path));
            page = pageFor(route);
        } catch (Exception exception) {
            runtimeActions.executeOnError(new RuntimeErrorContext(
                    exception, RuntimePhase.ROUTING, path, null, traceId,
                    errorMetadata(UjfeErrorCode.UJFE_ROUTE_NOT_FOUND, 404, "routing")));
            throw exception;
        }

        RenderContext renderContext = new RenderContext(
                route.path(), page, this, Map.of(), clientState, start, traceId, Map.of());
        runtimeActions.executeBeforeRender(renderContext);

        LiveRenderResult result;
        LifecycleTracker lifecycleTracker = lifecycleRuntime.beginRender(new LifecycleContext(
                route.path(), this, traceId, Map.of()));
        try {
            result = componentRenderer.render(() -> pageRenderer.render(page), clientState, lifecycleTracker, route.path());
            routeLifecycleFailures(lifecycleTracker.complete(), route.path(), eventId, traceId);
            currentPath = route.path();
            currentPage = page;
        } catch (Exception exception) {
            lifecycleTracker.abort();
            routeRenderFailure(exception, route.path(), eventId, traceId);
            throw exception;
        }

        Duration duration = Duration.between(start, Instant.now());
        RenderResult renderResult = new RenderResult(
                result.html(), result.css(), currentPath, duration, traceId,
                config.headNodes(), Map.of(), Map.of(), Map.of());
        runtimeActions.executeAfterRender(renderResult);

        return result;
    }

    public LiveRenderResult handleEvent(String eventId, ClientState nextClientState) {
        return handleEvent(eventId, nextClientState, new LiveHttpRequestMetadata(null, null, null, null));
    }

    public LiveRenderResult handleEvent(String eventId, ClientState nextClientState, LiveHttpRequestMetadata metadata) {
        return handleEvent(eventId, "", nextClientState, metadata);
    }

    public LiveRenderResult handleEvent(
            String eventId,
            String eventValue,
            ClientState nextClientState,
            LiveHttpRequestMetadata metadata
    ) {
        writeLock.lock();
        try {
            return handleEventLocked(eventId, eventValue, nextClientState, metadata);
        } finally {
            writeLock.unlock();
        }
    }

    private LiveRenderResult handleEventLocked(
            String eventId,
            String eventValue,
            ClientState nextClientState,
            LiveHttpRequestMetadata metadata
    ) {
        String traceId = nextTraceId();
        try {
            LiveHttpSecurity.validateCsrf(config, csrfToken, metadata);
        } catch (RuntimeException exception) {
            runtimeActions.executeOnError(new RuntimeErrorContext(
                    exception, RuntimePhase.EVENT, currentPath, eventId, traceId,
                    errorMetadata(UjfeErrorCode.UJFE_CSRF_VALIDATION_FAILED, 403, "csrf")));
            throw exception;
        }

        Instant start = Instant.now();

        ClientState eventClientState = clientState.mergeCookiesAndReplaceLocalStorage(nextClientState);

        LiveEventContext eventContext = new LiveEventContext(
                eventId, "live", this, Map.of(), eventClientState, null, Map.of(), traceId, Map.of());
        runtimeActions.executeBeforeEvent(eventContext);

        LiveRenderResult result;
        try {
            clientState = eventClientState;
            componentRenderer.handleWithClientState(clientState, () -> eventRegistry.handle(eventId, eventValue));
            result = renderPathLocked(currentPath, eventId);
        } catch (Exception exception) {
            runtimeActions.executeOnError(new RuntimeErrorContext(
                    exception, RuntimePhase.EVENT, currentPath, eventId, traceId,
                    errorMetadata(UjfeErrorCode.UJFE_EVENT_HANDLER_ERROR, 500, "event")));
            throw exception;
        }

        Duration duration = Duration.between(start, Instant.now());
        LiveEventResult eventResult = new LiveEventResult(
                result.html(), eventId, duration, traceId,
                Map.of("eventType", "live"), Map.of("path", currentPath), clientStateMetadata(), Map.of());
        runtimeActions.executeAfterEvent(eventResult);

        return result;
    }

    public LiveRenderResult updateClientState(ClientState nextClientState) {
        return updateClientState(nextClientState, new LiveHttpRequestMetadata(null, null, null, null));
    }

    public LiveRenderResult updateClientState(ClientState nextClientState, LiveHttpRequestMetadata metadata) {
        writeLock.lock();
        try {
            return updateClientStateLocked(nextClientState, metadata);
        } finally {
            writeLock.unlock();
        }
    }

    private LiveRenderResult updateClientStateLocked(ClientState nextClientState, LiveHttpRequestMetadata metadata) {
        String traceId = nextTraceId();
        try {
            LiveHttpSecurity.validateCsrf(config, csrfToken, metadata);
        } catch (RuntimeException exception) {
            runtimeActions.executeOnError(new RuntimeErrorContext(
                    exception, RuntimePhase.STATE, currentPath, null, traceId,
                    errorMetadata(UjfeErrorCode.UJFE_CSRF_VALIDATION_FAILED, 403, "csrf")));
            throw exception;
        }
        mergeClientState(nextClientState);
        return renderPathLocked(currentPath, null);
    }

    public void reportHttpError(
            Throwable exception,
            RuntimePhase phase,
            String path,
            String eventId,
            String traceId,
            Map<String, Object> requestMetadata,
            Map<String, Object> runtimeMetadata
    ) {
        runtimeActions.executeOnError(new RuntimeErrorContext(
                exception,
                phase,
                path,
                eventId,
                traceId == null || traceId.isBlank() ? nextTraceId() : traceId,
                Map.of(),
                Map.of(),
                requestMetadata,
                Map.of("sessionReference", Integer.toHexString(sessionId.hashCode())),
                runtimeMetadata
        ));
    }

    public String renderDocument(String path, ClientState initialClientState) {
        writeLock.lock();
        try {
            mergeClientState(initialClientState);
            LiveRenderResult result = renderPathLocked(path, null);
            return "<!doctype html>"
                    + "<html lang=\"" + AttributeEscaper.escape(config.lang()) + "\">"
                    + "<head>"
                    + "<meta charset=\"utf-8\">"
                    + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                    + "<title>" + HtmlEscaper.escape(config.title()) + "</title>"
                    + renderHeadNodes()
                    + renderActionHeadContributions()
                    + renderInternalCss(result.css())
                    + renderCsrfMetaTag()
                    + "</head>"
                    + "<body>"
                    + "<div id=\"ujfe-root\">" + result.html() + "</div>"
                    + "<script src=\"/_ujfe/client.js\"></script>"
                    + (config.devToolsEnabled() ? "<script src=\"/_ujfe/dev.js\"></script>" : "")
                    + "</body>"
                    + "</html>";
        } finally {
            writeLock.unlock();
        }
    }

    public String renderCss(Collection<String> classes) {
        readLock.lock();
        try {
            return componentRenderer.renderCss(classes);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public void close() {
        writeLock.lock();
        try {
            String traceId = nextTraceId();
            List<LifecycleException> failures = lifecycleRuntime.cleanup(
                    new UnmountContext(currentPath, this, traceId, Map.of(), "session-close"));
            routeLifecycleFailures(failures, currentPath, null, traceId);
            eventRegistry.clear();
            currentPage = null;
        } finally {
            writeLock.unlock();
        }
    }

    private void mergeClientState(ClientState nextClientState) {
        clientState = clientState.mergeCookiesAndReplaceLocalStorage(nextClientState);
    }

    private Object pageFor(RouteDefinition route) {
        if (currentPage != null && route.path().equals(currentPath)) {
            return currentPage;
        }
        return route.createPage();
    }

    private void routeLifecycleFailures(
            List<LifecycleException> failures,
            String path,
            String eventId,
            String traceId
    ) {
        for (LifecycleException failure : failures) {
            runtimeActions.executeOnError(new RuntimeErrorContext(
                    failure, RuntimePhase.LIFECYCLE, path, eventId, traceId,
                    Map.of("callback", failure.callback())));
        }
    }

    private void routeRenderFailure(Exception exception, String path, String eventId, String traceId) {
        if (exception instanceof LifecycleException) {
            LifecycleException lifecycleException = (LifecycleException) exception;
            runtimeActions.executeOnError(new RuntimeErrorContext(
                    lifecycleException, RuntimePhase.LIFECYCLE, path, eventId, traceId,
                    Map.of("callback", lifecycleException.callback())));
            return;
        }
        runtimeActions.executeOnError(new RuntimeErrorContext(
                exception, RuntimePhase.RENDER, path, eventId, traceId,
                errorMetadata(UjfeErrorCode.UJFE_RENDER_ERROR, 500, "render")));
    }

    static Map<String, Object> errorMetadata(UjfeErrorCode code, int httpStatus, String reason) {
        return Map.of(
                "errorCode", code.name(),
                "httpStatus", httpStatus,
                "reason", reason
        );
    }

    private String renderHeadNodes() {
        if (config.headNodes().isEmpty()) {
            return "";
        }

        UjfeContext context = UjfeContext.create();
        StringBuilder html = new StringBuilder();
        for (Node headNode : config.headNodes()) {
            html.append(headNode.render(context));
        }
        return html.toString();
    }

    private String renderActionHeadContributions() {
        List<Node> contributions = runtimeActions.executeHeadContributions();
        if (contributions.isEmpty()) {
            return "";
        }
        UjfeContext context = UjfeContext.create();
        StringBuilder html = new StringBuilder();
        for (Node node : contributions) {
            html.append(node.render(context));
        }
        return html.toString();
    }

    private String renderInternalCss(String css) {
        if (config.cssMode() == CssMode.EXTERNAL) {
            return "";
        }
        return "<style data-ujfe-css>" + css + "</style>";
    }

    private String renderCsrfMetaTag() {
        if (config.isCsrfProtectionDisabled()) {
            return "";
        }
        return "<meta name=\"ujfe-csrf-token\" content=\"" + HtmlEscaper.escape(csrfToken) + "\">";
    }

    private Map<String, Object> clientStateMetadata() {
        return Map.of(
                "cookies", clientState.cookies(),
                "localStorage", clientState.localStorage()
        );
    }

    private static String nextTraceId() {
        return UUID.randomUUID().toString();
    }
}
