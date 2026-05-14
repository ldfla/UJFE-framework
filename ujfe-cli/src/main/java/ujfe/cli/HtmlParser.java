package ujfe.cli;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class HtmlParser {
    private static final Set<String> VOID_TAGS = Set.of("area", "base", "br", "col", "embed", "hr", "img",
            "input", "link", "meta", "param", "source", "track", "wbr");

    HtmlParseResult parse(String html) {
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
                index = commentEnd < 0 ? html.length() : commentEnd + 3;
                continue;
            }

            if (startsWith(html, tagStart, "<!")) {
                int declarationEnd = html.indexOf('>', tagStart + 2);
                index = declarationEnd < 0 ? html.length() : declarationEnd + 1;
                continue;
            }

            int tagEnd = findTagEnd(html, tagStart + 1);
            if (tagEnd < 0) {
                warnings.add("Ignoring unterminated tag at index " + tagStart);
                break;
            }

            String rawTag = html.substring(tagStart + 1, tagEnd).trim();
            if (rawTag.isEmpty()) {
                index = tagEnd + 1;
                continue;
            }

            if (rawTag.startsWith("/")) {
                closeTag(rawTag.substring(1).trim(), stack, warnings);
                index = tagEnd + 1;
                continue;
            }

            boolean selfClosing = rawTag.endsWith("/");
            if (selfClosing) {
                rawTag = rawTag.substring(0, rawTag.length() - 1).trim();
            }

            ParsedTag parsedTag = parseTag(rawTag);
            HtmlNode node = HtmlNode.element(parsedTag.name(), parsedTag.attributes());
            appendNode(node, stack, roots);

            if (!selfClosing && !VOID_TAGS.contains(parsedTag.name())) {
                stack.push(node);
            }

            index = tagEnd + 1;
        }

        while (!stack.isEmpty()) {
            warnings.add("Unclosed tag: <" + stack.pop().tagName() + ">");
        }

        return new HtmlParseResult(roots, warnings);
    }

    private static void appendText(String rawText, Deque<HtmlNode> stack, List<HtmlNode> roots) {
        String text = decodeEntities(rawText);
        if (text.trim().isEmpty()) {
            return;
        }
        appendNode(HtmlNode.text(text.trim()), stack, roots);
    }

    private static void appendNode(HtmlNode node, Deque<HtmlNode> stack, List<HtmlNode> roots) {
        if (stack.isEmpty()) {
            roots.add(node);
        } else {
            stack.peek().addChild(node);
        }
    }

    private static void closeTag(String rawName, Deque<HtmlNode> stack, List<String> warnings) {
        String name = tagName(rawName);
        while (!stack.isEmpty()) {
            HtmlNode current = stack.pop();
            if (current.tagName().equals(name)) {
                return;
            }
            warnings.add("Auto-closing <" + current.tagName() + "> before </" + name + ">");
        }
        warnings.add("Ignoring unmatched closing tag </" + name + ">");
    }

    private static ParsedTag parseTag(String rawTag) {
        int index = 0;
        while (index < rawTag.length() && !Character.isWhitespace(rawTag.charAt(index))) {
            index++;
        }

        String name = tagName(rawTag.substring(0, index));
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
                index++;
                continue;
            }

            String attributeName = rawTag.substring(nameStart, index);
            index = skipWhitespace(rawTag, index);
            String value = "";
            if (index < rawTag.length() && rawTag.charAt(index) == '=') {
                index = skipWhitespace(rawTag, index + 1);
                AttributeValue attributeValue = readAttributeValue(rawTag, index);
                value = decodeEntities(attributeValue.value());
                index = attributeValue.nextIndex();
            }
            attributes.put(attributeName, value);
        }

        return new ParsedTag(name, attributes);
    }

    private static AttributeValue readAttributeValue(String value, int startIndex) {
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
            return new AttributeValue(result.toString(), value.length());
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
        return rawName.trim().toLowerCase(Locale.ROOT);
    }

    private static String decodeEntities(String value) {
        return value
                .replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&amp;", "&");
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
