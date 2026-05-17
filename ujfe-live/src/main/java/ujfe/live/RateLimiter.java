package ujfe.live;

public interface RateLimiter {
    RateLimitDecision allow(RateLimitRequest request);

    RateLimitMetrics metrics();
}
