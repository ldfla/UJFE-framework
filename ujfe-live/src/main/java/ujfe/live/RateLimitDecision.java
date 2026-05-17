package ujfe.live;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public final class RateLimitDecision {
    private final boolean allowed;
    private final RateLimitKeyType keyType;
    private final Duration retryAfter;

    private RateLimitDecision(boolean allowed, RateLimitKeyType keyType, Duration retryAfter) {
        this.allowed = allowed;
        this.keyType = Objects.requireNonNull(keyType, "keyType");
        this.retryAfter = retryAfter;
    }

    public static RateLimitDecision allowed(RateLimitKeyType keyType) {
        return new RateLimitDecision(true, keyType, null);
    }

    public static RateLimitDecision rejected(RateLimitKeyType keyType, Duration retryAfter) {
        Objects.requireNonNull(retryAfter, "retryAfter");
        if (retryAfter.isNegative() || retryAfter.isZero()) {
            throw new IllegalArgumentException("retryAfter must be positive");
        }
        return new RateLimitDecision(false, keyType, retryAfter);
    }

    public boolean allowed() {
        return allowed;
    }

    public RateLimitKeyType keyType() {
        return keyType;
    }

    public Optional<Duration> retryAfter() {
        return Optional.ofNullable(retryAfter);
    }
}
