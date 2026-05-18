package ujfe.cli;

import ujfe.core.HtmlElementMetadata;

import java.util.*;

final class HtmlParser {
    HtmlParseResult parse(String html) {
        return parse(html, CommentPolicy.DROP, false);
    }

    HtmlParseResult parse(String html, CommentPolicy commentPolicy, boolean unsafeFallbackEnabled) {
        Objects.requireNonNull(html, "html");
        Objects.requireNonNull(commentPolicy, "commentPolicy");
        if (commentPolicy == CommentPolicy.PRESERVE) {
            throw new HtmlConversionException("HTML comment preserve policy is not supported because UJFE has no safe comment node API. Use --comments drop or --comments unsafe-fallback.");
        }
        try {
            return parseStrict(html, commentPolicy);
        } catch (HtmlConversionException exception) {
            if (!unsafeFallbackEnabled) {
                throw exception;
            }
            return new HtmlParseResult(
                List.of(HtmlNode.unsafe(html, exception.getMessage())),
                List.of("Malformed HTML emitted through unsafeHtml(...): " + exception.getMessage())
            );
        }
    }

    private HtmlParseResult parseStrict(String html, CommentPolicy commentPolicy) {
        List<HtmlNode> roots = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Deque<HtmlNode> stack = new ArrayDeque<>();
        int index = 0;

        while (index < html.length()) {
            int tagStart = html.indexOf('<', index);
            if (tagStart < 0) {
                appendText(html.substring(index), stack, roots);
                break;
            }

            appendText(html.substring(index, tagStart), stack, roots);

            if (startsWith(html, tagStart, "<!--")) {
                int commentEnd = html.indexOf("-->", tagStart + 4);
                if (commentEnd < 0) {
                    throw malformed("Unterminated HTML comment at index " + tagStart);
                }
                String comment = html.substring(tagStart, commentEnd + 3);
                if (commentPolicy == CommentPolicy.UNSAFE_FALLBACK) {
                    appendNode(HtmlNode.unsafe(comment, "HTML comment"), stack, roots);
                    warnings.add("HTML comment emitted through unsafeHtml(...).");
                }
                index = commentEnd + 3;
                continue;
            }

            if (startsWith(html, tagStart, "<!")) {
                int declarationEnd = html.indexOf('>', tagStart + 2);
                if (declarationEnd < 0) {
                    throw malformed("Unterminated HTML declaration at index " + tagStart);
                }
                index = declarationEnd + 1;
                continue;
            }

            int tagEnd = findTagEnd(html, tagStart + 1);
            if (tagEnd < 0) {
                throw malformed("Unterminated tag at index " + tagStart);
            }

            String rawTag = html.substring(tagStart + 1, tagEnd)
                .trim();
            if (rawTag.isEmpty()) {
                index = tagEnd + 1;
                continue;
            }

            if (rawTag.startsWith("/")) {
                closeTag(rawTag.substring(1)
                    .trim(), stack);
                index = tagEnd + 1;
                continue;
            }

            boolean selfClosing = rawTag.endsWith("/");
            if (selfClosing) {
                rawTag = rawTag.substring(0, rawTag.length() - 1)
                    .trim();
            }

            ParsedTag parsedTag = parseTag(rawTag, tagStart);
            HtmlNode node = HtmlNode.element(parsedTag.name(), parsedTag.attributes());
            appendNode(node, stack, roots);

            if (!selfClosing && !HtmlElementMetadata.isVoidElement(parsedTag.name())) {
                stack.push(node);
            }

            index = tagEnd + 1;
        }

        while (!stack.isEmpty()) {
            throw malformed("Unclosed tag: <" + stack.pop()
                .tagName() + ">");
        }

        return new HtmlParseResult(roots, warnings);
    }

    private static void appendText(String rawText, Deque<HtmlNode> stack, List<HtmlNode> roots) {
        String text = decodeEntities(rawText);
        if (text.trim()
            .isEmpty()) {
            return;
        }
        appendNode(HtmlNode.text(normalizeText(text)), stack, roots);
    }

    private static void appendNode(HtmlNode node, Deque<HtmlNode> stack, List<HtmlNode> roots) {
        if (stack.isEmpty()) {
            roots.add(node);
        } else {
            stack.peek()
                .addChild(node);
        }
    }

    private static void closeTag(String rawName, Deque<HtmlNode> stack) {
        String name = tagName(rawName);
        if (stack.isEmpty()) {
            throw malformed("Unexpected closing tag </" + name + "> with no open element.");
        }
        HtmlNode current = stack.peek();
        if (!current.tagName()
            .equals(name)) {
            throw malformed("Unexpected closing tag </" + name + ">; expected </" + current.tagName() + ">.");
        }
        stack.pop();
    }

    private static ParsedTag parseTag(String rawTag, int sourceIndex) {
        int index = 0;
        while (index < rawTag.length() && !Character.isWhitespace(rawTag.charAt(index))) {
            index++;
        }

        String name = tagName(rawTag.substring(0, index));
        if (name.isBlank()) {
            throw malformed("Missing tag name at index " + sourceIndex + ".");
        }
        Map<String, String> attributes = new LinkedHashMap<>();

        while (index < rawTag.length()) {
            index = skipWhitespace(rawTag, index);
            if (index >= rawTag.length()) {
                break;
            }

            int nameStart = index;
            while (index < rawTag.length() && isAttributeNameChar(rawTag.charAt(index))) {
                index++;
            }
            if (index == nameStart) {
                throw malformed("Broken attribute syntax near <" + name + "> at index " + sourceIndex + ".");
            }

            String attributeName = rawTag.substring(nameStart, index);
            index = skipWhitespace(rawTag, index);
            String value = "";
            if (index < rawTag.length() && rawTag.charAt(index) == '=') {
                index = skipWhitespace(rawTag, index + 1);
                AttributeValue attributeValue = readAttributeValue(rawTag, index, attributeName);
                value = decodeEntities(attributeValue.value());
                index = attributeValue.nextIndex();
            }
            attributes.put(attributeName, value);
        }

        return new ParsedTag(name, attributes);
    }

    private static AttributeValue readAttributeValue(String value, int startIndex, String attributeName) {
        if (startIndex >= value.length()) {
            return new AttributeValue("", startIndex);
        }

        char quote = value.charAt(startIndex);
        if (quote == '"' || quote == '\'') {
            StringBuilder result = new StringBuilder();
            for (int index = startIndex + 1; index < value.length(); index++) {
                char current = value.charAt(index);
                if (current == quote) {
                    return new AttributeValue(result.toString(), index + 1);
                }
                result.append(current);
            }
            throw malformed("Unterminated quoted attribute value for " + attributeName + ".");
        }

        int index = startIndex;
        while (index < value.length() && !Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return new AttributeValue(value.substring(startIndex, index), index);
    }

    private static int findTagEnd(String html, int startIndex) {
        char quote = 0;
        for (int index = startIndex; index < html.length(); index++) {
            char current = html.charAt(index);
            if (quote != 0) {
                if (current == quote) {
                    quote = 0;
                }
                continue;
            }
            if (current == '"' || current == '\'') {
                quote = current;
                continue;
            }
            if (current == '>') {
                return index;
            }
        }
        return -1;
    }

    private static int skipWhitespace(String value, int index) {
        while (index < value.length() && Character.isWhitespace(value.charAt(index))) {
            index++;
        }
        return index;
    }

    private static boolean isAttributeNameChar(char current) {
        return Character.isLetterOrDigit(current) || current == '-' || current == '_' || current == ':' || current == '.';
    }

    private static boolean startsWith(String value, int offset, String prefix) {
        return offset >= 0 && offset + prefix.length() <= value.length() && value.startsWith(prefix, offset);
    }

    private static String tagName(String rawName) {
        return rawName.trim()
            .toLowerCase(Locale.ROOT);
    }

    private static String decodeEntities(String value) {
        String decoded = value
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&amp;", "&");
        StringBuilder result = new StringBuilder(decoded.length());
        for (int index = 0; index < decoded.length(); index++) {
            char current = decoded.charAt(index);
            if (current == '&' && index + 3 < decoded.length() && decoded.charAt(index + 1) == '#') {
                int semicolon = decoded.indexOf(';', index + 2);
                if (semicolon > 0) {
                    String entity = decoded.substring(index + 2, semicolon);
                    try {
                        int codePoint = parseEntityCodePoint(entity);
                        result.appendCodePoint(codePoint);
                        index = semicolon;
                        continue;
                    } catch (IllegalArgumentException ignored) {
                        // Leave unknown numeric entities unchanged.
                    }
                }
            }
            result.append(current);
        }
        return result.toString();
    }

    private static String normalizeText(String text) {
        if (text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
            return text.trim();
        }
        return text;
    }

    private static int parseEntityCodePoint(String entity) {
        if (entity.startsWith("x") || entity.startsWith("X")) {
            return Integer.parseInt(entity.substring(1), 16);
        }
        return Integer.parseInt(entity);
    }

    private static HtmlConversionException malformed(String message) {
        return new HtmlConversionException("Malformed HTML: " + message);
    }

    private static final class ParsedTag {
        private final String name;
        private final Map<String, String> attributes;

        private ParsedTag(String name, Map<String, String> attributes) {
            this.name = name;
            this.attributes = attributes;
        }

        private String name() {
            return name;
        }

        private Map<String, String> attributes() {
            return attributes;
        }
    }

    private static final class AttributeValue {
        private final String value;
        private final int nextIndex;

        private AttributeValue(String value, int nextIndex) {
            this.value = value;
            this.nextIndex = nextIndex;
        }

        private String value() {
            return value;
        }

        private int nextIndex() {
            return nextIndex;
        }
    }
}
