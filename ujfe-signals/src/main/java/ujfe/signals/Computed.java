package ujfe.signals;

import java.util.Objects;
import java.util.function.Supplier;

public final class Computed<T> {
    private final Supplier<T> supplier;

    public Computed(Supplier<T> supplier) {
        this.supplier = Objects.requireNonNull(supplier, "supplier");
    }

    public T get() {
        return supplier.get();
    }
}
