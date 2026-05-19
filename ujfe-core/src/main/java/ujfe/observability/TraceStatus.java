package ujfe.observability;

/**
 * Stable status values for UJFE render and event traces.
 */
public enum TraceStatus {
    SUCCESS,
    CLIENT_ERROR,
    SERVER_ERROR,
    NOT_FOUND,
    FORBIDDEN,
    RATE_LIMITED,
    VALIDATION_FAILED,
    ERROR
}
