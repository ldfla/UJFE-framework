package ujfe.signals;

public final class Signals {
    private Signals() {
    }

    public static <T> Signal<T> signal(T value) {
        return new MutableSignal<>(value);
    }

    public static <T> Computed<T> computed(java.util.function.Supplier<T> supplier) {
        return new Computed<>(supplier);
    }
}
