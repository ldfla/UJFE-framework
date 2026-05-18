package ujfe.runtime.action;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable result passed to {@link AfterEventAction} after a live
 * event has been dispatched and the page re-rendered.
 */
public final class LiveEventResult {
    private final String html;
    private final String eventId;
    private final Duration eventDuration;
    private final String traceId;
    private final Map<String, Object> eventMetadata;
    private final Map<String, Object> reRenderMetadata;
    private final Map<String, Object> clientStateMetadata;
    private final Map<String, Object> runtimeMetadata;

    public LiveEventResult(
        String html,
        String eventId,
        Duration eventDuration,
        String traceId,
        Map<String, Object> metadata
    ) {
        this(html, eventId, eventDuration, traceId, metadata, Map.of(), Map.of(), Map.of());
    }

    public LiveEventResult(
        String html,
        String eventId,
        Duration eventDuration,
        String traceId,
        Map<String, Object> eventMetadata,
        Map<String, Object> reRenderMetadata,
        Map<String, Object> clientStateMetadata,
        Map<String, Object> runtimeMetadata
    ) {
        this.html = Objects.requireNonNull(html, "html");
        this.eventId = Objects.requireNonNull(eventId, "eventId");
        this.eventDuration = Objects.requireNonNull(eventDuration, "eventDuration");
        this.traceId = Objects.requireNonNull(traceId, "traceId");
        this.eventMetadata = copy(eventMetadata);
        this.reRenderMetadata = copy(reRenderMetadata);
        this.clientStateMetadata = copy(clientStateMetadata);
        this.runtimeMetadata = copy(runtimeMetadata);
    }

    public String html() {
        return html;
    }

    public String eventId() {
        return eventId;
    }

    public Duration eventDuration() {
        return eventDuration;
    }

    public String traceId() {
        return traceId;
    }

    public Map<String, Object> eventMetadata() {
        return eventMetadata;
    }

    public Map<String, Object> reRenderMetadata() {
        return reRenderMetadata;
    }

    public Map<String, Object> clientStateMetadata() {
        return clientStateMetadata;
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
