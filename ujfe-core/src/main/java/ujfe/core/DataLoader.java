package ujfe.core;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Blocking-friendly data loader abstraction with timeout, fallback, and
 * optional virtual-thread execution when available at runtime.
 */
public final class DataLoader<T> {
    private final Class<T> type;
    private final Map<String, Supplier<?>> parallelTasks = new LinkedHashMap<>();
    private Supplier<T> source;
    private Supplier<T> fallback;
    private Duration timeout = Duration.ofSeconds(30);
    private Executor executor = Executors.newCachedThreadPool();

    DataLoader(Class<T> type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    public DataLoader<T> source(Supplier<T> source) {
        this.source = Objects.requireNonNull(source, "source");
        return this;
    }

    public DataLoader<T> fallback(Supplier<T> fallback) {
        this.fallback = Objects.requireNonNull(fallback, "fallback");
        return this;
    }

    public DataLoader<T> timeout(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        this.timeout = timeout;
        return this;
    }

    public DataLoader<T> executor(Executor executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
        return this;
    }

    public DataLoader<T> virtualThreadsWhenAvailable() {
        this.executor = virtualThreadExecutorOrFallback();
        return this;
    }

    public DataLoader<T> parallel(String name, Supplier<?> task) {
        parallelTasks.put(BrowserApiBridge.safeName(name), Objects.requireNonNull(task, "task"));
        return this;
    }

    public LoadResult<T> load() {
        if (source == null) {
            throw new IllegalStateException("DataLoader source is required");
        }
        return run(source);
    }

    public LoadResult<Map<String, Object>> loadMap() {
        if (parallelTasks.isEmpty()) {
            throw new IllegalStateException("DataLoader parallel tasks are required");
        }
        return run(() -> {
            Map<String, CompletableFuture<?>> futures = new LinkedHashMap<>();
            parallelTasks.forEach((name, task) -> futures.put(name, CompletableFuture.supplyAsync(task, executor)));
            Map<String, Object> values = new LinkedHashMap<>();
            for (Map.Entry<String, CompletableFuture<?>> entry : futures.entrySet()) {
                try {
                    values.put(entry.getKey(), entry.getValue()
                        .get());
                } catch (InterruptedException exception) {
                    Thread.currentThread()
                        .interrupt();
                    throw new IllegalStateException("Data loader interrupted", exception);
                } catch (ExecutionException exception) {
                    throw new IllegalStateException("Data loader task failed", exception.getCause());
                }
            }
            return values;
        });
    }

    public Class<T> type() {
        return type;
    }

    private <R> LoadResult<R> run(Supplier<R> supplier) {
        CompletableFuture<R> future = CompletableFuture.supplyAsync(supplier, executor);
        try {
            return LoadResult.success(future.get(timeout.toMillis(), TimeUnit.MILLISECONDS));
        } catch (TimeoutException exception) {
            future.cancel(true);
            if (fallback != null) {
                @SuppressWarnings("unchecked")
                R fallbackValue = (R) fallback.get();
                return LoadResult.success(fallbackValue);
            }
            return LoadResult.timeout(exception);
        } catch (InterruptedException exception) {
            Thread.currentThread()
                .interrupt();
            return LoadResult.failure(exception);
        } catch (ExecutionException exception) {
            if (fallback != null) {
                @SuppressWarnings("unchecked")
                R fallbackValue = (R) fallback.get();
                return LoadResult.success(fallbackValue);
            }
            return LoadResult.failure(exception.getCause() == null ? exception : exception.getCause());
        }
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    private static Executor virtualThreadExecutorOrFallback() {
        try {
            // Keep ujfe-core Java 11 compatible while enabling virtual threads on Java 21+.
            Method method = Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
            Object executor = method.invoke(null);
            if (executor instanceof Executor) {
                return (Executor) executor;
            }
        } catch (ReflectiveOperationException ignored) {
            // Java 11 compatible fallback.
        }
        return Executors.newCachedThreadPool();
    }
}
