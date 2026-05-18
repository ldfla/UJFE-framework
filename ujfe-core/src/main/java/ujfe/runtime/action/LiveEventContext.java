package ujfe.runtime.action;

import ujfe.core.ClientState;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable context passed to {@link BeforeEventAction} before a live
 * event is dispatched.
 */
public final class LiveEventContext {
    private final String eventId;
    private final String eventType;
    private final Object session;
    private final Map<String, Object> requestMetadata;
    private final ClientState clientState;
    private final Object target;
    private final Map<String, String> submittedValues;
    private final String traceId;
    private final Map<String, Object> runtimeMetadata;

    public LiveEventContext(
        String eventId,
        ClientState clientState,
        String traceId,
        Map<String, Object> metadata
    ) {
        this(eventId, "", null, Map.of(), clientState, null, Map.of(), traceId, metadata);
    }

    public LiveEventContext(
        String eventId,
        String eventType,
        Object session,
        Map<String, Object> requestMetadata,
        ClientState clientState,
        Object target,
        Map<String, String> submittedValues,
        String traceId,
        Map<String, Object> runtimeMetadata
    ) {
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.eventType = eventType == null ? "" : eventType;
        this.session = session;
        this.requestMetadata = copy(requestMetadata);
        this.clientState = Objects.requireNonNull(clientState, "clientState");
        this.target = target;
        this.submittedValues = copyStrings(submittedValues);
        this.traceId = Objects.requireNonNull(traceId, "traceId");
        this.runtimeMetadata = copy(runtimeMetadata);
    }

    public String eventId() {
        return eventId;
    }

    public String eventType() {
        return eventType;
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

    public Object target() {
        return target;
    }

    public Map<String, String> submittedValues() {
        return submittedValues;
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

    private static Map<String, String> copyStrings(Map<String, String> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
