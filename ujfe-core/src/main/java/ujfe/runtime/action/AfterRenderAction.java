package ujfe.runtime.action;

/**
 * Executes after a page or component has been rendered.
 */
@FunctionalInterface
public interface AfterRenderAction {
    void execute(RenderResult result);
}
