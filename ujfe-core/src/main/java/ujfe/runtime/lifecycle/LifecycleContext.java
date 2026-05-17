package ujfe.runtime.lifecycle;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Base context for server-side lifecycle operations.
 */
public class LifecycleContext {
    private final String path;
    private final Object session;
    private final String traceId;
    private final Map<String, Object> metadata;

    public LifecycleContext(String path, Object session, String traceId, Map<String, Object> metadata) {
        this.path = path;
        this.session = session;
        this.traceId = Objects.requireNonNull(traceId, "traceId");
        this.metadata = copy(metadata);
    }

    public String path() {
        return path;
    }

    public Object session() {
        return session;
    }

    public String traceId() {
        return traceId;
    }

    public Map<String, Object> metadata() {
        return metadata;
    }

    private static Map<String, Object> copy(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
