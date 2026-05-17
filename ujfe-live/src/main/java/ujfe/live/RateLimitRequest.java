package ujfe.live;

import java.util.Objects;
import java.util.Optional;

public final class RateLimitRequest {
    private final String endpointPath;
    private final String sessionId;
    private final LiveHttpRequestMetadata metadata;

    public RateLimitRequest(String endpointPath, String sessionId, LiveHttpRequestMetadata metadata) {
        this.endpointPath = requireEndpointPath(endpointPath);
        this.sessionId = sessionId;
        this.metadata = Objects.requireNonNull(metadata, "metadata");
    }

    public String endpointPath() {
        return endpointPath;
    }

    public Optional<String> sessionId() {
        return Optional.ofNullable(sessionId).filter(value -> !value.isBlank());
    }

    public LiveHttpRequestMetadata metadata() {
        return metadata;
    }

    private static String requireEndpointPath(String value) {
        Objects.requireNonNull(value, "endpointPath");
        if (value.isBlank()) {
            throw new IllegalArgumentException("endpointPath must not be blank");
        }
        return value;
    }
}
