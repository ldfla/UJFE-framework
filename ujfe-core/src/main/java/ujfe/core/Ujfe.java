package ujfe.core;

import java.util.Optional;

public final class Ujfe {
    private Ujfe() {
    }

    public static Optional<String> cookie(String name) {
        return UjfeContext.current().flatMap(context -> context.cookie(name));
    }

    public static Optional<String> localStorage(String key) {
        return UjfeContext.current().flatMap(context -> context.localStorage(key));
    }

    public static Optional<String> sessionStorage(String key) {
        return UjfeContext.current().flatMap(context -> context.sessionStorage(key));
    }
}
