package ujfe.router.source;

import ujfe.router.RouteDefinition;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

public final class ManualRouteSource implements RouteSource {
    private final Map<String, RouteDefinition> routes = new LinkedHashMap<>();

    public ManualRouteSource register(String path, Supplier<Object> pageFactory) {
        return register(new RouteDefinition(path, pageFactory, "manual route " + path));
    }

    public ManualRouteSource register(RouteDefinition route) {
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

    @Override
    public Collection<RouteDefinition> routes() {
        return List.copyOf(routes.values());
    }
}
