package ujfe.runtime.action;

import ujfe.observability.RenderTrace;

@FunctionalInterface
public interface RenderTraceAction {
    void execute(RenderTrace trace) throws Exception;
}
