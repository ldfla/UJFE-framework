package ujfe.router;

import ujfe.core.Component;
import ujfe.core.Node;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

public final class PageClassValidator {
    private PageClassValidator() {
    }

    public static void validateAnnotatedPageClass(Class<?> pageType) {
        Objects.requireNonNull(pageType, "pageType");
        if (!pageType.isAnnotationPresent(Page.class)) {
            throw new RouteDiscoveryException("Missing @Page annotation on " + pageType.getName());
        }
        validatePageClass(pageType);
    }

    public static void validateAnnotatedPageInstanceClass(Class<?> pageType) {
        Objects.requireNonNull(pageType, "pageType");
        if (!pageType.isAnnotationPresent(Page.class)) {
            throw new RouteDiscoveryException("Missing @Page annotation on " + pageType.getName());
        }
        validatePageShape(pageType);
    }

    public static void validatePageClass(Class<?> pageType) {
        Objects.requireNonNull(pageType, "pageType");
        validatePageShape(pageType);
        validateNoArgConstructor(pageType);
    }

    private static void validatePageShape(Class<?> pageType) {
        if (pageType.isInterface()) {
            throw new RouteDiscoveryException("Page type cannot be an interface: " + pageType.getName());
        }
        if (Modifier.isAbstract(pageType.getModifiers())) {
            throw new RouteDiscoveryException("Page type cannot be abstract: " + pageType.getName());
        }
        validateRenderMethod(pageType);
    }

    public static Object instantiate(Class<?> pageType) {
        validatePageClass(pageType);
        try {
            Constructor<?> constructor = pageType.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new RouteDiscoveryException("Could not instantiate page " + pageType.getName(), exception);
        }
    }

    private static void validateRenderMethod(Class<?> pageType) {
        if (Component.class.isAssignableFrom(pageType)) {
            return;
        }

        Method render;
        try {
            render = pageType.getMethod("render");
        } catch (NoSuchMethodException exception) {
            throw new RouteDiscoveryException(
                "Page must implement Component or expose public render(): " + pageType.getName(), exception);
        }

        if (render.getParameterCount() != 0) {
            throw new RouteDiscoveryException("render() must not declare parameters on " + pageType.getName());
        }
        if (!Node.class.isAssignableFrom(render.getReturnType())) {
            throw new RouteDiscoveryException(
                "render() must return ujfe.core.Node on " + pageType.getName());
        }
    }

    private static void validateNoArgConstructor(Class<?> pageType) {
        try {
            pageType.getDeclaredConstructor();
        } catch (NoSuchMethodException exception) {
            throw new RouteDiscoveryException(
                "Page must expose a no-argument constructor for reflective route discovery: "
                    + pageType.getName(),
                exception);
        }
    }
}
