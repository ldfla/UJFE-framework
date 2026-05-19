package ujfe.observability;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Production-safe trace data for one UJFE render attempt.
 */
public final class RenderTrace {
    private final String traceId;
    private final String requestId;
    private final String route;
    private final String httpMethod;
    private final Integer httpStatus;
    private final String adapterName;
    private final Duration duration;
    private final long responseSizeBytes;
    private final TraceStatus status;
    private final String errorCode;
    private final String errorType;
    private final Instant startedAt;
    private final String source;

    private RenderTrace(Builder builder) {
        this.traceId = builder.traceId;
        this.requestId = builder.requestId;
        this.route = builder.route;
        this.httpMethod = builder.httpMethod;
        this.httpStatus = builder.httpStatus;
        this.adapterName = builder.adapterName;
        this.duration = Objects.requireNonNull(builder.duration, "duration");
        this.responseSizeBytes = builder.responseSizeBytes;
        this.status = Objects.requireNonNull(builder.status, "status");
        this.errorCode = builder.errorCode;
        this.errorType = builder.errorType;
        this.startedAt = Objects.requireNonNull(builder.startedAt, "startedAt");
        this.source = builder.source;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String traceId() {
        return traceId;
    }

    public String requestId() {
        return requestId;
    }

    public String route() {
        return route;
    }

    public String httpMethod() {
        return httpMethod;
    }

    public Integer httpStatus() {
        return httpStatus;
    }

    public String adapterName() {
        return adapterName;
    }

    public Duration duration() {
        return duration;
    }

    public long responseSizeBytes() {
        return responseSizeBytes;
    }

    public TraceStatus status() {
        return status;
    }

    public String errorCode() {
        return errorCode;
    }

    public String errorType() {
        return errorType;
    }

    public Instant startedAt() {
        return startedAt;
    }

    public String source() {
        return source;
    }

    public static final class Builder {
        private String traceId;
        private String requestId;
        private String route;
        private String httpMethod;
        private Integer httpStatus;
        private String adapterName;
        private Duration duration = Duration.ZERO;
        private long responseSizeBytes;
        private TraceStatus status = TraceStatus.SUCCESS;
        private String errorCode;
        private String errorType;
        private Instant startedAt = Instant.EPOCH;
        private String source = "render";

        private Builder() {
        }

        public Builder traceId(String traceId) {
            this.traceId = blankToNull(traceId);
            return this;
        }

        public Builder requestId(String requestId) {
            this.requestId = blankToNull(requestId);
            return this;
        }

        public Builder route(String route) {
            this.route = blankToNull(route);
            return this;
        }

        public Builder httpMethod(String httpMethod) {
            this.httpMethod = blankToNull(httpMethod);
            return this;
        }

        public Builder httpStatus(Integer httpStatus) {
            this.httpStatus = httpStatus;
            return this;
        }

        public Builder adapterName(String adapterName) {
            this.adapterName = blankToNull(adapterName);
            return this;
        }

        public Builder duration(Duration duration) {
            this.duration = Objects.requireNonNull(duration, "duration");
            return this;
        }

        public Builder responseSizeBytes(long responseSizeBytes) {
            this.responseSizeBytes = Math.max(0L, responseSizeBytes);
            return this;
        }

        public Builder status(TraceStatus status) {
            this.status = Objects.requireNonNull(status, "status");
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = blankToNull(errorCode);
            return this;
        }

        public Builder errorType(String errorType) {
            this.errorType = blankToNull(errorType);
            return this;
        }

        public Builder startedAt(Instant startedAt) {
            this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
            return this;
        }

        public Builder source(String source) {
            this.source = blankToNull(source);
            return this;
        }

        public RenderTrace build() {
            return new RenderTrace(this);
        }

        private static String blankToNull(String value) {
            return value == null || value.isBlank() ? null : value;
        }
    }
}
