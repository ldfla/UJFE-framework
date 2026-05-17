package ujfe.runtime.lifecycle;

import java.util.Map;

public final class MountContext extends LifecycleContext {
    public MountContext(String path, Object session, String traceId, Map<String, Object> metadata) {
        super(path, session, traceId, metadata);
    }

    public MountContext(LifecycleContext context) {
        this(context.path(), context.session(), context.traceId(), context.metadata());
    }
}
