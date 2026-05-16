package ujfe.runtime.action;

/**
 * Executes after a live event has been dispatched.
 */
@FunctionalInterface
public interface AfterEventAction {
    void execute(LiveEventResult result);
}
