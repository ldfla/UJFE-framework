package ujfe.live;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class LiveEventRegistry {
    private final ConcurrentMap<String, LiveEventHandler> handlers = new ConcurrentHashMap<>();

    public String register(Runnable handler) {
        Objects.requireNonNull(handler, "handler");
        String eventId = nextEventId();
        handlers.put(eventId, handler::run);
        return eventId;
    }

    public String register(LiveEventHandler handler) {
        Objects.requireNonNull(handler, "handler");
        String eventId = nextEventId();
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

    public void clear() {
        handlers.clear();
    }

    private static String nextEventId() {
        return "evt-" + UUID.randomUUID();
    }
}
