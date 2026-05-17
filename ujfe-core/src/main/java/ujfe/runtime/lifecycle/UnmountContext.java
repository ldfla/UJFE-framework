package ujfe.runtime.lifecycle;

import java.util.Map;
import java.util.Objects;

public final class UnmountContext extends LifecycleContext {
    private final String reason;

    public UnmountContext(String path, Object session, String traceId, Map<String, Object> metadata, String reason) {
        super(path, session, traceId, metadata);
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public UnmountContext(LifecycleContext context, String reason) {
        this(context.path(), context.session(), context.traceId(), context.metadata(), reason);
    }

    public String reason() {
        return reason;
    }
}
