package ujfe.live;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.LongSupplier;

public final class TokenBucketRateLimiter implements RateLimiter {
    private final LiveSessionConfig config;
    private final LongSupplier nanoTime;
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final RateLimitMetrics metrics = new RateLimitMetrics();

    public TokenBucketRateLimiter(LiveSessionConfig config) {
        this(config, System::nanoTime);
    }

    TokenBucketRateLimiter(LiveSessionConfig config, LongSupplier nanoTime) {
        this.config = Objects.requireNonNull(config, "config");
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
    }

    @Override
    public RateLimitDecision allow(RateLimitRequest request) {
        Objects.requireNonNull(request, "request");
        if (!config.isInternalEndpointRateLimitingEnabled()) {
            metrics.recordAllowed(request.endpointPath());
            return RateLimitDecision.allowed(RateLimitKeyType.SESSION);
        }

        RateLimitKey key = keyFor(request);
        Bucket bucket = buckets.computeIfAbsent(key.value(), ignored -> new Bucket(nanoTime.getAsLong()));
        RateLimitDecision decision = bucket.tryConsume(config, key.type(), nanoTime.getAsLong());
        if (decision.allowed()) {
            metrics.recordAllowed(request.endpointPath());
        } else {
            metrics.recordRejected(request.endpointPath(), decision.keyType());
        }
        return decision;
    }

    @Override
    public RateLimitMetrics metrics() {
        return metrics;
    }

    private RateLimitKey keyFor(RateLimitRequest request) {
        return request.sessionId()
                .map(sessionId -> new RateLimitKey(RateLimitKeyType.SESSION, "session:" + sessionId))
                .orElseGet(() -> new RateLimitKey(
                        RateLimitKeyType.IP,
                        "ip:" + ClientIpResolver.resolve(request.metadata(), config)));
    }

    private static final class Bucket {
        private int tokens;
        private long lastRefillNanos;

        private Bucket(long createdAtNanos) {
            this.tokens = -1;
            this.lastRefillNanos = createdAtNanos;
        }

        private synchronized RateLimitDecision tryConsume(
                LiveSessionConfig config,
                RateLimitKeyType keyType,
                long nowNanos
        ) {
            if (tokens < 0) {
                tokens = config.internalEndpointRateLimitCapacity();
                lastRefillNanos = nowNanos;
            }

            refill(config, nowNanos);
            if (tokens > 0) {
                tokens--;
                return RateLimitDecision.allowed(keyType);
            }
            return RateLimitDecision.rejected(keyType, retryAfter(config, nowNanos));
        }

        private void refill(LiveSessionConfig config, long nowNanos) {
            long elapsed = Math.max(0, nowNanos - lastRefillNanos);
            long period = config.internalEndpointRateLimitRefillPeriod().toNanos();
            if (elapsed < period) {
                return;
            }

            long periods = elapsed / period;
            long refill = periods * (long) config.internalEndpointRateLimitRefillTokens();
            tokens = (int) Math.min(config.internalEndpointRateLimitCapacity(), tokens + refill);
            lastRefillNanos += periods * period;
        }

        private Duration retryAfter(LiveSessionConfig config, long nowNanos) {
            long period = config.internalEndpointRateLimitRefillPeriod().toNanos();
            long elapsed = Math.max(0, nowNanos - lastRefillNanos);
            long remaining = Math.max(1, period - elapsed);
            return Duration.ofNanos(remaining);
        }
    }

    private static final class RateLimitKey {
        private final RateLimitKeyType type;
        private final String value;

        private RateLimitKey(RateLimitKeyType type, String value) {
            this.type = Objects.requireNonNull(type, "type");
            this.value = Objects.requireNonNull(value, "value");
        }

        private RateLimitKeyType type() {
            return type;
        }

        private String value() {
            return value;
        }
    }
}
