package ujfe.signals;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

public final class MutableSignal<T> implements Signal<T> {
    private final AtomicReference<T> value;
    private final CopyOnWriteArrayList<Consumer<T>> listeners;

    public MutableSignal(T initialValue) {
        this.value = new AtomicReference<>(initialValue);
        this.listeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public T get() {
        return value.get();
    }

    @Override
    public void set(T value) {
        T previous = this.value.getAndSet(value);
        if (!Objects.equals(previous, value)) {
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

    private void notifyListeners(T next) {
        for (Consumer<T> listener : listeners) {
            listener.accept(next);
        }
    }
}
