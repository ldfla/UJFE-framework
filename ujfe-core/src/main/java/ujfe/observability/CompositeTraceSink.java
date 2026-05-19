package ujfe.observability;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Deterministic composite sink that invokes child sinks in registration order.
 */
public final class CompositeTraceSink implements TraceSink {
    private final List<TraceSink> sinks;

    private CompositeTraceSink(List<TraceSink> sinks) {
        this.sinks = List.copyOf(sinks);
    }

    public static TraceSink of(List<? extends TraceSink> sinks) {
        Objects.requireNonNull(sinks, "sinks");
        if (sinks.isEmpty()) {
            return NoopTraceSink.INSTANCE;
        }
        List<TraceSink> copiedSinks = sinks.stream()
            .map(sink -> Objects.requireNonNull(sink, "sink"))
            .collect(Collectors.toList());
        if (copiedSinks.size() == 1) {
            return copiedSinks.get(0);
        }
        return new CompositeTraceSink(copiedSinks);
    }

    @Override
    public void onRenderTrace(RenderTrace trace) {
        for (TraceSink sink : sinks) {
            sink.onRenderTrace(trace);
        }
    }

    @Override
    public void onEventTrace(EventTrace trace) {
        for (TraceSink sink : sinks) {
            sink.onEventTrace(trace);
        }
    }
}
