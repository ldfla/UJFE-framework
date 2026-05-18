package ujfe.validation;

import java.util.*;

final class ValidationElement {
    private final String tagName;
    private final Map<String, String> attributes;
    private final List<ValidationElement> children = new ArrayList<>();
    private final List<String> textSegments = new ArrayList<>();
    private final ValidationElement parent;
    private final int typeIndex;

    ValidationElement(String tagName, Map<String, String> attributes, ValidationElement parent, int typeIndex) {
        this.tagName = Objects.requireNonNull(tagName, "tagName")
            .toLowerCase(Locale.ROOT);
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(attributes, "attributes")));
        this.parent = parent;
        this.typeIndex = typeIndex;
    }

    String tagName() {
        return tagName;
    }

    ValidationElement parent() {
        return parent;
    }

    List<ValidationElement> children() {
        return List.copyOf(children);
    }

    void addChild(ValidationElement child) {
        children.add(Objects.requireNonNull(child, "child"));
    }

    void addText(String text) {
        if (text != null && !text.isEmpty()) {
            textSegments.add(text);
        }
    }

    boolean hasAttribute(String name) {
        return attributes.containsKey(normalizeName(name));
    }

    String attribute(String name) {
        return attributes.get(normalizeName(name));
    }

    boolean attributeEquals(String name, String value) {
        String attribute = attribute(name);
        return attribute != null && attribute.equalsIgnoreCase(value);
    }

    String normalizedAttribute(String name) {
        return ValidationSupport.normalizedText(attribute(name));
    }

    boolean hasNonBlankAttribute(String name) {
        return !normalizedAttribute(name).isBlank();
    }

    boolean hasAncestor(String tagName) {
        String normalized = normalizeName(tagName);
        ValidationElement current = parent;
        while (current != null) {
            if (normalized.equals(current.tagName())) {
                return true;
            }
            current = current.parent();
        }
        return false;
    }

    boolean containsDescendant(ValidationElement target) {
        for (ValidationElement child : children) {
            if (child == target || child.containsDescendant(target)) {
                return true;
            }
        }
        return false;
    }

    String textContent() {
        if (isNonTextContainer()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (String segment : textSegments) {
            text.append(segment)
                .append(' ');
        }
        for (ValidationElement child : children) {
            text.append(child.textContent())
                .append(' ');
        }
        return ValidationSupport.normalizedText(text.toString());
    }

    String visibleTextContent() {
        if (isHiddenFromVisibleText() || isNonTextContainer()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (String segment : textSegments) {
            text.append(segment)
                .append(' ');
        }
        for (ValidationElement child : children) {
            text.append(child.visibleTextContent())
                .append(' ');
        }
        return ValidationSupport.normalizedText(text.toString());
    }

    String location() {
        if (parent == null || "#document".equals(tagName)) {
            return "#document";
        }
        Deque<String> parts = new ArrayDeque<>();
        ValidationElement current = this;
        while (current != null && current.parent() != null) {
            parts.addFirst(current.tagName() + ":nth-of-type(" + current.typeIndex + ")");
            current = current.parent();
        }
        return String.join(" > ", parts);
    }

    private boolean isHiddenFromVisibleText() {
        return hasAttribute("hidden") || attributeEquals("aria-hidden", "true");
    }

    private boolean isNonTextContainer() {
        return "script".equals(tagName) || "style".equals(tagName) || "template".equals(tagName);
    }

    private static String normalizeName(String name) {
        return Objects.requireNonNull(name, "name")
            .toLowerCase(Locale.ROOT);
    }
}
