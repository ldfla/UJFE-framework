package ujfe.router;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public final class RouteDefinition {
    private final String path;
    private final Supplier<Object> pageFactory;
    private final Class<?> pageType;
    private final String sourceDescription;

    public RouteDefinition(String path, Supplier<Object> pageFactory) {
        this(path, pageFactory, null, "manual route");
    }

    public RouteDefinition(String path, Supplier<Object> pageFactory, String sourceDescription) {
        this(path, pageFactory, null, sourceDescription);
    }

    private RouteDefinition(String path, Supplier<Object> pageFactory, Class<?> pageType, String sourceDescription) {
        this.path = normalizePath(path);
        this.pageFactory = Objects.requireNonNull(pageFactory, "pageFactory");
        this.pageType = pageType;
        this.sourceDescription = sourceDescription == null || sourceDescription.isBlank()
            ? "route " + this.path
            : sourceDescription;
    }

    public static RouteDefinition pageClass(String path, Class<?> pageType) {
        Objects.requireNonNull(pageType, "pageType");
        PageClassValidator.validatePageClass(pageType);
        return new RouteDefinition(
            path,
            () -> PageClassValidator.instantiate(pageType),
            pageType,
            pageType.getName()
        );
    }

    public String path() {
        return path;
    }

    public Object createPage() {
        return pageFactory.get();
    }

    public Optional<Class<?>> pageType() {
        return Optional.ofNullable(pageType);
    }

    public String sourceDescription() {
        return sourceDescription;
    }

    static String normalizePath(String path) {
        Objects.requireNonNull(path, "path");
        if (path.isBlank()) {
            throw new IllegalArgumentException("Route path cannot be blank");
        }
        String normalized = path.startsWith("/") ? path : "/" + path;
        validatePath(normalized);
        return normalized;
    }

    private static void validatePath(String path) {
        for (int index = 0; index < path.length(); index++) {
            char current = path.charAt(index);
            if (Character.isWhitespace(current)) {
                throw new IllegalArgumentException("Route path cannot contain whitespace: " + path);
            }
        }
        if (path.indexOf('?') >= 0 || path.indexOf('#') >= 0) {
            throw new IllegalArgumentException("Route path cannot contain query or fragment markers: " + path);
        }
        if (path.indexOf('<') >= 0 || path.indexOf('>') >= 0 || path.indexOf('"') >= 0 || path.indexOf('\'') >= 0) {
            throw new IllegalArgumentException("Route path contains invalid characters: " + path);
        }
    }
}
