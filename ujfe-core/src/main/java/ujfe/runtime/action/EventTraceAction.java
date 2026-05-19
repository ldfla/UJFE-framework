package ujfe.runtime.action;

import ujfe.observability.EventTrace;

@FunctionalInterface
public interface EventTraceAction {
    void execute(EventTrace trace) throws Exception;
}
