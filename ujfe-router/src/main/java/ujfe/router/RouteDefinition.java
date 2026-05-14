package ujfe.router;

import java.util.Objects;
import java.util.function.Supplier;

public final class RouteDefinition {
    private final String path;
    private final Supplier<Object> pageFactory;

    public RouteDefinition(String path, Supplier<Object> pageFactory) {
        this.path = normalizePath(path);
        this.pageFactory = Objects.requireNonNull(pageFactory, "pageFactory");
    }

    public String path() {
        return path;
    }

    public Object createPage() {
        return pageFactory.get();
    }

    static String normalizePath(String path) {
        Objects.requireNonNull(path, "path");
        if (path.isBlank()) {
            throw new IllegalArgumentException("Route path cannot be blank");
        }
        return path.startsWith("/") ? path : "/" + path;
    }
}
