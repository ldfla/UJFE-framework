package ujfe.runtime.action;

import ujfe.core.ClientState;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable context passed to {@link BeforeRenderAction} before a page
 * or component is rendered.
 */
public final class RenderContext {
    private final String path;
    private final Object page;
    private final Object session;
    private final Map<String, Object> requestMetadata;
    private final ClientState clientState;
    private final Instant renderTimestamp;
    private final String traceId;
    private final Map<String, Object> runtimeMetadata;

    public RenderContext(
        String path,
        ClientState clientState,
        Instant renderTimestamp,
        String traceId,
        Map<String, Object> metadata
    ) {
        this(path, null, null, Map.of(), clientState, renderTimestamp, traceId, metadata);
    }

    public RenderContext(
        String path,
        Object page,
        Object session,
        Map<String, Object> requestMetadata,
        ClientState clientState,
        Instant renderTimestamp,
        String traceId,
        Map<String, Object> runtimeMetadata
    ) {
        this.path = Objects.requireNonNull(path, "path");
        this.page = page;
        this.session = session;
        this.requestMetadata = copy(requestMetadata);
        this.clientState = Objects.requireNonNull(clientState, "clientState");
        this.renderTimestamp = Objects.requireNonNull(renderTimestamp, "renderTimestamp");
        this.traceId = Objects.requireNonNull(traceId, "traceId");
        this.runtimeMetadata = copy(runtimeMetadata);
    }

    public String path() {
        return path;
    }

    public Object page() {
        return page;
    }

    public Object session() {
        return session;
    }

    public Map<String, Object> requestMetadata() {
        return requestMetadata;
    }

    public ClientState clientState() {
        return clientState;
    }

    public Instant renderTimestamp() {
        return renderTimestamp;
    }

    public String traceId() {
        return traceId;
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
