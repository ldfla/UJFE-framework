package ujfe.runtime.action;

/**
 * Identifies the runtime phase in which an error occurred.
 */
public enum RuntimePhase {
    RENDER,
    EVENT,
    STATE,
    ROUTING,
    ADAPTER,
    LIFECYCLE,
    HEAD_CONTRIBUTION,
    INTERNAL
}
