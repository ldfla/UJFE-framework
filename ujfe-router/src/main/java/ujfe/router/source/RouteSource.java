package ujfe.router.source;

import ujfe.router.RouteDefinition;

import java.util.Collection;

public interface RouteSource {
    Collection<RouteDefinition> routes();
}
