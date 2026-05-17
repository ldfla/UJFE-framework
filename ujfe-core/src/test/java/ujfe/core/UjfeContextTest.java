package ujfe.core;

import org.junit.jupiter.api.Test;
import ujfe.runtime.lifecycle.LifecycleContext;
import ujfe.runtime.lifecycle.LifecycleRuntime;
import ujfe.runtime.lifecycle.LifecycleTracker;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class UjfeContextTest {
    @Test
    void builderConfiguresExecutorAndLifecycleTracker() {
        LifecycleRuntime runtime = LifecycleRuntime.create();
        LifecycleTracker tracker = runtime.beginRender(new LifecycleContext("/", this, "trace", Map.of()));
        CountingLifecycle lifecycle = new CountingLifecycle();
        AtomicBoolean executed = new AtomicBoolean();
        Executor executor = command -> {
            executed.set(true);
            command.run();
        };

        UjfeContext context = UjfeContext.builder()
                .executor(executor)
                .lifecycleTracker(tracker)
                .build();

        context.executor().execute(() -> {
        });
        context.trackLifecycle(lifecycle);
        tracker.complete();

        assertTrue(executed.get());
        assertTrue(runtime.isMounted(lifecycle));
        assertTrue(lifecycle.mounted.get());
    }

    private static final class CountingLifecycle implements Lifecycle {
        private final AtomicBoolean mounted = new AtomicBoolean();

        @Override
        public void onMount() {
            mounted.set(true);
        }
    }
}
