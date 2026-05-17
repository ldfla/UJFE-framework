package ujfe.live;

import ujfe.core.ClientState;

import java.util.Objects;

public final class LiveHttpEventPayload {
    private final String eventId;
    private final ClientState clientState;

    public LiveHttpEventPayload(String eventId, ClientState clientState) {
        this.eventId = requireText(eventId, "eventId");
        this.clientState = Objects.requireNonNull(clientState, "clientState");
    }

    public String eventId() {
        return eventId;
    }

    public ClientState clientState() {
        return clientState;
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
