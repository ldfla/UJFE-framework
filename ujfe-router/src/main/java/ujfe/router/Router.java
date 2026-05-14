package ujfe.router;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public final class Router {
    private final Map<String, RouteDefinition> routes = new LinkedHashMap<>();

    public Router register(Object pageInstance) {
        Objects.requireNonNull(pageInstance, "pageInstance");
        Page page = pageInstance.getClass().getAnnotation(Page.class);
        if (page == null) {
            throw new IllegalArgumentException("Missing @Page annotation on " + pageInstance.getClass().getName());
        }
        return register(page.value(), () -> pageInstance);
    }

    public Router register(Class<?> pageType) {
        Objects.requireNonNull(pageType, "pageType");
        Page page = pageType.getAnnotation(Page.class);
        if (page == null) {
            throw new IllegalArgumentException("Missing @Page annotation on " + pageType.getName());
        }
        return register(page.value(), () -> instantiate(pageType));
    }

    public Router register(String path, Supplier<Object> pageFactory) {
        RouteDefinition route = new RouteDefinition(path, pageFactory);
        routes.put(route.path(), route);
        return this;
    }

    public Optional<RouteDefinition> resolve(String path) {
        return Optional.ofNullable(routes.get(RouteDefinition.normalizePath(path)));
    }

    public Collection<RouteDefinition> routes() {
        return routes.values();
    }

    private static Object instantiate(Class<?> pageType) {
        try {
            Constructor<?> constructor = pageType.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not instantiate page " + pageType.getName(), exception);
        }
    }
}
