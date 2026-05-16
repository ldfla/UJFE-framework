package ujfe.runtime.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An ordered, immutable chain of actions of a single type.
 *
 * <p>Actions are sorted by their {@link ActionOrder} priority. Actions
 * with equal priority preserve their registration order.</p>
 *
 * @param <A> the action type
 */
public final class ActionChain<A> {
    private final List<OrderedAction<A>> actions;

    private ActionChain(List<OrderedAction<A>> actions) {
        List<OrderedAction<A>> sorted = new ArrayList<>(actions);
        Collections.sort(sorted);
        this.actions = List.copyOf(sorted);
    }

    static <A> ActionChain<A> of(List<OrderedAction<A>> actions) {
        return new ActionChain<>(actions);
    }

    /**
     * Returns the ordered actions in this chain.
     */
    public List<A> actions() {
        List<A> orderedActions = new ArrayList<>(actions.size());
        for (OrderedAction<A> orderedAction : actions) {
            orderedActions.add(orderedAction.action());
        }
        return List.copyOf(orderedActions);
    }

    /**
     * Returns true if this chain has no actions.
     */
    public boolean isEmpty() {
        return actions.isEmpty();
    }

    static final class OrderedAction<A> implements Comparable<OrderedAction<A>> {
        private final ActionOrder order;
        private final int registrationIndex;
        private final A action;

        OrderedAction(ActionOrder order, int registrationIndex, A action) {
            this.order = Objects.requireNonNull(order, "order");
            this.registrationIndex = registrationIndex;
            this.action = Objects.requireNonNull(action, "action");
        }

        A action() {
            return action;
        }

        @Override
        public int compareTo(OrderedAction<A> other) {
            int result = order.compareTo(other.order);
            if (result != 0) return result;
            return Integer.compare(registrationIndex, other.registrationIndex);
        }
    }
}
