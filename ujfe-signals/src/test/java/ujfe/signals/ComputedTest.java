package ujfe.signals;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

final class ComputedTest {
    @Test
    void computedCalculatesDerivedValue() {
        Signal<Integer> count = Signals.signal(2);
        Computed<Integer> doubled = Signals.computed(() -> count.get() * 2);

        assertEquals(4, doubled.get());
    }

    @Test
    void computedEvaluatesLazilyAndCachesValue() {
        AtomicInteger evaluations = new AtomicInteger();
        Computed<Integer> computed = new Computed<>(() -> {
            evaluations.incrementAndGet();
            return 42;
        });

        assertEquals(0, evaluations.get());
        assertEquals(42, computed.get());
        assertEquals(42, computed.get());
        assertEquals(1, evaluations.get());
    }

    @Test
    void dependencyUpdatesInvalidateWithoutImmediateRecompute() {
        Signal<Integer> count = Signals.signal(1);
        AtomicInteger evaluations = new AtomicInteger();
        Computed<Integer> doubled = Signals.computed(() -> {
            evaluations.incrementAndGet();
            return count.get() * 2;
        });

        assertEquals(2, doubled.get());
        count.set(2);
        count.set(3);
        count.update(value -> value + 1);

        assertEquals(1, evaluations.get());
        assertEquals(8, doubled.get());
        assertEquals(2, evaluations.get());
        assertEquals(8, doubled.get());
        assertEquals(2, evaluations.get());
    }

    @Test
    void subscribersAreNotifiedAfterSuccessfulRecomputationOnlyWhenValueChanges() {
        Signal<Integer> count = Signals.signal(1);
        Computed<Integer> parity = Signals.computed(() -> count.get() % 2);
        List<Integer> updates = new ArrayList<>();

        try (AutoCloseable subscription = parity.subscribe(updates::add)) {
            assertEquals(1, parity.get());
            assertTrue(updates.isEmpty());

            count.set(3);
            assertTrue(updates.isEmpty());
            assertEquals(1, parity.get());
            assertTrue(updates.isEmpty());

            count.set(4);
            assertTrue(updates.isEmpty());
            assertEquals(0, parity.get());
            assertEquals(List.of(0), updates);
        } catch (Exception exception) {
            throw new AssertionError("Subscription cleanup failed", exception);
        }
    }

    @Test
    void multipleInvalidationsCollapseAndSubscriberReceivesLatestValue() {
        Signal<Integer> count = Signals.signal(1);
        AtomicInteger evaluations = new AtomicInteger();
        Computed<Integer> doubled = Signals.computed(() -> {
            evaluations.incrementAndGet();
            return count.get() * 2;
        });
        List<Integer> updates = new ArrayList<>();
        try (AutoCloseable subscription = doubled.subscribe(updates::add)) {
            assertEquals(2, doubled.get());
            count.set(2);
            count.set(3);
            count.set(4);

            assertEquals(1, evaluations.get());
            assertTrue(updates.isEmpty());
            assertEquals(8, doubled.get());
            assertEquals(2, evaluations.get());
            assertEquals(List.of(8), updates);
        } catch (Exception exception) {
            throw new AssertionError("Subscription cleanup failed", exception);
        }
    }

    @Test
    void nestedComputedSignalsPropagateInvalidationDeterministically() {
        Signal<Integer> count = Signals.signal(2);
        AtomicInteger doubledEvaluations = new AtomicInteger();
        AtomicInteger labelEvaluations = new AtomicInteger();
        Computed<Integer> doubled = Signals.computed(() -> {
            doubledEvaluations.incrementAndGet();
            return count.get() * 2;
        });
        Computed<String> label = Signals.computed(() -> {
            labelEvaluations.incrementAndGet();
            return "Value: " + doubled.get();
        });

        assertEquals("Value: 4", label.get());
        count.set(3);

        assertEquals(1, doubledEvaluations.get());
        assertEquals(1, labelEvaluations.get());
        assertEquals("Value: 6", label.get());
        assertEquals(2, doubledEvaluations.get());
        assertEquals(2, labelEvaluations.get());
    }

    @Test
    void dynamicDependenciesDoNotLeakBetweenComputations() {
        Signal<Boolean> useFirst = Signals.signal(true);
        Signal<Integer> first = Signals.signal(10);
        Signal<Integer> second = Signals.signal(20);
        AtomicInteger evaluations = new AtomicInteger();
        Computed<Integer> selected = Signals.computed(() -> {
            evaluations.incrementAndGet();
            return useFirst.get() ? first.get() : second.get();
        });

        assertEquals(10, selected.get());
        useFirst.set(false);
        assertEquals(20, selected.get());

        first.set(11);
        assertEquals(20, selected.get());
        assertEquals(2, evaluations.get());

        second.set(21);
        assertEquals(21, selected.get());
        assertEquals(3, evaluations.get());
    }

    @Test
    void exceptionsPropagateWithoutReplacingPreviousSuccessfulCache() {
        Signal<Integer> denominator = Signals.signal(1);
        Computed<Integer> quotient = Signals.computed(() -> {
            int value = denominator.get();
            if (value == 0) {
                throw new IllegalStateException("division by zero");
            }
            return 10 / value;
        });
        List<Integer> updates = new ArrayList<>();
        try (AutoCloseable subscription = quotient.subscribe(updates::add)) {
            assertEquals(10, quotient.get());
            denominator.set(0);

            IllegalStateException failure = assertThrows(IllegalStateException.class, quotient::get);
            assertEquals("division by zero", failure.getMessage());
            assertTrue(updates.isEmpty());

            denominator.set(1);
            assertEquals(10, quotient.get());
            assertTrue(updates.isEmpty());

            denominator.set(2);
            assertEquals(5, quotient.get());
            assertEquals(List.of(5), updates);
        } catch (Exception exception) {
            throw new AssertionError("Subscription cleanup failed", exception);
        }
    }

    @Test
    void failedFirstComputationDoesNotLeakDependencyTracking() {
        Signal<Integer> count = Signals.signal(1);
        Computed<Integer> failing = Signals.computed(() -> {
            count.get();
            throw new IllegalStateException("failed");
        });

        assertThrows(IllegalStateException.class, failing::get);
        count.set(2);

        Computed<Integer> healthy = Signals.computed(() -> count.get() + 1);
        assertEquals(3, healthy.get());
    }

    @Test
    void directCircularComputedDependenciesFailPredictably() {
        AtomicReference<Computed<Integer>> first = new AtomicReference<>();
        AtomicReference<Computed<Integer>> second = new AtomicReference<>();

        first.set(Signals.computed(() -> second.get().get() + 1));
        second.set(Signals.computed(() -> first.get().get() + 1));

        ComputedCycleException failure = assertThrows(ComputedCycleException.class, () -> first.get().get());
        assertTrue(failure.getMessage().contains("Circular computed dependency"));
    }

    @Test
    void computedSignalsAreReadOnlyThroughSignalContract() {
        Signal<Integer> computed = Signals.computed(() -> 1);

        UnsupportedOperationException setFailure =
                assertThrows(UnsupportedOperationException.class, () -> computed.set(2));
        UnsupportedOperationException updateFailure =
                assertThrows(UnsupportedOperationException.class, () -> computed.update(value -> value + 1));

        assertEquals("Computed signals are read-only", setFailure.getMessage());
        assertEquals("Computed signals are read-only", updateFailure.getMessage());
    }

    @Test
    void concurrentReadsShareOneCoherentCacheEntry() throws Exception {
        AtomicInteger evaluations = new AtomicInteger();
        Computed<Integer> computed = Signals.computed(() -> {
            evaluations.incrementAndGet();
            sleepBriefly();
            return 42;
        });
        var executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();

        try {
            for (int index = 0; index < 16; index++) {
                futures.add(executor.submit(() -> {
                    await(start);
                    return computed.get();
                }));
            }

            start.countDown();
            for (Future<Integer> future : futures) {
                assertEquals(42, future.get());
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, evaluations.get());
    }

    @Test
    void concurrentInvalidationAndReadsRemainCoherent() throws Exception {
        Signal<Integer> source = Signals.signal(0);
        Computed<Integer> doubled = Signals.computed(() -> source.get() * 2);
        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
        var executor = Executors.newFixedThreadPool(6);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        assertEquals(0, doubled.get());

        try {
            for (int index = 1; index <= 50; index++) {
                int next = index;
                futures.add(executor.submit(() -> {
                    await(start);
                    source.set(next);
                    int value = doubled.get();
                    if (value % 2 != 0) {
                        failures.add(new AssertionError("Computed value must remain even: " + value));
                    }
                }));
            }

            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertTrue(failures.isEmpty());
        assertEquals(source.get() * 2, doubled.get());
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    private static void sleepBriefly() {
        try {
            TimeUnit.MILLISECONDS.sleep(20);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
