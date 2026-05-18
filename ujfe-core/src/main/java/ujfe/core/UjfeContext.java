package ujfe.core;

import ujfe.runtime.lifecycle.LifecycleTracker;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class UjfeContext {
    private static final ThreadLocal<UjfeContext> CURRENT = new ThreadLocal<>();

    private final ElementIdGenerator elementIdGenerator;
    private final EventRegistrar eventRegistrar;
    private final Executor executor;
    private final ClientState clientState;
    private final Set<String> cssClasses;
    private final LifecycleTracker lifecycleTracker;

    private UjfeContext(Builder builder) {
        this.elementIdGenerator = builder.elementIdGenerator;
        this.eventRegistrar = builder.eventRegistrar;
        this.executor = builder.executor;
        this.clientState = builder.clientState;
        this.cssClasses = new LinkedHashSet<>();
        this.lifecycleTracker = builder.lifecycleTracker;
    }

    public static UjfeContext create() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Optional<UjfeContext> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static <T> T withCurrent(UjfeContext context, Supplier<T> supplier) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(supplier, "supplier");

        UjfeContext previous = CURRENT.get();
        CURRENT.set(context);
        try {
            return supplier.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    public String nextElementId(String prefix) {
        return elementIdGenerator.nextId(prefix);
    }

    public Optional<String> registerEvent(Runnable handler) {
        if (eventRegistrar == null) {
            return Optional.empty();
        }
        Objects.requireNonNull(handler, "handler");
        return Optional.of(eventRegistrar.register(value -> handler.run()));
    }

    public Optional<String> registerEvent(Consumer<String> handler) {
        if (eventRegistrar == null) {
            return Optional.empty();
        }
        return Optional.of(eventRegistrar.register(Objects.requireNonNull(handler, "handler")));
    }

    public Executor executor() {
        return executor;
    }

    public Optional<String> cookie(String name) {
        return clientState.cookie(name);
    }

    public Optional<String> localStorage(String key) {
        return clientState.localStorage(key);
    }

    public Optional<String> sessionStorage(String key) {
        return clientState.sessionStorage(key);
    }

    public ClientState clientState() {
        return clientState;
    }

    public void registerCssClasses(String classes) {
        if (classes == null || classes.trim().isEmpty()) {
            return;
        }

        String[] tokens = classes.trim().split("\\s+");
        for (String token : tokens) {
            if (!token.isEmpty()) {
                cssClasses.add(token);
            }
        }
    }

    public Set<String> cssClasses() {
        return Collections.unmodifiableSet(cssClasses);
    }

    public void trackLifecycle(Object candidate) {
        if (lifecycleTracker != null) {
            lifecycleTracker.track(candidate);
        }
    }

    @FunctionalInterface
    public interface EventRegistrar {
        String register(Consumer<String> handler);
    }

    public static final class Builder {
        private ElementIdGenerator elementIdGenerator = ElementIdGenerator.sequential();
        private EventRegistrar eventRegistrar;
        private Executor executor = Runnable::run;
        private ClientState clientState = ClientState.empty();
        private LifecycleTracker lifecycleTracker;

        private Builder() {
        }

        public Builder elementIdGenerator(ElementIdGenerator elementIdGenerator) {
            this.elementIdGenerator = Objects.requireNonNull(elementIdGenerator, "elementIdGenerator");
            return this;
        }

        public Builder eventRegistrar(EventRegistrar eventRegistrar) {
            this.eventRegistrar = eventRegistrar;
            return this;
        }

        public Builder executor(Executor executor) {
            this.executor = Objects.requireNonNull(executor, "executor");
            return this;
        }

        public Builder clientState(ClientState clientState) {
            this.clientState = Objects.requireNonNull(clientState, "clientState");
            return this;
        }

        public Builder lifecycleTracker(LifecycleTracker lifecycleTracker) {
            this.lifecycleTracker = lifecycleTracker;
            return this;
        }

        public UjfeContext build() {
            return new UjfeContext(this);
        }
    }
}
