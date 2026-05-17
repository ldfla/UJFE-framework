package ujfe.signals;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

public final class MutableSignal<T> implements Signal<T>, SignalDependency {
    private final AtomicReference<T> value;
    private final CopyOnWriteArrayList<Consumer<T>> listeners;
    private final CopyOnWriteArrayList<Runnable> invalidationListeners;

    public MutableSignal(T initialValue) {
        this.value = new AtomicReference<>(initialValue);
        this.listeners = new CopyOnWriteArrayList<>();
        this.invalidationListeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public T get() {
        DependencyTracker.record(this);
        return value.get();
    }

    @Override
    public void set(T value) {
        T previous = this.value.getAndSet(value);
        if (!Objects.equals(previous, value)) {
            notifyInvalidationListeners();
            notifyListeners(value);
        }
    }

    @Override
    public void update(Function<T, T> updater) {
        Objects.requireNonNull(updater, "updater");
        while (true) {
            T current = value.get();
            T next = updater.apply(current);
            if (value.compareAndSet(current, next)) {
                if (!Objects.equals(current, next)) {
                    notifyInvalidationListeners();
                    notifyListeners(next);
                }
                return;
            }
        }
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

    private void notifyInvalidationListeners() {
        for (Runnable listener : invalidationListeners) {
            listener.run();
        }
    }

    private void notifyListeners(T next) {
        for (Consumer<T> listener : listeners) {
            listener.accept(next);
        }
    }
}
