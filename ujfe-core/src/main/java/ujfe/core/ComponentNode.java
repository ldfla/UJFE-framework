package ujfe.core;

import java.util.Objects;

/**
 * Node wrapper that renders a component through the active UJFE runtime context.
 *
 * <p>When a component also implements {@link Lifecycle}, the active
 * lifecycle tracker can observe it before rendering. This keeps nested
 * component lifecycle detection explicit and runtime-agnostic.</p>
 */
public final class ComponentNode implements Node {
    private final Component component;

    public ComponentNode(Component component) {
        this.component = Objects.requireNonNull(component, "component");
    }

    public Component component() {
        return component;
    }

    @Override
    public String render(UjfeContext context) {
        Objects.requireNonNull(context, "context");
        context.trackLifecycle(component);
        Node rendered = Objects.requireNonNull(component.render(), "component.render()");
        return rendered.render(context);
    }
}
