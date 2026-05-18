package ujfe.live;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.LongSupplier;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

final class TokenBucketRateLimiterTest {
    @Test
    void requestsWithinConfiguredLimitSucceedAndExceededRequestIsRejected() {
        MutableNanoTime clock = new MutableNanoTime();
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(rateLimitConfig(2), clock);
        LiveHttpRequestMetadata metadata = metadata("10.0.0.10");

        assertTrue(limiter.allow(request("session-a", metadata))
            .allowed());
        assertTrue(limiter.allow(request("session-a", metadata))
            .allowed());
        RateLimitDecision rejected = limiter.allow(request("session-a", metadata));

        assertFalse(rejected.allowed());
        assertEquals(RateLimitKeyType.SESSION, rejected.keyType());
        assertEquals(Duration.ofSeconds(10), rejected.retryAfter()
            .orElseThrow());
        assertEquals(2, limiter.metrics()
            .allowedRequests());
        assertEquals(1, limiter.metrics()
            .rejectedRequests(LiveHttpPaths.EVENT));
        assertEquals(1, limiter.metrics()
            .rejectedRequests(RateLimitKeyType.SESSION));
    }

    @Test
    void tokenBucketRefillsDeterministically() {
        MutableNanoTime clock = new MutableNanoTime();
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(rateLimitConfig(1), clock);
        LiveHttpRequestMetadata metadata = metadata("10.0.0.10");

        assertTrue(limiter.allow(request("session-a", metadata))
            .allowed());
        assertFalse(limiter.allow(request("session-a", metadata))
            .allowed());

        clock.advance(Duration.ofSeconds(10));

        assertTrue(limiter.allow(request("session-a", metadata))
            .allowed());
    }

    @Test
    void differentSessionsHaveIndependentLimits() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(rateLimitConfig(1), new MutableNanoTime());
        LiveHttpRequestMetadata metadata = metadata("10.0.0.10");

        assertTrue(limiter.allow(request("session-a", metadata))
            .allowed());
        assertTrue(limiter.allow(request("session-b", metadata))
            .allowed());
        assertFalse(limiter.allow(request("session-a", metadata))
            .allowed());
    }

    @Test
    void fallbackLimitAppliesByRemoteAddressWhenSessionIsMissing() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(rateLimitConfig(1), new MutableNanoTime());

        assertTrue(limiter.allow(request(null, metadata("10.0.0.10")))
            .allowed());
        RateLimitDecision rejected = limiter.allow(request(null, metadata("10.0.0.10")));
        assertTrue(limiter.allow(request(null, metadata("10.0.0.11")))
            .allowed());

        assertFalse(rejected.allowed());
        assertEquals(RateLimitKeyType.IP, rejected.keyType());
        assertEquals(1, limiter.metrics()
            .rejectedRequests(RateLimitKeyType.IP));
    }

    @Test
    void forwardedHeadersAreNotTrustedByDefault() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(rateLimitConfig(1), new MutableNanoTime());

        assertTrue(limiter.allow(request(null, metadata("10.0.0.1", "203.0.113.10")))
            .allowed());
        RateLimitDecision rejected = limiter.allow(request(null, metadata("10.0.0.1", "203.0.113.11")));

        assertFalse(rejected.allowed());
        assertEquals(RateLimitKeyType.IP, rejected.keyType());
    }

    @Test
    void trustedProxyConfigurationUsesForwardedClientAddress() {
        LiveSessionConfig config = LiveSessionConfig.builder()
            .internalEndpointRateLimit(1, 1, Duration.ofSeconds(10))
            .trustedProxy("10.0.0.1")
            .build();
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(config, new MutableNanoTime());

        assertTrue(limiter.allow(request(null, metadata("10.0.0.1", "203.0.113.10")))
            .allowed());
        assertTrue(limiter.allow(request(null, metadata("10.0.0.1", "203.0.113.11")))
            .allowed());
        assertFalse(limiter.allow(request(null, metadata("10.0.0.1", "203.0.113.10")))
            .allowed());

        assertEquals(java.util.Set.of("10.0.0.1"), config.trustedProxyAddresses());
    }

    @Test
    void disabledRateLimitingAllowsRequestsAndStillRecordsAllowedMetrics() {
        LiveSessionConfig config = LiveSessionConfig.builder()
            .internalEndpointRateLimitingEnabled(false)
            .internalEndpointRateLimit(1, 1, Duration.ofSeconds(10))
            .build();
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(config, new MutableNanoTime());

        assertTrue(limiter.allow(request("session-a", metadata("10.0.0.10")))
            .allowed());
        assertTrue(limiter.allow(request("session-a", metadata("10.0.0.10")))
            .allowed());

        assertFalse(config.isInternalEndpointRateLimitingEnabled());
        assertEquals(2, limiter.metrics()
            .allowedRequests(LiveHttpPaths.EVENT));
        assertEquals(0, limiter.metrics()
            .rejectedRequests());
    }

    @Test
    void configurationRejectsInvalidRateLimitValues() {
        assertThrows(IllegalArgumentException.class,
            () -> LiveSessionConfig.builder()
                .internalEndpointRateLimit(0, 1, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class,
            () -> LiveSessionConfig.builder()
                .internalEndpointRateLimit(1, 0, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class,
            () -> LiveSessionConfig.builder()
                .internalEndpointRateLimit(1, 1, Duration.ZERO));
    }

    @Test
    void logsRateLimitRejectionWithSafeMetadataOnly() {
        LiveRateLimitException failure = new LiveRateLimitException(
            LiveHttpPaths.EVENT,
            RateLimitKeyType.SESSION,
            Duration.ofSeconds(5)
        );

        try (LogCapture logs = LogCapture.attach()) {
            LiveHttpCodec.logRejectedRateLimit(failure, "spring adapter", "trace-42\nsecret");

            String message = logs.singleMessage();
            assertTrue(message.contains("event=ujfe.live_rate_limit_rejected"));
            assertTrue(message.contains("reason=rate_limit_exceeded"));
            assertTrue(message.contains("endpoint=/_ujfe/event"));
            assertTrue(message.contains("keyType=session"));
            assertTrue(message.contains("adapter=spring_adapter"));
            assertTrue(message.contains("retryAfterSeconds=5"));
            assertTrue(message.contains("traceId=trace-42_secret"));
            assertFalse(message.contains("csrf"));
            assertFalse(message.contains("Cookie"));
            assertFalse(message.contains("{\"eventId\""));
        }
    }

    private static LiveSessionConfig rateLimitConfig(int capacity) {
        return LiveSessionConfig.builder()
            .internalEndpointRateLimit(capacity, 1, Duration.ofSeconds(10))
            .build();
    }

    private static RateLimitRequest request(String sessionId, LiveHttpRequestMetadata metadata) {
        return new RateLimitRequest(LiveHttpPaths.EVENT, sessionId, metadata);
    }

    private static LiveHttpRequestMetadata metadata(String remoteAddress) {
        return metadata(remoteAddress, null);
    }

    private static LiveHttpRequestMetadata metadata(String remoteAddress, String xForwardedFor) {
        return new LiveHttpRequestMetadata(null, null, null, "localhost", "http",
            remoteAddress, null, xForwardedFor, null);
    }

    private static final class MutableNanoTime implements LongSupplier {
        private long current;

        @Override
        public long getAsLong() {
            return current;
        }

        void advance(Duration duration) {
            current += duration.toNanos();
        }
    }

    private static final class LogCapture implements AutoCloseable {
        private final Logger logger;
        private final Handler handler;
        private final List<String> messages = new CopyOnWriteArrayList<>();
        private final boolean useParentHandlers;

        private LogCapture(Logger logger) {
            this.logger = logger;
            this.useParentHandlers = logger.getUseParentHandlers();
            this.handler = new Handler() {
                @Override
                public void publish(LogRecord record) {
                    messages.add(record.getMessage());
                }

                @Override
                public void flush() {
                }

                @Override
                public void close() {
                }
            };
            this.logger.setUseParentHandlers(false);
            this.logger.addHandler(handler);
        }

        static LogCapture attach() {
            return new LogCapture(Logger.getLogger(LiveHttpCodec.class.getName()));
        }

        String singleMessage() {
            assertEquals(1, messages.size());
            return messages.get(0);
        }

        @Override
        public void close() {
            logger.removeHandler(handler);
            logger.setUseParentHandlers(useParentHandlers);
        }
    }
}
