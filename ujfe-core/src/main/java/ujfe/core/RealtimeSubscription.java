package ujfe.core;

import java.time.Duration;
import java.util.Objects;

/**
 * Declarative realtime subscription marker for SSE/WebSocket/polling bridges.
 */
public final class RealtimeSubscription implements Node {
    private final String endpoint;
    private Class<?> eventType;
    private Duration heartbeat = Duration.ofSeconds(30);
    private Duration reconnectDelay = Duration.ofSeconds(2);
    private Duration debounce = Duration.ZERO;
    private Duration pollingFallback;
    private String connectionStateTarget;
    private String payloadTarget;

    RealtimeSubscription(String endpoint) {
        this.endpoint = RouteBuilder.validateContinueUrl(endpoint);
    }

    public RealtimeSubscription onEvent(Class<?> eventType) {
        this.eventType = Objects.requireNonNull(eventType, "eventType");
        return this;
    }

    public RealtimeSubscription heartbeat(Duration heartbeat) {
        this.heartbeat = nonNegative(heartbeat, "heartbeat");
        return this;
    }

    public RealtimeSubscription reconnectDelay(Duration reconnectDelay) {
        this.reconnectDelay = nonNegative(reconnectDelay, "reconnectDelay");
        return this;
    }

    public RealtimeSubscription debounce(Duration debounce) {
        this.debounce = nonNegative(debounce, "debounce");
        return this;
    }

    public RealtimeSubscription pollingFallback(Duration interval) {
        this.pollingFallback = nonNegative(interval, "interval");
        return this;
    }

    public RealtimeSubscription connectionStateTarget(String selector) {
        this.connectionStateTarget = requireText(selector, "selector");
        return this;
    }

    public RealtimeSubscription payloadTarget(String selector) {
        this.payloadTarget = requireText(selector, "selector");
        return this;
    }

    @Override
    public String render(UjfeContext context) {
        Objects.requireNonNull(context, "context");
        Element marker = UI.div()
            .data("ujfe-realtime", "true")
            .data("ujfe-realtime-endpoint", endpoint)
            .data("ujfe-heartbeat-ms", Long.toString(heartbeat.toMillis()))
            .data("ujfe-reconnect-delay-ms", Long.toString(reconnectDelay.toMillis()))
            .data("ujfe-debounce-ms", Long.toString(debounce.toMillis()))
            .role("status")
            .aria("live", "polite")
            .child(UI.span("Connecting"));
        if (eventType != null) {
            marker.data("ujfe-event-type", eventType.getName());
        }
        if (pollingFallback != null) {
            marker.data("ujfe-polling-fallback-ms", Long.toString(pollingFallback.toMillis()));
        }
        if (connectionStateTarget != null) {
            marker.data("ujfe-connection-state-target", connectionStateTarget);
        }
        if (payloadTarget != null) {
            marker.data("ujfe-payload-target", payloadTarget);
        }
        return marker.render(context);
    }

    private static Duration nonNegative(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isNegative()) {
            throw new IllegalArgumentException(name + " cannot be negative");
        }
        return value;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value.trim();
    }
}
