package ujfe.router;

import java.util.Arrays;
import java.util.Objects;

public final class PageScanner {
    public Router scan(Router router, Class<?>... candidates) {
        Objects.requireNonNull(router, "router");
        Arrays.stream(candidates)
                .filter(candidate -> candidate.isAnnotationPresent(Page.class))
                .forEach(router::register);
        return router;
    }
}
