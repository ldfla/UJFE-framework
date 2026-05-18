package ujfe.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class UjfeJavaGenerator {
    private static final Set<String> UI_FACTORIES = Set.of(
        "html", "head", "body", "title", "meta", "link", "style", "script", "base",
        "div", "figure", "figcaption", "details", "summary", "dialog",
        "header", "main", "aside", "section", "article", "nav", "footer", "address",
        "h1", "h2", "h3", "h4", "h5", "h6",
        "b", "i", "u", "em", "strong", "small", "mark", "abbr", "cite",
        "p", "pre", "code", "blockquote", "q", "br", "hr",
        "img", "picture", "source", "track", "audio", "video", "canvas", "svg", "map", "area",
        "iframe", "object", "embed", "param",
        "table", "thead", "tbody", "tfoot", "tr", "td", "th", "caption", "colgroup", "col",
        "form", "label", "input", "button", "a", "select", "option", "optgroup", "textarea",
        "fieldset", "legend", "datalist", "output", "progress", "meter",
        "li", "ul", "ol", "dt", "dd", "dl", "span", "template", "slot"
    );
    private static final Set<String> BOOLEAN_ATTRIBUTES = Set.of("autofocus", "autoplay", "checked", "controls",
        "disabled", "formnovalidate", "hidden", "ismap", "itemscope", "loop", "multiple", "muted",
        "novalidate", "open", "playsinline", "popover", "readonly", "required", "reversed", "selected");

    String generate(HtmlParseResult parseResult, Path outputPath) {
        String packageName = packageName(outputPath);
        String className = className(outputPath);
        String expression = renderRoots(parseResult.roots());

        StringBuilder java = new StringBuilder();
        if (!packageName.isBlank()) {
            java.append("package ")
                .append(packageName)
                .append(";\n\n");
        }
        java.append("import static ujfe.core.UI.*;\n\n")
            .append("import ujfe.core.Node;\n")
            .append("import ujfe.router.Page;\n\n")
            .append("@Page(\"/\")\n")
            .append("public final class ")
            .append(className)
            .append(" {\n\n")
            .append("    public Node render() {\n")
            .append("        return ")
            .append(indentContinuation(expression, 8))
            .append(";\n")
            .append("    }\n")
            .append("}\n");
        return java.toString();
    }

    private static String renderRoots(List<HtmlNode> roots) {
        if (roots.isEmpty()) {
            return "div()";
        }
        if (roots.size() == 1) {
            return renderNode(roots.get(0), 8);
        }

        StringBuilder expression = new StringBuilder("div()");
        for (HtmlNode root : roots) {
            expression.append("\n                .child(")
                .append(indentContinuation(renderNode(root, 16), 16))
                .append(")");
        }
        return expression.toString();
    }

    private static String renderNode(HtmlNode node, int indent) {
        if (node.textNode()) {
            return "text(" + quote(node.text()) + ")";
        }

        StringBuilder expression = new StringBuilder(factory(node.tagName()));

        for (Map.Entry<String, String> attribute : node.attributes()
            .entrySet()) {
            String name = attribute.getKey();
            String value = attribute.getValue();
            expression.append("\n")
                .append(spaces(indent + 8))
                .append(".attr(")
                .append(quote(name))
                .append(", ");
            if (BOOLEAN_ATTRIBUTES.contains(name.toLowerCase(java.util.Locale.ROOT)) && value.isBlank()) {
                expression.append("true");
            } else {
                expression.append(quote(value));
            }
            expression.append(")");
        }

        for (HtmlNode child : node.children()) {
            expression.append("\n")
                .append(spaces(indent + 8))
                .append(".child(")
                .append(indentContinuation(renderNode(child, indent + 16), indent + 16))
                .append(")");
        }

        return expression.toString();
    }

    private static String factory(String tagName) {
        if (UI_FACTORIES.contains(tagName)) {
            return tagName + "()";
        }
        return "element(" + quote(tagName) + ")";
    }

    private static String packageName(Path outputPath) {
        String normalized = outputPath.normalize()
            .toString()
            .replace('\\', '/');
        int marker = normalized.indexOf("src/main/java/");
        if (marker < 0) {
            return "";
        }

        String afterJava = normalized.substring(marker + "src/main/java/".length());
        int lastSlash = afterJava.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "";
        }
        return afterJava.substring(0, lastSlash)
            .replace('/', '.');
    }

    private static String className(Path outputPath) {
        String fileName = outputPath.getFileName()
            .toString();
        int dotIndex = fileName.lastIndexOf('.');
        String rawName = dotIndex < 0 ? fileName : fileName.substring(0, dotIndex);
        if (!rawName.matches("[A-Za-z_$][A-Za-z0-9_$]*")) {
            throw new IllegalArgumentException("Output filename must be a valid Java class name: " + fileName);
        }
        return rawName;
    }

    private static String indentContinuation(String expression, int spaces) {
        String[] lines = expression.split("\\n", -1);
        if (lines.length == 1) {
            return expression;
        }

        StringBuilder result = new StringBuilder(lines[0]);
        String prefix = spaces(spaces);
        for (int index = 1; index < lines.length; index++) {
            result.append('\n')
                .append(prefix)
                .append(lines[index]);
        }
        return result.toString();
    }

    private static String quote(String value) {
        StringBuilder quoted = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '\\':
                    quoted.append("\\\\");
                    break;
                case '"':
                    quoted.append("\\\"");
                    break;
                case '\n':
                    quoted.append("\\n");
                    break;
                case '\r':
                    quoted.append("\\r");
                    break;
                case '\t':
                    quoted.append("\\t");
                    break;
                default:
                    quoted.append(current);
                    break;
            }
        }
        return quoted.append('"')
            .toString();
    }

    private static String spaces(int count) {
        return " ".repeat(count);
    }
}
