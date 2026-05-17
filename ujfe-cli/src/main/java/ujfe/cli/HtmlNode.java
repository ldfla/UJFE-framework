package ujfe.cli;

import java.util.*;

final class HtmlNode {
    private final String tagName;
    private final String text;
    private final Map<String, String> attributes;
    private final List<HtmlNode> children;

    private HtmlNode(String tagName, String text, Map<String, String> attributes, List<HtmlNode> children) {
        this.tagName = tagName;
        this.text = text;
        this.attributes = attributes;
        this.children = children;
    }

    static HtmlNode element(String tagName, Map<String, String> attributes) {
        return new HtmlNode(
                Objects.requireNonNull(tagName, "tagName"),
                null,
                new LinkedHashMap<>(Objects.requireNonNull(attributes, "attributes")),
                new ArrayList<>()
        );
    }

    static HtmlNode text(String text) {
        return new HtmlNode(null, Objects.requireNonNull(text, "text"), Map.of(), List.of());
    }

    boolean textNode() {
        return text != null;
    }

    String tagName() {
        return tagName;
    }

    String text() {
        return text;
    }

    Map<String, String> attributes() {
        return Collections.unmodifiableMap(attributes);
    }

    List<HtmlNode> children() {
        return Collections.unmodifiableList(children);
    }

    void addChild(HtmlNode child) {
        children.add(Objects.requireNonNull(child, "child"));
    }
}
