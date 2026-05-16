package ujfe.router;

import ujfe.core.Component;
import ujfe.core.Node;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

public final class PageRenderer {
    public Node render(RouteDefinition route) {
        Objects.requireNonNull(route, "route");
        return render(route.createPage());
    }

    public Node render(Object page) {
        Objects.requireNonNull(page, "page");
        if (page instanceof Component) {
            return ((Component) page).render();
        }
        return renderViaMethod(page);
    }

    private Node renderViaMethod(Object page) {
        try {
            Method render = page.getClass().getMethod("render");
            Object result = render.invoke(page);
            if (!(result instanceof Node)) {
                throw new IllegalStateException("render() must return ujfe.core.Node on " + page.getClass().getName());
            }
            return (Node) result;
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("Page must implement Component or expose public render()", exception);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Cannot access render() on " + page.getClass().getName(), exception);
        } catch (InvocationTargetException exception) {
            Throwable target = exception.getTargetException();
            if (target instanceof RuntimeException) {
                throw (RuntimeException) target;
            }
            throw new IllegalStateException("render() failed on " + page.getClass().getName(), target);
        }
    }
}
