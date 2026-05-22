package ujfe.router;

import ujfe.core.RenderMode;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public final class RouteDefinition {
    private final String path;
    private final Supplier<Object> pageFactory;
    private final Class<?> pageType;
    private final String sourceDescription;
    private final RenderMode renderMode;

    public RouteDefinition(String path, Supplier<Object> pageFactory) {
        this(path, pageFactory, null, "manual route", RenderMode.dynamic());
    }

    public RouteDefinition(String path, Supplier<Object> pageFactory, RenderMode renderMode) {
        this(path, pageFactory, null, "manual route", renderMode);
    }

    public RouteDefinition(String path, Supplier<Object> pageFactory, String sourceDescription) {
        this(path, pageFactory, null, sourceDescription, RenderMode.dynamic());
    }

    public RouteDefinition(String path, Supplier<Object> pageFactory, String sourceDescription, RenderMode renderMode) {
        this(path, pageFactory, null, sourceDescription, renderMode);
    }

    private RouteDefinition(
        String path,
        Supplier<Object> pageFactory,
        Class<?> pageType,
        String sourceDescription,
        RenderMode renderMode
    ) {
        this.path = normalizePath(path);
        this.pageFactory = Objects.requireNonNull(pageFactory, "pageFactory");
        this.pageType = pageType;
        this.sourceDescription = sourceDescription == null || sourceDescription.isBlank()
            ? "route " + this.path
            : sourceDescription;
        this.renderMode = Objects.requireNonNull(renderMode, "renderMode");
    }

    public static RouteDefinition pageClass(String path, Class<?> pageType) {
        Objects.requireNonNull(pageType, "pageType");
        PageClassValidator.validatePageClass(pageType);
        return new RouteDefinition(
            path,
            () -> PageClassValidator.instantiate(pageType),
            pageType,
            pageType.getName(),
            RenderMode.dynamic()
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

    public RenderMode renderMode() {
        return renderMode;
    }

    public RouteDefinition withRenderMode(RenderMode renderMode) {
        return new RouteDefinition(path, pageFactory, pageType, sourceDescription, renderMode);
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
