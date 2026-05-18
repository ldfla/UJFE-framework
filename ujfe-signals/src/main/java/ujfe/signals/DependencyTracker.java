package ujfe.signals;

import java.util.*;
import java.util.function.Supplier;

final class DependencyTracker {
    private static final ThreadLocal<ArrayDeque<DependencyCollector>> COLLECTORS =
        ThreadLocal.withInitial(ArrayDeque::new);

    private DependencyTracker() {
    }

    static void record(SignalDependency dependency) {
        Objects.requireNonNull(dependency, "dependency");
        ArrayDeque<DependencyCollector> collectors = COLLECTORS.get();
        if (!collectors.isEmpty()) {
            collectors.peek()
                .add(dependency);
        }
    }

    static <T> Evaluation<T> collect(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        DependencyCollector collector = new DependencyCollector();
        ArrayDeque<DependencyCollector> collectors = COLLECTORS.get();
        collectors.push(collector);
        try {
            return new Evaluation<>(supplier.get(), collector.dependencies());
        } finally {
            collectors.pop();
            if (collectors.isEmpty()) {
                COLLECTORS.remove();
            }
        }
    }

    static final class Evaluation<T> {
        private final T value;
        private final List<SignalDependency> dependencies;

        private Evaluation(T value, List<SignalDependency> dependencies) {
            this.value = value;
            this.dependencies = dependencies;
        }

        T value() {
            return value;
        }

        List<SignalDependency> dependencies() {
            return dependencies;
        }
    }

    private static final class DependencyCollector {
        private final IdentityHashMap<SignalDependency, Boolean> seen = new IdentityHashMap<>();
        private final List<SignalDependency> dependencies = new ArrayList<>();

        void add(SignalDependency dependency) {
            if (!seen.containsKey(dependency)) {
                seen.put(dependency, Boolean.TRUE);
                dependencies.add(dependency);
            }
        }

        List<SignalDependency> dependencies() {
            return List.copyOf(dependencies);
        }
    }
}
