package ujfe.live;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configures the HTTP security headers emitted by UJFE runtime adapters.
 */
public final class SecurityHeadersConfig {
    public static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    public static final String X_FRAME_OPTIONS = "X-Frame-Options";
    public static final String REFERRER_POLICY = "Referrer-Policy";
    public static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";
    public static final String PERMISSIONS_POLICY = "Permissions-Policy";

    public static final String DEFAULT_CONTENT_SECURITY_POLICY = "default-src 'self'; "
        + "script-src 'self'; "
        + "style-src 'self' 'unsafe-inline'; "
        + "img-src 'self' data: https:; "
        + "media-src 'self' data: https:; "
        + "object-src 'none'; "
        + "base-uri 'self'; "
        + "frame-ancestors 'none'; "
        + "form-action 'self'";

    private final boolean enabled;
    private final Map<String, String> headers;

    private SecurityHeadersConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(builder.headers));
    }

    public static SecurityHeadersConfig defaults() {
        return builder().build();
    }

    public static SecurityHeadersConfig disabled() {
        return builder().enabled(false)
            .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Map<String, String> headers() {
        if (!enabled) {
            return Map.of();
        }
        return headers;
    }

    public static final class Builder {
        private boolean enabled = true;
        private final Map<String, String> headers = defaultHeaders();

        private Builder() {
        }

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder disable() {
            this.enabled = false;
            return this;
        }

        public Builder header(String name, String value) {
            headers.put(requireHeaderName(name), requireHeaderValue(value));
            return this;
        }

        public Builder remove(String name) {
            headers.remove(requireHeaderName(name));
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            Objects.requireNonNull(headers, "headers");
            headers.forEach(this::header);
            return this;
        }

        public SecurityHeadersConfig build() {
            return new SecurityHeadersConfig(this);
        }
    }

    private static Map<String, String> defaultHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(X_CONTENT_TYPE_OPTIONS, "nosniff");
        headers.put(REFERRER_POLICY, "strict-origin-when-cross-origin");
        headers.put(X_FRAME_OPTIONS, "DENY");
        headers.put(CONTENT_SECURITY_POLICY, DEFAULT_CONTENT_SECURITY_POLICY);
        headers.put(PERMISSIONS_POLICY, "geolocation=(), microphone=(), camera=()");
        return headers;
    }

    private static String requireHeaderName(String name) {
        Objects.requireNonNull(name, "name");
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Header name must not be blank");
        }
        if (trimmed.indexOf('\r') >= 0 || trimmed.indexOf('\n') >= 0 || trimmed.indexOf(':') >= 0) {
            throw new IllegalArgumentException("Header name contains invalid characters");
        }
        return trimmed;
    }

    private static String requireHeaderValue(String value) {
        Objects.requireNonNull(value, "value");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Header value must not be blank");
        }
        if (trimmed.indexOf('\r') >= 0 || trimmed.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("Header value contains invalid characters");
        }
        return trimmed;
    }
}
