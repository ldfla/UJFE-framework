package ujfe.signals;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Computed<T> implements Signal<T>, SignalDependency {
    private static final ThreadLocal<ArrayDeque<Computed<?>>> EVALUATION_STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    private final Supplier<T> supplier;
    private final Object lock = new Object();
    private final CopyOnWriteArrayList<Consumer<T>> listeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Runnable> invalidationListeners = new CopyOnWriteArrayList<>();
    private final IdentityHashMap<SignalDependency, AutoCloseable> dependencySubscriptions = new IdentityHashMap<>();

    private boolean valid;
    private boolean hasCachedValue;
    private T cachedValue;

    public Computed(Supplier<T> supplier) {
        this.supplier = Objects.requireNonNull(supplier, "supplier");
    }

    @Override
    public T get() {
        DependencyTracker.record(this);
        List<Consumer<T>> listenersToNotify = List.of();
        T result;

        synchronized (lock) {
            if (valid) {
                return cachedValue;
            }

            ArrayDeque<Computed<?>> stack = EVALUATION_STACK.get();
            if (stack.contains(this)) {
                throw new ComputedCycleException("Circular computed dependency detected");
            }

            stack.push(this);
            DependencyTracker.Evaluation<T> evaluation;
            try {
                evaluation = DependencyTracker.collect(supplier);
            } finally {
                stack.pop();
                if (stack.isEmpty()) {
                    EVALUATION_STACK.remove();
                }
            }

            T next = evaluation.value();
            boolean shouldNotify = hasCachedValue && !Objects.equals(cachedValue, next);
            replaceDependencies(evaluation.dependencies());
            cachedValue = next;
            hasCachedValue = true;
            valid = true;
            result = next;
            if (shouldNotify) {
                listenersToNotify = List.copyOf(listeners);
            }
        }

        notifyListeners(listenersToNotify, result);
        return result;
    }

    @Override
    public void set(T value) {
        throw new UnsupportedOperationException("Computed signals are read-only");
    }

    @Override
    public void update(Function<T, T> updater) {
        throw new UnsupportedOperationException("Computed signals are read-only");
    }

    @Override
    public AutoCloseable subscribe(Consumer<T> listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    @Override
    public AutoCloseable subscribeInvalidation(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        invalidationListeners.add(listener);
        return () -> invalidationListeners.remove(listener);
    }

    private void invalidate() {
        List<Runnable> listenersToNotify;
        synchronized (lock) {
            if (!valid) {
                return;
            }
            valid = false;
            listenersToNotify = List.copyOf(invalidationListeners);
        }

        for (Runnable listener : listenersToNotify) {
            listener.run();
        }
    }

    private void replaceDependencies(List<SignalDependency> nextDependencies) {
        List<SignalDependency> toRemove = new ArrayList<>();
        for (SignalDependency dependency : dependencySubscriptions.keySet()) {
            if (!containsIdentity(nextDependencies, dependency)) {
                toRemove.add(dependency);
            }
        }

        for (SignalDependency dependency : toRemove) {
            closeSubscription(dependencySubscriptions.remove(dependency));
        }

        for (SignalDependency dependency : nextDependencies) {
            if (!dependencySubscriptions.containsKey(dependency)) {
                dependencySubscriptions.put(dependency, dependency.subscribeInvalidation(this::invalidate));
            }
        }
    }

    private static boolean containsIdentity(List<SignalDependency> dependencies, SignalDependency target) {
        for (SignalDependency dependency : dependencies) {
            if (dependency == target) {
                return true;
            }
        }
        return false;
    }

    private static void closeSubscription(AutoCloseable subscription) {
        if (subscription == null) {
            return;
        }
        try {
            subscription.close();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to remove computed dependency subscription", exception);
        }
    }

    private static <T> void notifyListeners(List<Consumer<T>> listeners, T value) {
        for (Consumer<T> listener : listeners) {
            listener.accept(value);
        }
    }
}
