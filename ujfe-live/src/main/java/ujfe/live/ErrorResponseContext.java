package ujfe.live;

import ujfe.runtime.action.RuntimePhase;

/**
 * Safe request metadata used for error logging and error response rendering.
 *
 * <p>This context intentionally does not carry request bodies, cookies,
 * authorization headers, CSRF tokens, or other sensitive values.</p>
 */
public final class ErrorResponseContext {
    private final String adapter;
    private final String method;
    private final String path;
    private final String requestId;
    private final RuntimePhase phase;

    private ErrorResponseContext(Builder builder) {
        this.adapter = builder.adapter;
        this.method = builder.method;
        this.path = builder.path;
        this.requestId = builder.requestId;
        this.phase = builder.phase;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String adapter() {
        return adapter;
    }

    public String method() {
        return method;
    }

    public String path() {
        return path;
    }

    public String requestId() {
        return requestId;
    }

    public RuntimePhase phase() {
        return phase;
    }

    public static final class Builder {
        private String adapter;
        private String method;
        private String path;
        private String requestId;
        private RuntimePhase phase = RuntimePhase.INTERNAL;

        private Builder() {
        }

        public Builder adapter(String adapter) {
            this.adapter = adapter;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder phase(RuntimePhase phase) {
            if (phase != null) {
                this.phase = phase;
            }
            return this;
        }

        public ErrorResponseContext build() {
            return new ErrorResponseContext(this);
        }
    }
}
