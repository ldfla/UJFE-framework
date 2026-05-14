package ujfe.core;

public interface Lifecycle {
    default void onMount() {
    }

    default void onUnmount() {
    }
}
