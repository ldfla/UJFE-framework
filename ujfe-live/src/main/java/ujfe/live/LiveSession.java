package ujfe.live;

import ujfe.core.*;
import ujfe.observability.EventTrace;
import ujfe.observability.ObservabilityConfig;
import ujfe.observability.RenderTrace;
import ujfe.observability.TraceStatus;
import ujfe.router.PageRenderer;
import ujfe.router.RouteDefinition;
import ujfe.router.Router;
import ujfe.runtime.action.*;
import ujfe.runtime.lifecycle.*;
import ujfe.validation.DocumentValidator;
import ujfe.validation.ValidationFinding;
import ujfe.validation.ValidationMode;
import ujfe.validation.ValidationResult;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static ujfe.core.UI.link;

public final class LiveSession implements AutoCloseable {
    private static final Logger VALIDATION_LOGGER = Logger.getLogger(LiveSession.class.getName());
    private static final LiveHttpRequestMetadata NO_HTTP_METADATA = new LiveHttpRequestMetadata(null, null, null, null);

    private final Router router;
    private final PageRenderer pageRenderer;
    private final LiveEventRegistry eventRegistry;
    private final LiveComponentRenderer componentRenderer;
    private final LiveSessionConfig config;
    private final RuntimeActionRegistry runtimeActions;
    private final ObservabilityConfig observability;
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
        this.observability = config.observabilityConfig();
        this.lifecycleRuntime = LifecycleRuntime.create();
        this.componentRenderer = new LiveComponentRenderer(
            eventRegistry,
            ElementIdGenerator.sequential(),
            config.themeSupplier(),
            config.cssMode()
        );
        this.csrfToken = LiveHttpSecurity.generateCsrfToken();
        this.sessionId = UUID.randomUUID()
            .toString();
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
                decision.retryAfter()
                    .orElse(null)
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
            return renderPathLocked(path, null, NO_HTTP_METADATA);
        } finally {
            writeLock.unlock();
        }
    }

    public boolean hasRoute(String path) {
        readLock.lock();
        try {
            return router.resolve(path)
                .isPresent();
        } finally {
            readLock.unlock();
        }
    }

    public Optional<RenderMode> renderMode(String path) {
        readLock.lock();
        try {
            return router.resolve(path)
                .map(RouteDefinition::renderMode);
        } finally {
            readLock.unlock();
        }
    }

    private LiveRenderResult renderPathLocked(String path, String eventId) {
        return renderPathLocked(path, eventId, NO_HTTP_METADATA);
    }

    private LiveRenderResult renderPathLocked(String path, String eventId, LiveHttpRequestMetadata metadata) {
        String traceId = nextTraceId();
        Instant start = observability.clock()
            .instant();
        RouteDefinition route;
        Object page;

        try {
            route = router.resolve(path)
                .orElseThrow(() -> new IllegalArgumentException("No UJFE route registered for " + path));
            page = pageFor(route);
        } catch (Exception exception) {
            emitRenderTrace(RenderTrace.builder()
                .traceId(traceId)
                .requestId(requestId(metadata))
                .route(path)
                .httpMethod(method(metadata))
                .httpStatus(404)
                .adapterName(adapterName(metadata))
                .startedAt(start)
                .duration(durationSince(start))
                .responseSizeBytes(0)
                .status(TraceStatus.NOT_FOUND)
                .errorCode(UjfeErrorCode.UJFE_ROUTE_NOT_FOUND.name())
                .errorType(errorType(exception))
                .source("routing")
                .build());
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
            emitRenderTrace(RenderTrace.builder()
                .traceId(traceId)
                .requestId(requestId(metadata))
                .route(route.path())
                .httpMethod(method(metadata))
                .httpStatus(500)
                .adapterName(adapterName(metadata))
                .startedAt(start)
                .duration(durationSince(start))
                .responseSizeBytes(0)
                .status(TraceStatus.SERVER_ERROR)
                .errorCode(UjfeErrorCode.UJFE_RENDER_ERROR.name())
                .errorType(errorType(exception))
                .source("render")
                .build());
            throw exception;
        }

        Duration duration = durationSince(start);
        RenderResult renderResult = new RenderResult(
            result.html(), result.css(), currentPath, duration, traceId,
            config.headNodes(), Map.of(), Map.of(), Map.of());
        runtimeActions.executeAfterRender(renderResult);
        emitRenderTrace(RenderTrace.builder()
            .traceId(traceId)
            .requestId(requestId(metadata))
            .route(currentPath)
            .httpMethod(method(metadata))
            .httpStatus(200)
            .adapterName(adapterName(metadata))
            .startedAt(start)
            .duration(duration)
            .responseSizeBytes(responseSize(result.html()))
            .status(TraceStatus.SUCCESS)
            .source("render")
            .build());

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
            emitEventTrace(EventTrace.builder()
                .traceId(traceId)
                .requestId(requestId(metadata))
                .route(currentPath)
                .eventId(eventId)
                .httpMethod(method(metadata))
                .httpStatus(403)
                .adapterName(adapterName(metadata))
                .startedAt(observability.clock()
                    .instant())
                .duration(Duration.ZERO)
                .responseSizeBytes(0)
                .status(TraceStatus.FORBIDDEN)
                .errorCode(UjfeErrorCode.UJFE_CSRF_VALIDATION_FAILED.name())
                .errorType(errorType(exception))
                .handlerFound(false)
                .handlerCompleted(false)
                .build());
            runtimeActions.executeOnError(new RuntimeErrorContext(
                exception, RuntimePhase.EVENT, currentPath, eventId, traceId,
                errorMetadata(UjfeErrorCode.UJFE_CSRF_VALIDATION_FAILED, 403, "csrf")));
            throw exception;
        }

        Instant start = Instant.now();
        Instant traceStart = observability.clock()
            .instant();

        ClientState eventClientState = clientState.mergeCookiesAndReplaceLocalStorage(filterClientState(nextClientState));

        LiveEventContext eventContext = new LiveEventContext(
            eventId, "live", this, Map.of(), eventClientState, null, Map.of(), traceId, Map.of());
        runtimeActions.executeBeforeEvent(eventContext);

        LiveRenderResult result;
        boolean handlerFound = eventRegistry.find(eventId)
            .isPresent();
        boolean handlerCompleted = false;
        Duration handlerDuration = Duration.ZERO;
        try {
            clientState = eventClientState;
            if (!handlerFound) {
                result = renderPathLocked(currentPath, eventId, metadata);
            } else {
                Instant handlerStart = observability.clock()
                    .instant();
                componentRenderer.handleWithClientState(clientState, () -> eventRegistry.handle(eventId, eventValue));
                handlerDuration = durationSince(handlerStart);
                handlerCompleted = true;
                result = renderPathLocked(currentPath, eventId, metadata);
            }
        } catch (Exception exception) {
            handlerDuration = durationSince(traceStart);
            emitEventTrace(EventTrace.builder()
                .traceId(traceId)
                .requestId(requestId(metadata))
                .route(currentPath)
                .eventId(eventId)
                .httpMethod(method(metadata))
                .httpStatus(500)
                .adapterName(adapterName(metadata))
                .startedAt(traceStart)
                .duration(handlerDuration)
                .responseSizeBytes(0)
                .status(TraceStatus.SERVER_ERROR)
                .errorCode(UjfeErrorCode.UJFE_EVENT_HANDLER_ERROR.name())
                .errorType(errorType(exception))
                .handlerFound(handlerFound)
                .handlerCompleted(false)
                .build());
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
        emitEventTrace(EventTrace.builder()
            .traceId(traceId)
            .requestId(requestId(metadata))
            .route(currentPath)
            .eventId(eventId)
            .httpMethod(method(metadata))
            .httpStatus(200)
            .adapterName(adapterName(metadata))
            .startedAt(traceStart)
            .duration(handlerDuration)
            .responseSizeBytes(responseSize(result.html()))
            .status(handlerFound ? TraceStatus.SUCCESS : TraceStatus.NOT_FOUND)
            .handlerFound(handlerFound)
            .handlerCompleted(handlerCompleted)
            .build());

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
        return renderPathLocked(currentPath, null, metadata);
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
        String effectiveTraceId = traceId == null || traceId.isBlank() ? nextTraceId() : traceId;
        runtimeActions.executeOnError(new RuntimeErrorContext(
            exception,
            phase,
            path,
            eventId,
            effectiveTraceId,
            Map.of(),
            Map.of(),
            requestMetadata,
            Map.of("sessionReference", Integer.toHexString(sessionId.hashCode())),
            runtimeMetadata
        ));
        emitHttpErrorTrace(phase, path, eventId, effectiveTraceId, requestMetadata, runtimeMetadata, exception);
    }

    public String renderDocument(String path, ClientState initialClientState) {
        return renderDocument(path, initialClientState, NO_HTTP_METADATA);
    }

    public String renderDocument(String path, ClientState initialClientState, LiveHttpRequestMetadata metadata) {
        writeLock.lock();
        try {
            mergeClientState(initialClientState);
            LiveRenderResult result = renderPathLocked(path, null, metadata);
            String document = "<!doctype html>"
                + "<html lang=\"" + AttributeEscaper.escape(config.lang()) + "\">"
                + "<head>"
                + "<meta charset=\"utf-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
                + "<title>" + HtmlEscaper.escape(config.title()) + "</title>"
                + renderHeadNodes()
                + renderExternalStylesheets()
                + renderActionHeadContributions()
                + renderInternalCss(result.css())
                + renderCsrfMetaTag()
                + "</head>"
                + "<body>"
                + "<div" + renderRootAttributes() + ">" + result.html() + "</div>"
                + "<script src=\"/_ujfe/client.js\"></script>"
                + (config.devToolsEnabled() ? "<script src=\"/_ujfe/dev.js\"></script>" : "")
                + "</body>"
                + "</html>";
            validateRenderedDocument(path, document);
            return document;
        } finally {
            writeLock.unlock();
        }
    }

    public void reportRouteNotFound(String path, LiveHttpRequestMetadata metadata) {
        String traceId = nextTraceId();
        Instant start = observability.clock()
            .instant();
        emitRenderTrace(RenderTrace.builder()
            .traceId(traceId)
            .requestId(requestId(metadata))
            .route(path)
            .httpMethod(method(metadata))
            .httpStatus(404)
            .adapterName(adapterName(metadata))
            .startedAt(start)
            .duration(Duration.ZERO)
            .responseSizeBytes(0)
            .status(TraceStatus.NOT_FOUND)
            .errorCode(UjfeErrorCode.UJFE_ROUTE_NOT_FOUND.name())
            .errorType("routing")
            .source("safe-error")
            .build());
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
        clientState = clientState.mergeCookiesAndReplaceLocalStorage(filterClientState(nextClientState));
    }

    private ClientState filterClientState(ClientState nextClientState) {
        return config.clientStatePolicy()
            .filter(nextClientState);
    }

    private Object pageFor(RouteDefinition route) {
        if (currentPage != null && route.path()
            .equals(currentPath)) {
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

    private void emitHttpErrorTrace(
        RuntimePhase phase,
        String path,
        String eventId,
        String traceId,
        Map<String, Object> requestMetadata,
        Map<String, Object> runtimeMetadata,
        Throwable exception
    ) {
        int httpStatus = intMetadata(runtimeMetadata, "httpStatus", 500);
        String errorCode = stringMetadata(runtimeMetadata, "errorCode");
        TraceStatus status = statusFor(httpStatus);
        Instant startedAt = observability.clock()
            .instant();
        if (phase == RuntimePhase.EVENT) {
            emitEventTrace(EventTrace.builder()
                .traceId(traceId)
                .requestId(stringMetadata(requestMetadata, "requestId"))
                .route(path)
                .eventId(eventId)
                .httpMethod(stringMetadata(requestMetadata, "method"))
                .httpStatus(httpStatus)
                .adapterName(stringMetadata(requestMetadata, "adapter"))
                .startedAt(startedAt)
                .duration(Duration.ZERO)
                .responseSizeBytes(0)
                .status(status)
                .errorCode(errorCode)
                .errorType(errorType(exception))
                .handlerFound(false)
                .handlerCompleted(false)
                .build());
            return;
        }
        if (phase == RuntimePhase.RENDER || phase == RuntimePhase.ROUTING) {
            emitRenderTrace(RenderTrace.builder()
                .traceId(traceId)
                .requestId(stringMetadata(requestMetadata, "requestId"))
                .route(path)
                .httpMethod(stringMetadata(requestMetadata, "method"))
                .httpStatus(httpStatus)
                .adapterName(stringMetadata(requestMetadata, "adapter"))
                .startedAt(startedAt)
                .duration(Duration.ZERO)
                .responseSizeBytes(0)
                .status(status)
                .errorCode(errorCode)
                .errorType(errorType(exception))
                .source("safe-error")
                .build());
        }
    }

    private void emitRenderTrace(RenderTrace trace) {
        if (!observability.renderTracesEnabled()) {
            return;
        }
        try {
            observability.traceSink()
                .onRenderTrace(trace);
        } catch (Exception exception) {
            System.err.println("UJFE: trace sink failed for render trace: " + exception.getMessage());
        }
        runtimeActions.executeRenderTrace(trace);
    }

    private void emitEventTrace(EventTrace trace) {
        if (!observability.eventTracesEnabled()) {
            return;
        }
        try {
            observability.traceSink()
                .onEventTrace(trace);
        } catch (Exception exception) {
            System.err.println("UJFE: trace sink failed for event trace: " + exception.getMessage());
        }
        runtimeActions.executeEventTrace(trace);
    }

    private Duration durationSince(Instant start) {
        return Duration.between(start, observability.clock()
            .instant());
    }

    private static long responseSize(String html) {
        return html == null ? 0 : html.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    }

    private static String adapterName(LiveHttpRequestMetadata metadata) {
        return metadata == null ? null : metadata.adapterName()
            .orElse(null);
    }

    private static String method(LiveHttpRequestMetadata metadata) {
        return metadata == null ? null : metadata.method()
            .orElse(null);
    }

    private static String requestId(LiveHttpRequestMetadata metadata) {
        return metadata == null ? null : metadata.requestId()
            .orElse(null);
    }

    private static String errorType(Throwable exception) {
        return exception == null ? null : exception.getClass()
            .getName();
    }

    private static TraceStatus statusFor(int httpStatus) {
        if (httpStatus == 404) {
            return TraceStatus.NOT_FOUND;
        }
        if (httpStatus == 403 || httpStatus == 401) {
            return TraceStatus.FORBIDDEN;
        }
        if (httpStatus == 429) {
            return TraceStatus.RATE_LIMITED;
        }
        if (httpStatus >= 400 && httpStatus < 500) {
            return TraceStatus.CLIENT_ERROR;
        }
        if (httpStatus >= 500) {
            return TraceStatus.SERVER_ERROR;
        }
        return TraceStatus.SUCCESS;
    }

    private static String stringMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata == null ? null : metadata.get(key);
        return value instanceof String && !((String) value).isBlank() ? (String) value : null;
    }

    private static int intMetadata(Map<String, Object> metadata, String key, int fallback) {
        Object value = metadata == null ? null : metadata.get(key);
        return value instanceof Number ? ((Number) value).intValue() : fallback;
    }

    private String renderHeadNodes() {
        if (config.headNodes()
            .isEmpty()) {
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
        if (config.cssMode() != CssMode.INTERNAL) {
            return "";
        }
        return "<style data-ujfe-css>" + css + "</style>";
    }

    private String renderExternalStylesheets() {
        if (config.cssMode() != CssMode.EXTERNAL || config.externalStylesheets()
            .isEmpty()) {
            return "";
        }
        UjfeContext context = UjfeContext.create();
        StringBuilder html = new StringBuilder();
        for (String href : config.externalStylesheets()) {
            html.append(link().attr("rel", "stylesheet")
                .attr("href", href)
                .render(context));
        }
        return html.toString();
    }

    private String renderCsrfMetaTag() {
        if (config.isCsrfProtectionDisabled()) {
            return "";
        }
        return "<meta name=\"ujfe-csrf-token\" content=\"" + HtmlEscaper.escape(csrfToken) + "\">";
    }

    private String renderRootAttributes() {
        return " id=\"ujfe-root\""
            + clientStatePolicyAttribute("data-ujfe-client-state-cookies", config.clientStatePolicy()
            .allowedCookies())
            + clientStatePolicyAttribute("data-ujfe-local-storage-keys", config.clientStatePolicy()
            .allowedLocalStorageKeys())
            + clientStatePolicyAttribute("data-ujfe-session-storage-keys", config.clientStatePolicy()
            .allowedSessionStorageKeys());
    }

    private static String clientStatePolicyAttribute(String name, Collection<String> values) {
        return " " + name + "=\"" + AttributeEscaper.escape(jsonStringArray(values)) + "\"";
    }

    private static String jsonStringArray(Collection<String> values) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (String value : values) {
            if (!first) {
                json.append(',');
            }
            json.append('"')
                .append(jsonEscape(value))
                .append('"');
            first = false;
        }
        return json.append(']')
            .toString();
    }

    private static String jsonEscape(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    escaped.append(current);
                    break;
            }
        }
        return escaped.toString();
    }

    private Map<String, Object> clientStateMetadata() {
        return Map.of(
            "cookies", clientState.cookies(),
            "localStorage", clientState.localStorage(),
            "sessionStorage", clientState.sessionStorage()
        );
    }

    private void validateRenderedDocument(String path, String document) {
        if (config.validationOptions()
            .mode() == ValidationMode.OFF) {
            return;
        }
        DocumentValidator validator = DocumentValidator.of(config.validationOptions());
        ValidationResult result = validator.validate(document);
        if (!result.hasFindings()) {
            return;
        }
        if (config.validationOptions()
            .mode() == ValidationMode.STRICT) {
            result.throwIfInvalid();
        }
        for (ValidationFinding finding : result.findings()) {
            VALIDATION_LOGGER.warning(() -> "event=ujfe.validation_finding"
                + " mode=" + config.validationOptions()
                .mode()
                + " rule=" + safeLogValue(finding.ruleId(), "unknown")
                + " severity=" + finding.severity()
                + " category=" + finding.category()
                .value()
                + " path=" + safeLogValue(path, "unknown")
                + optionalLogValue(" element=", finding.element())
                + optionalLogValue(" attribute=", finding.attribute())
                + optionalLogValue(" location=", finding.location())
                + " message=\"" + safeLogMessage(finding.message()) + "\"");
        }
    }

    private static String optionalLogValue(String prefix, String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return prefix + safeLogValue(value, "unknown");
    }

    private static String safeLogMessage(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replaceAll("[\\r\\n\\t]+", " ")
            .replace('"', '\'');
    }

    private static String safeLogValue(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.replaceAll("[^A-Za-z0-9_.:/#()=-]", "_");
    }

    private static String nextTraceId() {
        return UUID.randomUUID()
            .toString();
    }
}
