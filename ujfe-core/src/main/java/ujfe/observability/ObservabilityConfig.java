package ujfe.observability;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Safe, dependency-free observability configuration.
 */
public final class ObservabilityConfig {
    private final boolean renderTracesEnabled;
    private final boolean eventTracesEnabled;
    private final TraceSink traceSink;
    private final Clock clock;

    private ObservabilityConfig(Builder builder) {
        this.renderTracesEnabled = builder.renderTracesEnabled;
        this.eventTracesEnabled = builder.eventTracesEnabled;
        this.traceSink = CompositeTraceSink.of(builder.traceSinks);
        this.clock = Objects.requireNonNull(builder.clock, "clock");
    }

    public static ObservabilityConfig defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean renderTracesEnabled() {
        return renderTracesEnabled;
    }

    public boolean eventTracesEnabled() {
        return eventTracesEnabled;
    }

    public TraceSink traceSink() {
        return traceSink;
    }

    public Clock clock() {
        return clock;
    }

    public static final class Builder {
        private boolean renderTracesEnabled = true;
        private boolean eventTracesEnabled = true;
        private final List<TraceSink> traceSinks = new ArrayList<>();
        private Clock clock = Clock.systemUTC();

        private Builder() {
        }

        public Builder renderTracesEnabled(boolean renderTracesEnabled) {
            this.renderTracesEnabled = renderTracesEnabled;
            return this;
        }

        public Builder eventTracesEnabled(boolean eventTracesEnabled) {
            this.eventTracesEnabled = eventTracesEnabled;
            return this;
        }

        public Builder traceSink(TraceSink traceSink) {
            this.traceSinks.clear();
            this.traceSinks.add(Objects.requireNonNull(traceSink, "traceSink"));
            return this;
        }

        public Builder addTraceSink(TraceSink traceSink) {
            this.traceSinks.add(Objects.requireNonNull(traceSink, "traceSink"));
            return this;
        }

        public Builder clock(Clock clock) {
            this.clock = Objects.requireNonNull(clock, "clock");
            return this;
        }

        public ObservabilityConfig build() {
            if (traceSinks.isEmpty()) {
                traceSinks.add(NoopTraceSink.INSTANCE);
            }
            return new ObservabilityConfig(this);
        }
    }
}
