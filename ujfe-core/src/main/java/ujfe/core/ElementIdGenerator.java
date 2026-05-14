package ujfe.core;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@FunctionalInterface
public interface ElementIdGenerator {
    String nextId(String prefix);

    static ElementIdGenerator sequential() {
        AtomicLong counter = new AtomicLong();
        return prefix -> {
            String normalizedPrefix = Objects.requireNonNullElse(prefix, "ujfe");
            return normalizedPrefix + "-" + counter.incrementAndGet();
        };
    }
}
