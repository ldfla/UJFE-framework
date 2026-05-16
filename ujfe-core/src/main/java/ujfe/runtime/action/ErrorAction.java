package ujfe.runtime.action;

/**
 * Executes when a runtime error occurs during rendering, event
 * processing, or head contribution.
 */
@FunctionalInterface
public interface ErrorAction {
    void execute(RuntimeErrorContext context);
}
