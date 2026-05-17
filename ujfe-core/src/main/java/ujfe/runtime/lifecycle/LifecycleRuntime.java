package ujfe.runtime.lifecycle;

import ujfe.core.Lifecycle;

import java.util.List;
import java.util.Objects;

public final class LifecycleRuntime {
    private final LifecycleRegistry registry;

    public LifecycleRuntime() {
        this(new LifecycleRegistry());
    }

    public LifecycleRuntime(LifecycleRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public static LifecycleRuntime create() {
        return new LifecycleRuntime();
    }

    public LifecycleTracker beginRender(LifecycleContext context) {
        return new LifecycleTracker(registry, context);
    }

    public List<LifecycleException> cleanup(UnmountContext context) {
        return registry.unmountAll(context);
    }

    public boolean isMounted(Lifecycle lifecycle) {
        return registry.isMounted(lifecycle);
    }

    public LifecycleRegistry registry() {
        return registry;
    }
}
