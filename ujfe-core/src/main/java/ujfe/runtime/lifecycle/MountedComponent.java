package ujfe.runtime.lifecycle;

import ujfe.core.Lifecycle;

import java.time.Instant;
import java.util.Objects;

public final class MountedComponent {
    private final Lifecycle lifecycle;
    private final long mountIndex;
    private final String path;
    private final String traceId;
    private final Instant mountedAt;
    private final LifecycleState state;

    MountedComponent(Lifecycle lifecycle, long mountIndex, MountContext context) {
        this(lifecycle, mountIndex, context.path(), context.traceId(), Instant.now(), LifecycleState.MOUNTED);
    }

    private MountedComponent(
            Lifecycle lifecycle,
            long mountIndex,
            String path,
            String traceId,
            Instant mountedAt,
            LifecycleState state
    ) {
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        this.mountIndex = mountIndex;
        this.path = path;
        this.traceId = Objects.requireNonNull(traceId, "traceId");
        this.mountedAt = Objects.requireNonNull(mountedAt, "mountedAt");
        this.state = Objects.requireNonNull(state, "state");
    }

    public Lifecycle lifecycle() {
        return lifecycle;
    }

    public long mountIndex() {
        return mountIndex;
    }

    public String path() {
        return path;
    }

    public String traceId() {
        return traceId;
    }

    public Instant mountedAt() {
        return mountedAt;
    }

    public LifecycleState state() {
        return state;
    }

    MountedComponent unmounted() {
        return new MountedComponent(lifecycle, mountIndex, path, traceId, mountedAt, LifecycleState.UNMOUNTED);
    }
}
