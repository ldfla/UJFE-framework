package ujfe.runtime.action;

import ujfe.core.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Mutable collector for document {@code <head>} contributions.
 *
 * <p>Actions use {@link #add(Node)} to contribute meta, link, style,
 * script, or other nodes. The collector preserves insertion order.</p>
 */
public final class HeadContributionContext {
    private final List<Node> nodes = new ArrayList<>();

    /**
     * Adds a node to the document head.
     */
    public void add(Node node) {
        nodes.add(Objects.requireNonNull(node, "node"));
    }

    /**
     * Returns an unmodifiable view of the contributed nodes in
     * insertion order.
     */
    public List<Node> nodes() {
        return List.copyOf(nodes);
    }
}
