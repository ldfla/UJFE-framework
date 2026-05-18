package ujfe.validation;

import java.util.*;

final class HtmlValidationParser {
    private static final Set<String> VOID_ELEMENTS = Set.of(
        "area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta",
        "param", "source", "track", "wbr"
    );

    private HtmlValidationParser() {
    }

    static ValidationDocument parse(String html) {
        Objects.requireNonNull(html, "html");
        ValidationElement root = new ValidationElement("#document", Map.of(), null, 1);
        Deque<ValidationElement> stack = new ArrayDeque<>();
        List<ValidationElement> elements = new ArrayList<>();
        stack.push(root);

        int index = 0;
        while (index < html.length()) {
            int tagStart = html.indexOf('<', index);
            if (tagStart < 0) {
                stack.peek()
                    .addText(html.substring(index));
                break;
            }
            if (tagStart > index) {
                stack.peek()
                    .addText(html.substring(index, tagStart));
            }

            if (startsWith(html, tagStart, "<!--")) {
                index = skipUntil(html, tagStart + 4, "-->");
                continue;
            }
            if (startsWith(html, tagStart, "<!")) {
                index = skipUntil(html, tagStart + 2, ">");
                continue;
            }
            if (startsWith(html, tagStart, "</")) {
                int tagEnd = html.indexOf('>', tagStart + 2);
                if (tagEnd < 0) {
                    break;
                }
                closeElement(stack, closingTagName(html.substring(tagStart + 2, tagEnd)));
                index = tagEnd + 1;
                continue;
            }

            int tagEnd = findTagEnd(html, tagStart + 1);
            if (tagEnd < 0) {
                stack.peek()
                    .addText(html.substring(tagStart));
                break;
            }
            StartTag tag = parseStartTag(html.substring(tagStart + 1, tagEnd));
            if (tag.name.isEmpty()) {
                index = tagEnd + 1;
                continue;
            }

            ValidationElement parent = stack.peek();
            ValidationElement element = new ValidationElement(
                tag.name,
                tag.attributes,
                parent,
                nextTypeIndex(parent, tag.name)
            );
            parent.addChild(element);
            elements.add(element);
            if (!tag.selfClosing && !VOID_ELEMENTS.contains(tag.name)) {
                stack.push(element);
            }
            index = tagEnd + 1;
        }
        return new ValidationDocument(root, elements);
    }

    private static int nextTypeIndex(ValidationElement parent, String tagName) {
        int count = 1;
        for (ValidationElement child : parent.children()) {
            if (tagName.equals(child.tagName())) {
                count++;
            }
        }
        return count;
    }

    private static void closeElement(Deque<ValidationElement> stack, String tagName) {
        if (tagName.isEmpty()) {
            return;
        }
        while (stack.size() > 1) {
            ValidationElement current = stack.pop();
            if (tagName.equals(current.tagName())) {
                return;
            }
        }
    }

    private static StartTag parseStartTag(String source) {
        String trimmed = source.trim();
        boolean selfClosing = trimmed.endsWith("/");
        if (selfClosing) {
            trimmed = trimmed.substring(0, trimmed.length() - 1)
                .trim();
        }

        int index = 0;
        while (index < trimmed.length() && !Character.isWhitespace(trimmed.charAt(index))) {
            index++;
        }
        String name = trimmed.substring(0, index)
            .toLowerCase(Locale.ROOT);
        Map<String, String> attributes = parseAttributes(trimmed, index);
        return new StartTag(name, attributes, selfClosing);
    }

    private static Map<String, String> parseAttributes(String source, int index) {
        Map<String, String> attributes = new LinkedHashMap<>();
        while (index < source.length()) {
            while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
                index++;
            }
            if (index >= source.length()) {
                break;
            }

            int nameStart = index;
            while (index < source.length()
                && !Character.isWhitespace(source.charAt(index))
                && source.charAt(index) != '=') {
                index++;
            }
            String name = source.substring(nameStart, index)
                .toLowerCase(Locale.ROOT);
            while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
                index++;
            }
            String value = null;
            if (index < source.length() && source.charAt(index) == '=') {
                index++;
                while (index < source.length() && Character.isWhitespace(source.charAt(index))) {
                    index++;
                }
                AttributeValue parsed = parseAttributeValue(source, index);
                value = parsed.value;
                index = parsed.nextIndex;
            }
            if (!name.isEmpty()) {
                attributes.put(name, value);
            }
        }
        return attributes;
    }

    private static AttributeValue parseAttributeValue(String source, int index) {
        if (index >= source.length()) {
            return new AttributeValue("", index);
        }
        char quote = source.charAt(index);
        if (quote == '"' || quote == '\'') {
            int valueStart = index + 1;
            int valueEnd = source.indexOf(quote, valueStart);
            if (valueEnd < 0) {
                return new AttributeValue(source.substring(valueStart), source.length());
            }
            return new AttributeValue(source.substring(valueStart, valueEnd), valueEnd + 1);
        }
        int valueStart = index;
        while (index < source.length() && !Character.isWhitespace(source.charAt(index))) {
            index++;
        }
        return new AttributeValue(source.substring(valueStart, index), index);
    }

    private static int findTagEnd(String html, int index) {
        char quote = 0;
        while (index < html.length()) {
            char current = html.charAt(index);
            if (quote != 0) {
                if (current == quote) {
                    quote = 0;
                }
            } else if (current == '"' || current == '\'') {
                quote = current;
            } else if (current == '>') {
                return index;
            }
            index++;
        }
        return -1;
    }

    private static String closingTagName(String source) {
        String trimmed = source.trim();
        int index = 0;
        while (index < trimmed.length() && !Character.isWhitespace(trimmed.charAt(index))) {
            index++;
        }
        return trimmed.substring(0, index)
            .toLowerCase(Locale.ROOT);
    }

    private static boolean startsWith(String source, int index, String prefix) {
        return source.regionMatches(index, prefix, 0, prefix.length());
    }

    private static int skipUntil(String source, int index, String terminator) {
        int end = source.indexOf(terminator, index);
        if (end < 0) {
            return source.length();
        }
        return end + terminator.length();
    }

    private static final class StartTag {
        private final String name;
        private final Map<String, String> attributes;
        private final boolean selfClosing;

        private StartTag(String name, Map<String, String> attributes, boolean selfClosing) {
            this.name = name;
            this.attributes = attributes;
            this.selfClosing = selfClosing;
        }
    }

    private static final class AttributeValue {
        private final String value;
        private final int nextIndex;

        private AttributeValue(String value, int nextIndex) {
            this.value = value;
            this.nextIndex = nextIndex;
        }
    }
}
