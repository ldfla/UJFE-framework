package ujfe.runtime.lifecycle;

import ujfe.core.Lifecycle;

import java.util.*;

public final class LifecycleRegistry {
    private final Map<Lifecycle, MountedComponent> mounted = new IdentityHashMap<>();
    private final List<Lifecycle> mountOrder = new ArrayList<>();
    private long nextMountIndex = 0;

    public synchronized boolean isMounted(Lifecycle lifecycle) {
        return mounted.containsKey(Objects.requireNonNull(lifecycle, "lifecycle"));
    }

    public synchronized int size() {
        return mounted.size();
    }

    public synchronized boolean isEmpty() {
        return mounted.isEmpty();
    }

    public synchronized List<MountedComponent> mountedComponents() {
        List<MountedComponent> components = new ArrayList<>();
        for (Lifecycle lifecycle : mountOrder) {
            MountedComponent component = mounted.get(lifecycle);
            if (component != null) {
                components.add(component);
            }
        }
        return List.copyOf(components);
    }

    synchronized List<LifecycleException> reconcile(
            MountContext mountContext,
            UnmountContext unmountContext,
            List<Lifecycle> observed
    ) {
        Objects.requireNonNull(mountContext, "mountContext");
        Objects.requireNonNull(unmountContext, "unmountContext");
        Objects.requireNonNull(observed, "observed");

        List<LifecycleException> failures = new ArrayList<>();
        unmountRemoved(observed, unmountContext, failures);
        mountNew(observed, mountContext);
        return List.copyOf(failures);
    }

    synchronized List<LifecycleException> unmountAll(UnmountContext context) {
        Objects.requireNonNull(context, "context");
        List<LifecycleException> failures = new ArrayList<>();
        List<Lifecycle> lifecycles = new ArrayList<>(mountOrder);
        for (int index = lifecycles.size() - 1; index >= 0; index--) {
            unmount(lifecycles.get(index), context, failures);
        }
        return List.copyOf(failures);
    }

    private void mountNew(List<Lifecycle> observed, MountContext context) {
        List<Lifecycle> mountedThisCycle = new ArrayList<>();
        for (Lifecycle lifecycle : observed) {
            if (mounted.containsKey(lifecycle)) {
                continue;
            }

            try {
                lifecycle.onMount();
                mounted.put(lifecycle, new MountedComponent(lifecycle, nextMountIndex++, context));
                mountOrder.add(lifecycle);
                mountedThisCycle.add(lifecycle);
            } catch (Exception exception) {
                LifecycleException failure = new LifecycleException(
                        "Lifecycle onMount failed", lifecycle, "onMount", exception, context);
                rollbackMountedThisCycle(mountedThisCycle, new UnmountContext(context, "mount-failure"), failure);
                throw failure;
            }
        }
    }

    private void unmountRemoved(
            List<Lifecycle> observed,
            UnmountContext context,
            List<LifecycleException> failures
    ) {
        List<Lifecycle> lifecycles = new ArrayList<>(mountOrder);
        for (int index = lifecycles.size() - 1; index >= 0; index--) {
            Lifecycle lifecycle = lifecycles.get(index);
            if (!containsIdentity(observed, lifecycle)) {
                unmount(lifecycle, context, failures);
            }
        }
    }

    private void rollbackMountedThisCycle(
            List<Lifecycle> mountedThisCycle,
            UnmountContext context,
            LifecycleException failure
    ) {
        List<LifecycleException> cleanupFailures = new ArrayList<>();
        for (int index = mountedThisCycle.size() - 1; index >= 0; index--) {
            unmount(mountedThisCycle.get(index), context, cleanupFailures);
        }
        cleanupFailures.forEach(failure::addSuppressed);
    }

    private void unmount(
            Lifecycle lifecycle,
            UnmountContext context,
            List<LifecycleException> failures
    ) {
        MountedComponent component = mounted.remove(lifecycle);
        removeFromMountOrder(lifecycle);
        if (component == null) {
            return;
        }

        try {
            lifecycle.onUnmount();
        } catch (Exception exception) {
            failures.add(new LifecycleException(
                    "Lifecycle onUnmount failed", lifecycle, "onUnmount", exception, context));
        }
    }

    private void removeFromMountOrder(Lifecycle lifecycle) {
        for (int index = 0; index < mountOrder.size(); index++) {
            if (mountOrder.get(index) == lifecycle) {
                mountOrder.remove(index);
                return;
            }
        }
    }

    private static boolean containsIdentity(List<Lifecycle> lifecycles, Lifecycle target) {
        for (Lifecycle lifecycle : lifecycles) {
            if (lifecycle == target) {
                return true;
            }
        }
        return false;
    }
}
