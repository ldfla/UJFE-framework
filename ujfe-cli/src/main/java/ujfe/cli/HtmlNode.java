package ujfe.cli;

import java.util.*;

final class HtmlNode {
    enum Kind {
        ELEMENT,
        TEXT,
        UNSAFE
    }

    private final Kind kind;
    private final String tagName;
    private final String text;
    private final String unsafeHtml;
    private final String unsafeReason;
    private final Map<String, String> attributes;
    private final List<HtmlNode> children;

    private HtmlNode(
        Kind kind,
        String tagName,
        String text,
        String unsafeHtml,
        String unsafeReason,
        Map<String, String> attributes,
        List<HtmlNode> children
    ) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.tagName = tagName;
        this.text = text;
        this.unsafeHtml = unsafeHtml;
        this.unsafeReason = unsafeReason;
        this.attributes = attributes;
        this.children = children;
    }

    static HtmlNode element(String tagName, Map<String, String> attributes) {
        return new HtmlNode(
            Kind.ELEMENT,
            Objects.requireNonNull(tagName, "tagName"),
            null,
            null,
            null,
            new LinkedHashMap<>(Objects.requireNonNull(attributes, "attributes")),
            new ArrayList<>()
        );
    }

    static HtmlNode text(String text) {
        return new HtmlNode(Kind.TEXT, null, Objects.requireNonNull(text, "text"), null, null, Map.of(), List.of());
    }

    static HtmlNode unsafe(String html, String reason) {
        return new HtmlNode(
            Kind.UNSAFE,
            null,
            null,
            Objects.requireNonNull(html, "html"),
            Objects.requireNonNull(reason, "reason"),
            Map.of(),
            List.of()
        );
    }

    Kind kind() {
        return kind;
    }

    boolean textNode() {
        return kind == Kind.TEXT;
    }

    boolean elementNode() {
        return kind == Kind.ELEMENT;
    }

    boolean unsafeNode() {
        return kind == Kind.UNSAFE;
    }

    String tagName() {
        return tagName;
    }

    String text() {
        return text;
    }

    String unsafeHtml() {
        return unsafeHtml;
    }

    String unsafeReason() {
        return unsafeReason;
    }

    Map<String, String> attributes() {
        return Collections.unmodifiableMap(attributes);
    }

    List<HtmlNode> children() {
        return Collections.unmodifiableList(children);
    }

    void addChild(HtmlNode child) {
        if (!elementNode()) {
            throw new IllegalStateException("Only element nodes can have children");
        }
        children.add(Objects.requireNonNull(child, "child"));
    }
}
