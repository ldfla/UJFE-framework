package ujfe.observability;

/**
 * Adapter-neutral sink for completed UJFE traces.
 */
public interface TraceSink {
    default void onRenderTrace(RenderTrace trace) {
    }

    default void onEventTrace(EventTrace trace) {
    }
}
