package ujfe.runtime.lifecycle;

import java.time.Instant;
import java.util.Objects;

public final class MountEvent {
    private final MountedComponent component;
    private final MountContext context;
    private final Instant timestamp;

    public MountEvent(MountedComponent component, MountContext context) {
        this.component = Objects.requireNonNull(component, "component");
        this.context = Objects.requireNonNull(context, "context");
        this.timestamp = Instant.now();
    }

    public MountedComponent component() {
        return component;
    }

    public MountContext context() {
        return context;
    }

    public Instant timestamp() {
        return timestamp;
    }
}
