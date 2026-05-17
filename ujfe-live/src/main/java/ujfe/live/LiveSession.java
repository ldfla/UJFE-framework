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

public final class LiveSession implements AutoCloseable {
    private final Router router;
    private final PageRenderer pageRenderer;
    private final LiveEventRegistry eventRegistry;
    private final LiveComponentRenderer componentRenderer;
    private final LiveSessionConfig config;
    private final RuntimeActionRegistry runtimeActions;
    private final LifecycleRuntime lifecycleRuntime;
    private final String csrfToken;
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
    }

    public String csrfToken() {
        return csrfToken;
    }

    public synchronized LiveRenderResult renderPath(String path) {
        return renderPath(path, null);
    }

    private LiveRenderResult renderPath(String path, String eventId) {
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
                    exception, RuntimePhase.RENDER, path, null, traceId, null));
            throw exception;
        }

        RenderContext renderContext = new RenderContext(
                route.path(), page, this, Map.of(), clientState, start, traceId, Map.of());
        runtimeActions.executeBeforeRender(renderContext);

        LiveRenderResult result;
        LifecycleTracker lifecycleTracker = lifecycleRuntime.beginRender(new LifecycleContext(
                route.path(), this, traceId, Map.of()));
        try {
            result = componentRenderer.render(() -> pageRenderer.render(page), clientState, lifecycleTracker);
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

    public synchronized LiveRenderResult handleEvent(String eventId, ClientState nextClientState) {
        return handleEvent(eventId, nextClientState, new LiveHttpRequestMetadata(null, null, null, null));
    }

    public synchronized LiveRenderResult handleEvent(String eventId, ClientState nextClientState, LiveHttpRequestMetadata metadata) {
        LiveHttpSecurity.validateCsrf(config, csrfToken, metadata);

        String traceId = nextTraceId();
        Instant start = Instant.now();

        ClientState eventClientState = clientState.mergeCookiesAndReplaceLocalStorage(nextClientState);

        LiveEventContext eventContext = new LiveEventContext(
                eventId, "live", this, Map.of(), eventClientState, null, Map.of(), traceId, Map.of());
        runtimeActions.executeBeforeEvent(eventContext);

        LiveRenderResult result;
        try {
            clientState = eventClientState;
            componentRenderer.handleWithClientState(clientState, () -> eventRegistry.handle(eventId));
            result = renderPath(currentPath, eventId);
        } catch (Exception exception) {
            runtimeActions.executeOnError(new RuntimeErrorContext(
                    exception, RuntimePhase.EVENT, currentPath, eventId, traceId, null));
            throw exception;
        }

        Duration duration = Duration.between(start, Instant.now());
        LiveEventResult eventResult = new LiveEventResult(
                result.html(), eventId, duration, traceId,
                Map.of("eventType", "live"), Map.of("path", currentPath), clientStateMetadata(), Map.of());
        runtimeActions.executeAfterEvent(eventResult);

        return result;
    }

    public synchronized LiveRenderResult updateClientState(ClientState nextClientState) {
        return updateClientState(nextClientState, new LiveHttpRequestMetadata(null, null, null, null));
    }

    public synchronized LiveRenderResult updateClientState(ClientState nextClientState, LiveHttpRequestMetadata metadata) {
        LiveHttpSecurity.validateCsrf(config, csrfToken, metadata);
        mergeClientState(nextClientState);
        return renderPath(currentPath);
    }

    public synchronized String renderDocument(String path, ClientState initialClientState) {
        mergeClientState(initialClientState);
        LiveRenderResult result = renderPath(path);
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
    }

    public synchronized String renderCss(Collection<String> classes) {
        return componentRenderer.renderCss(classes);
    }

    @Override
    public synchronized void close() {
        String traceId = nextTraceId();
        List<LifecycleException> failures = lifecycleRuntime.cleanup(
                new UnmountContext(currentPath, this, traceId, Map.of(), "session-close"));
        routeLifecycleFailures(failures, currentPath, null, traceId);
        eventRegistry.clear();
        currentPage = null;
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
                exception, RuntimePhase.RENDER, path, null, traceId, null));
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
