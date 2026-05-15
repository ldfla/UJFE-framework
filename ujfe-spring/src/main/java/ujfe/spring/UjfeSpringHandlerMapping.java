package ujfe.spring;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import ujfe.router.Router;

import java.util.Objects;
import java.util.Set;

public final class UjfeSpringHandlerMapping extends AbstractHandlerMapping {
    public static final int DEFAULT_ORDER = 100;
    private static final Set<String> INTERNAL_PATHS = Set.of(
            "/_ujfe/client.js",
            "/_ujfe/dev.js",
            "/_ujfe/css",
            "/_ujfe/event",
            "/_ujfe/state"
    );

    private final Router router;
    private final HttpRequestHandler handler;

    public UjfeSpringHandlerMapping(Router router, HttpRequestHandler handler) {
        this.router = Objects.requireNonNull(router, "router");
        this.handler = Objects.requireNonNull(handler, "handler");
        setOrder(DEFAULT_ORDER);
    }

    @Override
    protected Object getHandlerInternal(HttpServletRequest request) {
        String path = UjfeSpringPaths.pathWithinApplication(request);
        if (INTERNAL_PATHS.contains(path)) {
            return handler;
        }
        if (!"GET".equals(request.getMethod())) {
            return null;
        }
        return router.resolve(path).isPresent() ? handler : null;
    }
}
