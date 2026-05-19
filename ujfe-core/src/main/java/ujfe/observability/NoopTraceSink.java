package ujfe.observability;

/**
 * No-op trace sink used by default.
 */
public final class NoopTraceSink implements TraceSink {
    public static final NoopTraceSink INSTANCE = new NoopTraceSink();

    private NoopTraceSink() {
    }
}
