package ujfe.core;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * Route-level rendering and cache metadata.
 *
 * <p>The runtime can use this value to decide whether a route is always
 * dynamic, can be cached, can be pre-rendered as a shell, or should be
 * revalidated by an application event. It intentionally stores policy only;
 * authorization and invalidation remain application/runtime responsibilities.</p>
 */
public final class RenderMode {
    public enum Kind {
        DYNAMIC,
        STATIC,
        CACHED,
        STATIC_SHELL,
        STALE_WHILE_REVALIDATE
    }

    public enum CacheScope {
        PUBLIC("public"),
        PRIVATE("private"),
        NO_STORE("no-store");

        private final String directive;

        CacheScope(String directive) {
            this.directive = directive;
        }

        String directive() {
            return directive;
        }
    }

    private final Kind kind;
    private final CacheScope cacheScope;
    private final Duration maxAge;
    private final Duration staleWhileRevalidate;
    private final String revalidateEvent;

    private RenderMode(
        Kind kind,
        CacheScope cacheScope,
        Duration maxAge,
        Duration staleWhileRevalidate,
        String revalidateEvent
    ) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.cacheScope = Objects.requireNonNull(cacheScope, "cacheScope");
        this.maxAge = normalizeDuration(maxAge, "maxAge");
        this.staleWhileRevalidate = normalizeDuration(staleWhileRevalidate, "staleWhileRevalidate");
        this.revalidateEvent = normalizeEvent(revalidateEvent);
    }

    public static RenderMode dynamic() {
        return new RenderMode(Kind.DYNAMIC, CacheScope.NO_STORE, Duration.ZERO, Duration.ZERO, null);
    }

    public static RenderMode staticPage(Duration maxAge) {
        return new RenderMode(Kind.STATIC, CacheScope.PUBLIC, maxAge, Duration.ZERO, null);
    }

    public static RenderMode cached(Duration maxAge) {
        return new RenderMode(Kind.CACHED, CacheScope.PRIVATE, maxAge, Duration.ZERO, null);
    }

    public static RenderMode staticShell(Duration maxAge) {
        return new RenderMode(Kind.STATIC_SHELL, CacheScope.PRIVATE, maxAge, Duration.ZERO, null);
    }

    public static RenderMode staleWhileRevalidate(Duration maxAge, Duration staleWhileRevalidate) {
        return new RenderMode(Kind.STALE_WHILE_REVALIDATE, CacheScope.PUBLIC, maxAge, staleWhileRevalidate, null);
    }

    public RenderMode scope(CacheScope cacheScope) {
        return new RenderMode(kind, cacheScope, maxAge, staleWhileRevalidate, revalidateEvent);
    }

    public RenderMode revalidateOn(String eventName) {
        return new RenderMode(kind, cacheScope, maxAge, staleWhileRevalidate, eventName);
    }

    public Kind kind() {
        return kind;
    }

    public CacheScope cacheScope() {
        return cacheScope;
    }

    public Duration maxAge() {
        return maxAge;
    }

    public Duration staleWhileRevalidate() {
        return staleWhileRevalidate;
    }

    public Optional<String> revalidateEvent() {
        return Optional.ofNullable(revalidateEvent);
    }

    public String cacheControlHeader() {
        if (cacheScope == CacheScope.NO_STORE || kind == Kind.DYNAMIC) {
            return "no-store";
        }

        StringBuilder header = new StringBuilder(cacheScope.directive())
            .append(", max-age=")
            .append(maxAge.getSeconds());
        if (!staleWhileRevalidate.isZero()) {
            header.append(", stale-while-revalidate=")
                .append(staleWhileRevalidate.getSeconds());
        }
        return header.toString();
    }

    private static Duration normalizeDuration(Duration duration, String name) {
        Objects.requireNonNull(duration, name);
        if (duration.isNegative()) {
            throw new IllegalArgumentException(name + " cannot be negative");
        }
        return duration;
    }

    private static String normalizeEvent(String eventName) {
        if (eventName == null || eventName.isBlank()) {
            return null;
        }
        String trimmed = eventName.trim();
        for (int index = 0; index < trimmed.length(); index++) {
            char current = trimmed.charAt(index);
            if (!(Character.isLetterOrDigit(current) || current == '.' || current == '_' || current == '-' || current == ':')) {
                throw new IllegalArgumentException("Invalid render revalidation event: " + eventName);
            }
        }
        return trimmed;
    }
}
