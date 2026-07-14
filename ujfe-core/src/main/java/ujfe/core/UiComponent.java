package ujfe.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Co-located Java component unit for markup, props, scoped CSS, contracts, and
 * client module metadata.
 */
public final class UiComponent implements Node {
    private final String componentId;
    private final List<Node> children = new ArrayList<>();
    private Object props;
    private ScopedStyle style;
    private ClientModule clientModule;
    private UiContract contract;
    private String cssClasses;

    UiComponent(String componentId) {
        this.componentId = BrowserApiBridge.safeName(componentId);
    }

    public UiComponent props(Object props) {
        this.props = props;
        return this;
    }

    public UiComponent style(ScopedStyle style) {
        this.style = Objects.requireNonNull(style, "style");
        return this;
    }

    public UiComponent clientModule(ClientModule clientModule) {
        this.clientModule = Objects.requireNonNull(clientModule, "clientModule");
        return this;
    }

    public UiComponent contract(UiContract contract) {
        this.contract = Objects.requireNonNull(contract, "contract");
        return this;
    }

    public UiComponent css(String classes) {
        this.cssClasses = classes;
        return this;
    }

    public UiComponent child(Node child) {
        children.add(Objects.requireNonNull(child, "child"));
        return this;
    }

    public UiComponent children(Node... nodes) {
        Objects.requireNonNull(nodes, "nodes");
        for (Node node : nodes) {
            child(node);
        }
        return this;
    }

    @Override
    public String render(UjfeContext context) {
        Objects.requireNonNull(context, "context");
        Element root = UI.section()
            .data("ujfe-component", componentId);
        if (props != null) {
            root.data("ujfe-props-type", props.getClass()
                .getName());
        }
        if (cssClasses != null) {
            root.css(cssClasses);
        }
        if (style != null) {
            root.child(style);
        }
        if (contract != null) {
            root.child(contract);
        }
        for (Node child : children) {
            root.child(child);
        }
        if (clientModule != null) {
            root.child(clientModule);
        }
        return root.render(context);
    }
}
