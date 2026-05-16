package ujfe.runtime.action;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable context passed to {@link ErrorAction} when a runtime error
 * occurs.
 *
 * <p>The context does not expose sensitive server information to clients
 * automatically. The exception and metadata are available for server-side
 * logging and observability only.</p>
 */
public final class RuntimeErrorContext {
    private final Throwable exception;
    private final RuntimePhase phase;
    private final String path;
    private final String eventId;
    private final String traceId;
    private final Map<String, Object> routeMetadata;
    private final Map<String, Object> eventMetadata;
    private final Map<String, Object> requestMetadata;
    private final Map<String, Object> sessionMetadata;
    private final Map<String, Object> runtimeMetadata;

    public RuntimeErrorContext(
            Throwable exception,
            RuntimePhase phase,
            String path,
            String eventId,
            String traceId,
            Map<String, Object> metadata
    ) {
        this(exception, phase, path, eventId, traceId, Map.of(), Map.of(), Map.of(), Map.of(), metadata);
    }

    public RuntimeErrorContext(
            Throwable exception,
            RuntimePhase phase,
            String path,
            String eventId,
            String traceId,
            Map<String, Object> routeMetadata,
            Map<String, Object> eventMetadata,
            Map<String, Object> requestMetadata,
            Map<String, Object> sessionMetadata,
            Map<String, Object> runtimeMetadata
    ) {
        this.exception = Objects.requireNonNull(exception, "exception");
        this.phase = Objects.requireNonNull(phase, "phase");
        this.path = path;
        this.eventId = eventId;
        this.traceId = Objects.requireNonNull(traceId, "traceId");
        this.routeMetadata = copy(routeMetadata);
        this.eventMetadata = copy(eventMetadata);
        this.requestMetadata = copy(requestMetadata);
        this.sessionMetadata = copy(sessionMetadata);
        this.runtimeMetadata = copy(runtimeMetadata);
    }

    public Throwable exception() {
        return exception;
    }

    public RuntimePhase phase() {
        return phase;
    }

    /**
     * Returns the route path, or {@code null} if not applicable.
     */
    public String path() {
        return path;
    }

    /**
     * Returns the event ID, or {@code null} if the error occurred outside
     * event processing.
     */
    public String eventId() {
        return eventId;
    }

    public String traceId() {
        return traceId;
    }

    public Map<String, Object> routeMetadata() {
        return routeMetadata;
    }

    public Map<String, Object> eventMetadata() {
        return eventMetadata;
    }

    public Map<String, Object> requestMetadata() {
        return requestMetadata;
    }

    public Map<String, Object> sessionMetadata() {
        return sessionMetadata;
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
