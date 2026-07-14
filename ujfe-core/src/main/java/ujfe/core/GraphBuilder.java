package ujfe.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Declarative graph/canvas primitive for flow editors. It renders an accessible
 * SVG snapshot plus data attributes that an external CSP-safe module can
 * enhance with pan, zoom, selection, ordering, and keyboard interactions.
 */
public final class GraphBuilder implements Node {
    private final String id;
    private final List<GraphNode> nodes = new ArrayList<>();
    private final List<GraphEdge> edges = new ArrayList<>();
    private int width = 960;
    private int height = 540;
    private boolean panZoom = true;
    private boolean minimap;
    private boolean readonly;

    GraphBuilder(String id) {
        this.id = safeIdentifier(id, "id");
    }

    public GraphBuilder size(int width, int height) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Graph size must be positive");
        }
        this.width = width;
        this.height = height;
        return this;
    }

    public GraphBuilder panZoom(boolean panZoom) {
        this.panZoom = panZoom;
        return this;
    }

    public GraphBuilder minimap(boolean minimap) {
        this.minimap = minimap;
        return this;
    }

    public GraphBuilder readonly(boolean readonly) {
        this.readonly = readonly;
        return this;
    }

    public GraphBuilder node(String id, String label, int x, int y) {
        return node(id, label, "node", x, y);
    }

    public GraphBuilder node(String id, String label, String type, int x, int y) {
        nodes.add(new GraphNode(id, label, type, x, y));
        return this;
    }

    public GraphBuilder edge(String from, String to) {
        return edge(from, to, null, edges.size() + 1);
    }

    public GraphBuilder edge(String from, String to, String label, int order) {
        edges.add(new GraphEdge(from, to, label, order));
        return this;
    }

    @Override
    public String render(UjfeContext context) {
        Objects.requireNonNull(context, "context");
        Element root = UI.section()
            .id(id)
            .data("ujfe-graph-builder", "true")
            .data("ujfe-pan-zoom", Boolean.toString(panZoom))
            .data("ujfe-minimap", Boolean.toString(minimap))
            .data("ujfe-readonly", Boolean.toString(readonly));

        Element svg = UI.svg()
            .attr("viewBox", "0 0 " + width + " " + height)
            .attr("width", Integer.toString(width))
            .attr("height", Integer.toString(height))
            .role("img")
            .ariaLabel("Flow graph");
        Element edgeLayer = Element.svg("g")
            .attr("data-ujfe-graph-edges", "true");
        for (GraphEdge edge : edges) {
            edgeLayer.child(edgeElement(edge));
        }
        Element nodeLayer = Element.svg("g")
            .attr("data-ujfe-graph-nodes", "true");
        for (GraphNode node : nodes) {
            nodeLayer.child(nodeElement(node));
        }
        svg.child(edgeLayer)
            .child(nodeLayer);
        root.child(svg)
            .child(accessibleList());
        return root.render(context);
    }

    private Element nodeElement(GraphNode node) {
        Element group = Element.svg("g")
            .attr("tabindex", "0")
            .attr("role", "button")
            .attr("data-ujfe-node-id", node.id)
            .attr("data-ujfe-node-type", node.type)
            .attr("transform", "translate(" + node.x + " " + node.y + ")");
        group.child(Element.svg("rect")
            .attr("x", "-56")
            .attr("y", "-22")
            .attr("width", "112")
            .attr("height", "44")
            .attr("rx", "6"));
        group.child(Element.svg("text")
            .attr("text-anchor", "middle")
            .attr("dominant-baseline", "middle")
            .child(node.label));
        return group;
    }

    private Element edgeElement(GraphEdge edge) {
        GraphNode from = findNode(edge.from);
        GraphNode to = findNode(edge.to);
        Element group = Element.svg("g")
            .attr("data-ujfe-edge-from", edge.from)
            .attr("data-ujfe-edge-to", edge.to)
            .attr("data-ujfe-edge-order", Integer.toString(edge.order));
        if (from != null && to != null) {
            group.child(Element.svg("line")
                .attr("x1", Integer.toString(from.x))
                .attr("y1", Integer.toString(from.y))
                .attr("x2", Integer.toString(to.x))
                .attr("y2", Integer.toString(to.y)));
            if (edge.label != null) {
                group.child(Element.svg("text")
                    .attr("x", Integer.toString((from.x + to.x) / 2))
                    .attr("y", Integer.toString((from.y + to.y) / 2))
                    .child(edge.label));
            }
        }
        return group;
    }

    private GraphNode findNode(String id) {
        for (GraphNode node : nodes) {
            if (node.id.equals(id)) {
                return node;
            }
        }
        return null;
    }

    private Element accessibleList() {
        Element list = UI.ol()
            .data("ujfe-graph-list", "true");
        for (GraphNode node : nodes) {
            list.child(UI.li()
                .data("ujfe-node-id", node.id)
                .child(node.label + " (" + node.type + ")"));
        }
        return list;
    }

    private static String safeIdentifier(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        String trimmed = value.trim();
        for (int index = 0; index < trimmed.length(); index++) {
            char current = trimmed.charAt(index);
            if (!(Character.isLetterOrDigit(current) || current == '-' || current == '_' || current == ':')) {
                throw new IllegalArgumentException(name + " contains invalid characters");
            }
        }
        return trimmed;
    }

    private static final class GraphNode {
        private final String id;
        private final String label;
        private final String type;
        private final int x;
        private final int y;

        private GraphNode(String id, String label, String type, int x, int y) {
            this.id = safeIdentifier(id, "node id");
            this.label = requireText(label, "label");
            this.type = safeIdentifier(type, "type");
            this.x = x;
            this.y = y;
        }
    }

    private static final class GraphEdge {
        private final String from;
        private final String to;
        private final String label;
        private final int order;

        private GraphEdge(String from, String to, String label, int order) {
            if (order < 1) {
                throw new IllegalArgumentException("edge order must be greater than zero");
            }
            this.from = safeIdentifier(from, "from");
            this.to = safeIdentifier(to, "to");
            this.label = label == null || label.isBlank() ? null : label.trim();
            this.order = order;
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value.trim();
    }
}
