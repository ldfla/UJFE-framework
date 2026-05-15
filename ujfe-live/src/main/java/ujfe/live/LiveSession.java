package ujfe.live;

import ujfe.core.AttributeEscaper;
import ujfe.core.ClientState;
import ujfe.core.ElementIdGenerator;
import ujfe.core.HtmlEscaper;
import ujfe.core.Node;
import ujfe.core.UjfeContext;
import ujfe.html.CssTheme;
import ujfe.router.PageRenderer;
import ujfe.router.RouteDefinition;
import ujfe.router.Router;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

public final class LiveSession {
    private final Router router;
    private final PageRenderer pageRenderer;
    private final LiveEventRegistry eventRegistry;
    private final LiveComponentRenderer componentRenderer;
    private final LiveSessionConfig config;
    private String currentPath = "/";
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
        this.componentRenderer = new LiveComponentRenderer(
                eventRegistry,
                ElementIdGenerator.sequential(),
                config.themeSupplier(),
                config.cssMode()
        );
    }

    public synchronized LiveRenderResult renderPath(String path) {
        RouteDefinition route = router.resolve(path)
                .orElseThrow(() -> new IllegalArgumentException("No UJFE route registered for " + path));
        LiveRenderResult result = componentRenderer.render(() -> pageRenderer.render(route), clientState);
        currentPath = route.path();
        return result;
    }

    public synchronized LiveRenderResult handleEvent(String eventId, ClientState nextClientState) {
        mergeClientState(nextClientState);
        componentRenderer.handleWithClientState(clientState, () -> eventRegistry.handle(eventId));
        return renderPath(currentPath);
    }

    public synchronized LiveRenderResult updateClientState(ClientState nextClientState) {
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
                + renderInternalCss(result.css())
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

    private void mergeClientState(ClientState nextClientState) {
        clientState = clientState.mergeCookiesAndReplaceLocalStorage(nextClientState);
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

    private String renderInternalCss(String css) {
        if (config.cssMode() == CssMode.EXTERNAL) {
            return "";
        }
        return "<style data-ujfe-css>" + css + "</style>";
    }
}
