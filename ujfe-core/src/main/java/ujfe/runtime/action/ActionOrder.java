package ujfe.runtime.action;

/**
 * Priority level for runtime action ordering.
 *
 * <p>Actions with lower priority values execute first. Actions with equal
 * priority preserve their registration order.</p>
 */
public final class ActionOrder implements Comparable<ActionOrder> {
    public static final ActionOrder FIRST = new ActionOrder(0);
    public static final ActionOrder EARLY = new ActionOrder(250);
    public static final ActionOrder NORMAL = new ActionOrder(500);
    public static final ActionOrder LATE = new ActionOrder(750);
    public static final ActionOrder LAST = new ActionOrder(1000);

    private final int priority;

    private ActionOrder(int priority) {
        this.priority = priority;
    }

    /**
     * Creates an action order with a custom priority value.
     * Lower values execute first.
     */
    public static ActionOrder of(int priority) {
        return new ActionOrder(priority);
    }

    public int priority() {
        return priority;
    }

    @Override
    public int compareTo(ActionOrder other) {
        return Integer.compare(this.priority, other.priority);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ActionOrder)) return false;
        ActionOrder other = (ActionOrder) obj;
        return priority == other.priority;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(priority);
    }

    @Override
    public String toString() {
        return "ActionOrder(" + priority + ")";
    }
}
