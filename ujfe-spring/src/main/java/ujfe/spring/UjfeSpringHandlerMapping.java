package ujfe.spring;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import ujfe.live.LiveHttpPaths;
import ujfe.live.StaticAssetHandler;
import ujfe.router.Router;

import java.util.Objects;

public final class UjfeSpringHandlerMapping extends AbstractHandlerMapping {
    public static final int DEFAULT_ORDER = 100;
    private static final String INTERNAL_PATH_PREFIX = "/_ujfe/";
    private final Router router;
    private final HttpRequestHandler handler;

    public UjfeSpringHandlerMapping(Router router, HttpRequestHandler handler) {
        this.router = Objects.requireNonNull(router, "router");
        this.handler = Objects.requireNonNull(handler, "handler");
        setOrder(DEFAULT_ORDER);
    }

    @Override
    protected Object getHandlerInternal(@NonNull HttpServletRequest request) {
        String path = UjfeSpringPaths.pathWithinApplication(request);
        if (isInternalPath(path)) {
            return handler;
        }
        if (StaticAssetHandler.isStaticAssetPath(path)) {
            return null;
        }
        if (!"GET".equals(request.getMethod())) {
            return null;
        }
        return router.resolve(path)
            .isPresent() ? handler : null;
    }

    private static boolean isInternalPath(String path) {
        return LiveHttpPaths.isInternalPath(path) || path.startsWith(INTERNAL_PATH_PREFIX);
    }
}
