package ujfe.live;

import java.time.Duration;
import java.util.Objects;
import java.util.OptionalLong;

public final class LiveRateLimitException extends RuntimeException {
    private final String endpointPath;
    private final RateLimitKeyType keyType;
    private final Duration retryAfter;

    public LiveRateLimitException(String endpointPath, RateLimitKeyType keyType, Duration retryAfter) {
        super("Rate limit exceeded.");
        this.endpointPath = Objects.requireNonNull(endpointPath, "endpointPath");
        this.keyType = Objects.requireNonNull(keyType, "keyType");
        this.retryAfter = retryAfter;
    }

    public String endpointPath() {
        return endpointPath;
    }

    public RateLimitKeyType keyType() {
        return keyType;
    }

    public OptionalLong retryAfterSeconds() {
        if (retryAfter == null) {
            return OptionalLong.empty();
        }
        long seconds = Math.max(1L, (long) Math.ceil(retryAfter.toMillis() / 1000.0));
        return OptionalLong.of(seconds);
    }

    public String safeMessage() {
        return "Rate limit exceeded.";
    }
}
