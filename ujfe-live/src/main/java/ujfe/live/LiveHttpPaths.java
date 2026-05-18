package ujfe.live;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class LiveHttpPaths {
    public static final String CLIENT_SCRIPT = "/_ujfe/client.js";
    public static final String DEV_SCRIPT = "/_ujfe/dev.js";
    public static final String CSS = "/_ujfe/css";
    public static final String EVENT = "/_ujfe/event";
    public static final String STATE = "/_ujfe/state";

    private static final Set<String> INTERNAL_PATHS = Collections.unmodifiableSet(new LinkedHashSet<>(List.of(
        CLIENT_SCRIPT,
        DEV_SCRIPT,
        CSS,
        EVENT,
        STATE
    )));

    private LiveHttpPaths() {
    }

    public static Set<String> internalPaths() {
        return new LinkedHashSet<>(INTERNAL_PATHS);
    }

    public static boolean isInternalPath(String path) {
        return INTERNAL_PATHS.contains(path);
    }
}
