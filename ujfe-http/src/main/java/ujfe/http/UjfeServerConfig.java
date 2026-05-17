package ujfe.http;

import java.util.Objects;

public final class UjfeServerConfig {
    private final String host;
    private final int port;
    private final int maxJsonPayloadBytes;

    private UjfeServerConfig(Builder builder) {
        this.host = builder.host;
        this.port = builder.port;
        this.maxJsonPayloadBytes = builder.maxJsonPayloadBytes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public int maxJsonPayloadBytes() {
        return maxJsonPayloadBytes;
    }

    public static final class Builder {
        private String host = "0.0.0.0";
        private int port = 8080;
        private int maxJsonPayloadBytes = ujfe.live.LiveHttpCodec.DEFAULT_MAX_JSON_PAYLOAD_BYTES;

        private Builder() {
        }

        public Builder host(String host) {
            this.host = Objects.requireNonNull(host, "host");
            return this;
        }

        public Builder port(int port) {
            if (port < 1 || port > 65_535) {
                throw new IllegalArgumentException("Port must be between 1 and 65535");
            }
            this.port = port;
            return this;
        }

        public Builder maxJsonPayloadBytes(int maxJsonPayloadBytes) {
            ujfe.live.LiveHttpCodec.requirePayloadSize(0, maxJsonPayloadBytes);
            this.maxJsonPayloadBytes = maxJsonPayloadBytes;
            return this;
        }

        public UjfeServerConfig build() {
            return new UjfeServerConfig(this);
        }
    }
}
