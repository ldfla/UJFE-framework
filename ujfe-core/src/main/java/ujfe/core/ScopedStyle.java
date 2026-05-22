package ujfe.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * External scoped style declaration for a Java-first component.
 */
public final class ScopedStyle implements Node {
    private final String componentId;
    private final Map<String, String> tokens = new LinkedHashMap<>();
    private String href;

    ScopedStyle(String componentId) {
        this.componentId = BrowserApiBridge.safeName(componentId);
        this.href = "/assets/ujfe/components/" + this.componentId + ".css";
    }

    public ScopedStyle href(String href) {
        this.href = Objects.requireNonNull(href, "href");
        return this;
    }

    public ScopedStyle token(String name, String value) {
        tokens.put(BrowserApiBridge.safeName(name), BrowserApiBridge.requireText(value, "value"));
        return this;
    }

    public String className(String localName) {
        return componentId + "__" + BrowserApiBridge.safeName(localName);
    }

    @Override
    public String render(UjfeContext context) {
        Objects.requireNonNull(context, "context");
        Element link = UI.link()
            .attr("rel", "stylesheet")
            .href(href)
            .data("ujfe-scoped-style", componentId);
        if (!tokens.isEmpty()) {
            link.data("ujfe-style-tokens", tokensJson());
        }
        return link.render(context);
    }

    private String tokensJson() {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> token : tokens.entrySet()) {
            if (!first) {
                json.append(',');
            }
            json.append('"')
                .append(UiContract.escape(token.getKey()))
                .append("\":\"")
                .append(UiContract.escape(token.getValue()))
                .append('"');
            first = false;
        }
        json.append('}');
        return json.toString();
    }
}
