package ujfe.runtime.action;

/**
 * Executes before a page or component is rendered.
 */
@FunctionalInterface
public interface BeforeRenderAction {
    void execute(RenderContext context);
}
