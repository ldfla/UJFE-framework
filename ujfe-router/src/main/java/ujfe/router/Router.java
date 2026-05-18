package ujfe.router;

import ujfe.router.source.RouteSource;

import java.util.*;
import java.util.function.Supplier;

public final class Router {
    private final Map<String, RouteDefinition> routes = new LinkedHashMap<>();

    public Router register(Object pageInstance) {
        Objects.requireNonNull(pageInstance, "pageInstance");
        PageClassValidator.validateAnnotatedPageInstanceClass(pageInstance.getClass());
        Page page = pageInstance.getClass()
            .getAnnotation(Page.class);
        return register(new RouteDefinition(page.value(), () -> pageInstance, pageInstance.getClass()
            .getName()));
    }

    public Router register(Class<?> pageType) {
        Objects.requireNonNull(pageType, "pageType");
        PageClassValidator.validateAnnotatedPageClass(pageType);
        Page page = pageType.getAnnotation(Page.class);
        return register(RouteDefinition.pageClass(page.value(), pageType));
    }

    public Router register(String path, Supplier<Object> pageFactory) {
        return register(new RouteDefinition(path, pageFactory));
    }

    public Router register(RouteDefinition route) {
        Objects.requireNonNull(route, "route");
        RouteDefinition previous = routes.get(route.path());
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate route path '" + route.path()
                + "' for " + previous.sourceDescription()
                + " and " + route.sourceDescription());
        }
        routes.put(route.path(), route);
        return this;
    }

    public Router register(RouteSource source) {
        Objects.requireNonNull(source, "source");
        for (RouteDefinition route : source.routes()) {
            register(route);
        }
        return this;
    }

    public Router register(RouteSource... sources) {
        Objects.requireNonNull(sources, "sources");
        for (RouteSource source : sources) {
            register(source);
        }
        return this;
    }

    public Optional<RouteDefinition> resolve(String path) {
        return Optional.ofNullable(routes.get(RouteDefinition.normalizePath(path)));
    }

    public Collection<RouteDefinition> routes() {
        return List.copyOf(routes.values());
    }
}
