package ujfe.runtime.action;

/**
 * Executes before a live event is dispatched.
 */
@FunctionalInterface
public interface BeforeEventAction {
    void execute(LiveEventContext context);
}
