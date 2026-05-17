package ujfe.runtime.lifecycle;

import org.junit.jupiter.api.Test;
import ujfe.core.Lifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class LifecycleRuntimeTest {
    @Test
    void onMountExecutesOnceForStableInstance() {
        LifecycleRuntime runtime = LifecycleRuntime.create();
        CountingLifecycle component = new CountingLifecycle("component");

        assertTrue(render(runtime, "/", component).isEmpty());
        assertTrue(render(runtime, "/", component).isEmpty());

        assertEquals(1, component.mounts());
        assertEquals(0, component.unmounts());
        assertTrue(runtime.isMounted(component));
    }

    @Test
    void cleanupUnmountsMountedComponentsInReverseMountOrder() {
        LifecycleRuntime runtime = LifecycleRuntime.create();
        List<String> events = new ArrayList<>();
        CountingLifecycle first = new CountingLifecycle("first", events);
        CountingLifecycle second = new CountingLifecycle("second", events);

        render(runtime, "/", first, second);
        List<LifecycleException> failures = runtime.cleanup(unmountContext("session-close"));

        assertTrue(failures.isEmpty());
        assertEquals(List.of("first:mount", "second:mount", "second:unmount", "first:unmount"), events);
        assertTrue(runtime.registry().isEmpty());
    }

    @Test
    void routeSwitchUnmountsRemovedAndMountsNewComponents() {
        LifecycleRuntime runtime = LifecycleRuntime.create();
        CountingLifecycle dashboard = new CountingLifecycle("dashboard");
        CountingLifecycle settings = new CountingLifecycle("settings");

        render(runtime, "/dashboard", dashboard);
        render(runtime, "/settings", settings);

        assertEquals(1, dashboard.mounts());
        assertEquals(1, dashboard.unmounts());
        assertEquals(1, settings.mounts());
        assertEquals(0, settings.unmounts());
        assertFalse(runtime.isMounted(dashboard));
        assertTrue(runtime.isMounted(settings));
    }

    @Test
    void multipleMountedComponentsAreTracked() {
        LifecycleRuntime runtime = LifecycleRuntime.create();
        CountingLifecycle first = new CountingLifecycle("first");
        CountingLifecycle second = new CountingLifecycle("second");

        render(runtime, "/", first, second, first);

        assertEquals(2, runtime.registry().size());
        List<MountedComponent> mounted = runtime.registry().mountedComponents();
        assertEquals(2, mounted.size());
        assertSame(first, mounted.get(0).lifecycle());
        assertSame(second, mounted.get(1).lifecycle());
        assertEquals(0, mounted.get(0).mountIndex());
        assertEquals(1, mounted.get(1).mountIndex());
        assertEquals("/", mounted.get(0).path());
        assertEquals("trace", mounted.get(0).traceId());
        assertNotNull(mounted.get(0).mountedAt());
        assertEquals(1, first.mounts());
        assertEquals(1, second.mounts());
    }

    @Test
    void concurrentRenderingDoesNotDuplicateMounts() throws Exception {
        LifecycleRuntime runtime = LifecycleRuntime.create();
        CountingLifecycle component = new CountingLifecycle("shared");
        var executor = Executors.newFixedThreadPool(6);
        CountDownLatch start = new CountDownLatch(1);

        for (int index = 0; index < 12; index++) {
            executor.submit(() -> {
                await(start);
                render(runtime, "/", component);
            });
        }

        start.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(1, component.mounts());
        assertTrue(runtime.isMounted(component));
    }

    @Test
    void mountFailureDoesNotLeakPartialMountState() {
        LifecycleRuntime runtime = LifecycleRuntime.create();
        CountingLifecycle stable = new CountingLifecycle("stable");
        FailingMountLifecycle failing = new FailingMountLifecycle();

        LifecycleException failure = assertThrows(
                LifecycleException.class, () -> render(runtime, "/", stable, failing));

        assertEquals("/", failure.context().orElseThrow().path());
        assertEquals(1, stable.mounts());
        assertEquals(1, stable.unmounts());
        assertEquals(1, failing.mountAttempts());
        assertTrue(runtime.registry().isEmpty());
    }

    @Test
    void unmountFailureIsReportedAfterCleanupContinues() {
        LifecycleRuntime runtime = LifecycleRuntime.create();
        CountingLifecycle stable = new CountingLifecycle("stable");
        FailingUnmountLifecycle failing = new FailingUnmountLifecycle();

        render(runtime, "/", stable, failing);
        List<LifecycleException> failures = runtime.cleanup(unmountContext("session-close"));

        assertEquals(1, failures.size());
        assertEquals("onUnmount", failures.get(0).callback());
        assertEquals("session-close", ((UnmountContext) failures.get(0).context().orElseThrow()).reason());
        assertEquals(1, stable.unmounts());
        assertEquals(1, failing.unmountAttempts());
        assertTrue(runtime.registry().isEmpty());
    }

    @Test
    void emptyRegistryCleanupIsSafe() {
        LifecycleRuntime runtime = LifecycleRuntime.create();

        assertTrue(runtime.cleanup(unmountContext("empty")).isEmpty());
        assertTrue(runtime.registry().isEmpty());
    }

    @Test
    void lifecycleEventsExposeContextAndComponent() {
        CountingLifecycle lifecycle = new CountingLifecycle("event");
        MountContext mountContext = new MountContext("/", this, "trace", Map.of());
        MountedComponent mounted = new MountedComponent(lifecycle, 0, mountContext);
        MountedComponent unmounted = mounted.unmounted();

        MountEvent mountEvent = new MountEvent(mounted, mountContext);
        UnmountEvent unmountEvent = new UnmountEvent(unmounted, new UnmountContext(mountContext, "test"));

        assertEquals(mounted, mountEvent.component());
        assertEquals(0, mounted.mountIndex());
        assertNotNull(mounted.mountedAt());
        assertNotNull(mountEvent.timestamp());
        assertNotNull(unmountEvent.timestamp());
        assertEquals(LifecycleState.UNMOUNTED, unmounted.state());
        assertEquals("/", mountEvent.context().path());
        assertEquals("test", unmountEvent.context().reason());
    }

    private static List<LifecycleException> render(LifecycleRuntime runtime, String path, Object... components) {
        LifecycleTracker tracker = runtime.beginRender(new LifecycleContext(path, runtime, "trace", Map.of()));
        for (Object component : components) {
            tracker.track(component);
        }
        return tracker.complete();
    }

    private static UnmountContext unmountContext(String reason) {
        return new UnmountContext("/", null, "trace", Map.of(), reason);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private static class CountingLifecycle implements Lifecycle {
        private final String name;
        private final List<String> events;
        private final AtomicInteger mounts = new AtomicInteger();
        private final AtomicInteger unmounts = new AtomicInteger();

        CountingLifecycle(String name) {
            this(name, new ArrayList<>());
        }

        CountingLifecycle(String name, List<String> events) {
            this.name = name;
            this.events = events;
        }

        @Override
        public void onMount() {
            mounts.incrementAndGet();
            events.add(name + ":mount");
        }

        @Override
        public void onUnmount() {
            unmounts.incrementAndGet();
            events.add(name + ":unmount");
        }

        int mounts() {
            return mounts.get();
        }

        int unmounts() {
            return unmounts.get();
        }
    }

    private static final class FailingMountLifecycle implements Lifecycle {
        private final AtomicInteger mountAttempts = new AtomicInteger();

        @Override
        public void onMount() {
            mountAttempts.incrementAndGet();
            throw new IllegalStateException("mount failed");
        }

        int mountAttempts() {
            return mountAttempts.get();
        }
    }

    private static final class FailingUnmountLifecycle implements Lifecycle {
        private final AtomicInteger unmountAttempts = new AtomicInteger();

        @Override
        public void onUnmount() {
            unmountAttempts.incrementAndGet();
            throw new IllegalStateException("unmount failed");
        }

        int unmountAttempts() {
            return unmountAttempts.get();
        }
    }
}
