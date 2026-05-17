package ujfe.runtime.action;

import ujfe.core.Node;

import java.time.Duration;
import java.util.*;

/**
 * Immutable result passed to {@link AfterRenderAction} after a page
 * or component has been rendered.
 */
public final class RenderResult {
    private final String html;
    private final String css;
    private final String path;
    private final Duration renderDuration;
    private final String traceId;
    private final List<Node> headNodes;
    private final Map<String, Object> routeMetadata;
    private final Map<String, Object> cssMetadata;
    private final Map<String, Object> runtimeMetadata;

    public RenderResult(
            String html,
            String css,
            String path,
            Duration renderDuration,
            String traceId,
            Map<String, Object> metadata
    ) {
        this(html, css, path, renderDuration, traceId, List.of(), Map.of(), Map.of(), metadata);
    }

    public RenderResult(
            String html,
            String css,
            String path,
            Duration renderDuration,
            String traceId,
            List<Node> headNodes,
            Map<String, Object> routeMetadata,
            Map<String, Object> cssMetadata,
            Map<String, Object> runtimeMetadata
    ) {
        this.html = Objects.requireNonNull(html, "html");
        this.css = Objects.requireNonNull(css, "css");
        this.path = Objects.requireNonNull(path, "path");
        this.renderDuration = Objects.requireNonNull(renderDuration, "renderDuration");
        this.traceId = Objects.requireNonNull(traceId, "traceId");
        this.headNodes = headNodes == null || headNodes.isEmpty() ? List.of() : List.copyOf(headNodes);
        this.routeMetadata = copy(routeMetadata);
        this.cssMetadata = copy(cssMetadata);
        this.runtimeMetadata = copy(runtimeMetadata);
    }

    public String html() {
        return html;
    }

    public String css() {
        return css;
    }

    public String path() {
        return path;
    }

    public Duration renderDuration() {
        return renderDuration;
    }

    public String traceId() {
        return traceId;
    }

    public List<Node> headNodes() {
        return headNodes;
    }

    public Map<String, Object> routeMetadata() {
        return routeMetadata;
    }

    public Map<String, Object> cssMetadata() {
        return cssMetadata;
    }

    public Map<String, Object> runtimeMetadata() {
        return runtimeMetadata;
    }

    public Map<String, Object> metadata() {
        return runtimeMetadata;
    }

    private static Map<String, Object> copy(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
