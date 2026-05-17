package ujfe.runtime.lifecycle;

import ujfe.core.Lifecycle;

import java.util.Objects;
import java.util.Optional;

public final class LifecycleException extends RuntimeException {
    private final Lifecycle lifecycle;
    private final String callback;
    private final LifecycleContext context;

    public LifecycleException(String message, Lifecycle lifecycle, String callback, Throwable cause) {
        this(message, lifecycle, callback, cause, null);
    }

    public LifecycleException(
            String message,
            Lifecycle lifecycle,
            String callback,
            Throwable cause,
            LifecycleContext context
    ) {
        super(message, cause);
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        this.callback = Objects.requireNonNull(callback, "callback");
        this.context = context;
    }

    public Lifecycle lifecycle() {
        return lifecycle;
    }

    public String callback() {
        return callback;
    }

    public Optional<LifecycleContext> context() {
        return Optional.ofNullable(context);
    }
}
