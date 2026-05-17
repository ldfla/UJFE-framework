package ujfe.runtime.lifecycle;

import java.time.Instant;
import java.util.Objects;

public final class UnmountEvent {
    private final MountedComponent component;
    private final UnmountContext context;
    private final Instant timestamp;

    public UnmountEvent(MountedComponent component, UnmountContext context) {
        this.component = Objects.requireNonNull(component, "component");
        this.context = Objects.requireNonNull(context, "context");
        this.timestamp = Instant.now();
    }

    public MountedComponent component() {
        return component;
    }

    public UnmountContext context() {
        return context;
    }

    public Instant timestamp() {
        return timestamp;
    }
}
