package ujfe.runtime.action;

/**
 * Identifies the runtime phase in which an error occurred.
 */
public enum RuntimePhase {
    RENDER,
    EVENT,
    LIFECYCLE,
    HEAD_CONTRIBUTION,
    INTERNAL
}
