package ujfe.live;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

public final class RateLimitMetrics {
    private final LongAdder allowedRequests = new LongAdder();
    private final LongAdder rejectedRequests = new LongAdder();
    private final ConcurrentMap<String, LongAdder> allowedByEndpoint = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> rejectedByEndpoint = new ConcurrentHashMap<>();
    private final ConcurrentMap<RateLimitKeyType, LongAdder> rejectedByKeyType = new ConcurrentHashMap<>();

    void recordAllowed(String endpointPath) {
        allowedRequests.increment();
        allowedByEndpoint.computeIfAbsent(requireEndpoint(endpointPath), key -> new LongAdder()).increment();
    }

    void recordRejected(String endpointPath, RateLimitKeyType keyType) {
        rejectedRequests.increment();
        rejectedByEndpoint.computeIfAbsent(requireEndpoint(endpointPath), key -> new LongAdder()).increment();
        rejectedByKeyType.computeIfAbsent(Objects.requireNonNull(keyType, "keyType"), key -> new LongAdder()).increment();
    }

    public long allowedRequests() {
        return allowedRequests.sum();
    }

    public long rejectedRequests() {
        return rejectedRequests.sum();
    }

    public long allowedRequests(String endpointPath) {
        return count(allowedByEndpoint, requireEndpoint(endpointPath));
    }

    public long rejectedRequests(String endpointPath) {
        return count(rejectedByEndpoint, requireEndpoint(endpointPath));
    }

    public long rejectedRequests(RateLimitKeyType keyType) {
        return count(rejectedByKeyType, Objects.requireNonNull(keyType, "keyType"));
    }

    private static <K> long count(ConcurrentMap<K, LongAdder> values, K key) {
        LongAdder value = values.get(key);
        return value == null ? 0 : value.sum();
    }

    private static String requireEndpoint(String endpointPath) {
        Objects.requireNonNull(endpointPath, "endpointPath");
        if (endpointPath.isBlank()) {
            throw new IllegalArgumentException("endpointPath must not be blank");
        }
        return endpointPath;
    }
}
