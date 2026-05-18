package ujfe.live;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class LiveEventRegistry {
    private final ConcurrentMap<String, LiveEventHandler> handlers = new ConcurrentHashMap<>();
    private final Map<String, String> eventIdsByRenderKey = new LinkedHashMap<>();
    private Set<String> activeRenderKeys;
    private String activeRenderScope;
    private int activeRenderIndex;

    public synchronized void beginRender(String scope) {
        if (activeRenderKeys != null) {
            throw new IllegalStateException("Live event render scope is already active");
        }
        activeRenderScope = scope == null || scope.isBlank() ? "default" : scope;
        activeRenderKeys = new LinkedHashSet<>();
        activeRenderIndex = 0;
    }

    public synchronized void completeRender() {
        if (activeRenderKeys == null) {
            return;
        }

        eventIdsByRenderKey.entrySet().removeIf(entry -> {
            boolean removed = !activeRenderKeys.contains(entry.getKey());
            if (removed) {
                handlers.remove(entry.getValue());
            }
            return removed;
        });
        clearRenderState();
    }

    public synchronized void abortRender() {
        clearRenderState();
    }

    public synchronized String register(Runnable handler) {
        Objects.requireNonNull(handler, "handler");
        LiveEventHandler liveHandler = handler::run;
        return register(liveHandler);
    }

    public synchronized String register(LiveEventHandler handler) {
        Objects.requireNonNull(handler, "handler");
        String eventId = eventIdForCurrentRenderPosition();
        handlers.put(eventId, handler);
        return eventId;
    }

    public Optional<LiveEventHandler> find(String eventId) {
        return Optional.ofNullable(handlers.get(eventId));
    }

    public void handle(String eventId) {
        LiveEventHandler handler = find(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown UJFE event: " + eventId));
        handler.handle();
    }

    public synchronized void clear() {
        handlers.clear();
        eventIdsByRenderKey.clear();
        clearRenderState();
    }

    private String eventIdForCurrentRenderPosition() {
        if (activeRenderKeys == null) {
            return nextEventId();
        }
        String renderKey = activeRenderScope + "#" + ++activeRenderIndex;
        activeRenderKeys.add(renderKey);
        return eventIdsByRenderKey.computeIfAbsent(renderKey, ignored -> nextEventId());
    }

    private void clearRenderState() {
        activeRenderKeys = null;
        activeRenderScope = null;
        activeRenderIndex = 0;
    }

    private static String nextEventId() {
        return "evt-" + UUID.randomUUID();
    }
}
