package ujfe.signals;

import java.util.function.Consumer;
import java.util.function.Function;

public interface Signal<T> {
    T get();

    void set(T value);

    void update(Function<T, T> updater);

    AutoCloseable subscribe(Consumer<T> listener);
}
