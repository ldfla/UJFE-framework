package ujfe.spring;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import ujfe.live.LiveHttpPaths;
import ujfe.router.Router;

import java.util.Objects;

public final class UjfeSpringHandlerMapping extends AbstractHandlerMapping {
    public static final int DEFAULT_ORDER = 100;
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
        if (LiveHttpPaths.isInternalPath(path)) {
            return handler;
        }
        if (!"GET".equals(request.getMethod())) {
            return null;
        }
        return router.resolve(path)
            .isPresent() ? handler : null;
    }
}
