package ujfe.runtime.lifecycle;

import ujfe.core.Lifecycle;

import java.util.*;

public final class LifecycleTracker {
    private final LifecycleRegistry registry;
    private final LifecycleContext context;
    private final List<Lifecycle> observed = new ArrayList<>();
    private final Set<Lifecycle> observedSet = Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean completed;

    LifecycleTracker(LifecycleRegistry registry, LifecycleContext context) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.context = Objects.requireNonNull(context, "context");
    }

    public synchronized void track(Object candidate) {
        ensureOpen();
        if (!(candidate instanceof Lifecycle)) {
            return;
        }

        Lifecycle lifecycle = (Lifecycle) candidate;
        if (observedSet.add(lifecycle)) {
            observed.add(lifecycle);
        }
    }

    public synchronized List<LifecycleException> complete() {
        ensureOpen();
        completed = true;
        return registry.reconcile(
            new MountContext(context),
            new UnmountContext(context, "render-reconcile"),
            List.copyOf(observed)
        );
    }

    public synchronized void abort() {
        completed = true;
        observed.clear();
        observedSet.clear();
    }

    public LifecycleContext context() {
        return context;
    }

    private void ensureOpen() {
        if (completed) {
            throw new IllegalStateException("Lifecycle tracker is already completed");
        }
    }
}
