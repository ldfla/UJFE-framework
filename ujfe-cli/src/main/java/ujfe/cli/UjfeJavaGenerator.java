package ujfe.cli;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class UjfeJavaGenerator {
    private static final Set<String> UI_FACTORIES = Set.of(
            "div", "header", "main", "aside", "section", "nav",
            "h1", "h2", "h3", "h4", "h5",
            "b", "i", "u", "em", "strong", "p", "pre", "code", "br",
            "img", "picture", "source", "track", "audio", "video", "canvas", "map", "area",
            "iframe", "object", "embed", "param",
            "form", "label", "input", "button", "a", "select", "option", "optgroup", "textarea",
            "fieldset", "legend", "datalist", "output", "progress", "meter",
            "li", "ul", "ol", "dt", "dl", "html", "span"
    );
    private static final Set<String> BOOLEAN_ATTRIBUTES = Set.of("autofocus", "autoplay", "checked", "controls",
            "disabled", "hidden", "loop", "multiple", "muted", "playsinline", "readonly", "required", "selected");

    String generate(HtmlParseResult parseResult, Path outputPath) {
        String packageName = packageName(outputPath);
        String className = className(outputPath);
        String expression = renderRoots(parseResult.roots());

        StringBuilder java = new StringBuilder();
        if (!packageName.isBlank()) {
            java.append("package ").append(packageName).append(";\n\n");
        }
        java.append("import static ujfe.html.UI.*;\n\n")
                .append("import ujfe.html.Node;\n")
                .append("import ujfe.router.Page;\n\n")
                .append("@Page(\"/\")\n")
                .append("public final class ").append(className).append(" {\n\n")
                .append("    public Node render() {\n")
                .append("        return ").append(indentContinuation(expression, 8)).append(";\n")
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
        List<Map.Entry<String, String>> normalAttributes = new ArrayList<>();

        for (Map.Entry<String, String> attribute : node.attributes().entrySet()) {
            String name = attribute.getKey();
            String value = attribute.getValue();
            if ("class".equals(name)) {
                expression.append("\n")
                        .append(spaces(indent + 8))
                        .append(".css(")
                        .append(quote(value))
                        .append(")");
            } else if (BOOLEAN_ATTRIBUTES.contains(name.toLowerCase(Locale.ROOT)) && value.isBlank()) {
                expression.append("\n")
                        .append(spaces(indent + 8))
                        .append(".")
                        .append(booleanMethod(name))
                        .append("(true)");
            } else if (helperMethod(name) != null) {
                expression.append("\n")
                        .append(spaces(indent + 8))
                        .append(".")
                        .append(helperMethod(name))
                        .append("(")
                        .append(quote(value))
                        .append(")");
            } else {
                normalAttributes.add(attribute);
            }
        }

        for (Map.Entry<String, String> attribute : normalAttributes) {
            expression.append("\n")
                    .append(spaces(indent + 8))
                    .append(".attr(")
                    .append(quote(attribute.getKey()))
                    .append(", ")
                    .append(quote(attribute.getValue()))
                    .append(")");
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
        if ("li".equals(tagName)) {
            return "le()";
        }
        if (UI_FACTORIES.contains(tagName)) {
            return tagName + "()";
        }
        return "element(" + quote(tagName) + ")";
    }

    private static String helperMethod(String attributeName) {
        switch (attributeName) {
            case "id":
            case "title":
            case "lang":
            case "dir":
            case "role":
            case "type":
            case "name":
            case "value":
            case "placeholder":
            case "action":
            case "method":
            case "src":
            case "href":
            case "alt":
            case "label":
            case "min":
            case "max":
            case "step":
            case "pattern":
            case "preload":
            case "poster":
            case "crossorigin":
            case "loading":
                return attributeName;
            case "for":
                return "forId";
            case "inputmode":
                return "inputMode";
            case "referrerpolicy":
                return "referrerPolicy";
            default:
                return null;
        }
    }

    private static String booleanMethod(String attributeName) {
        if ("playsinline".equals(attributeName)) {
            return "playsInline";
        }
        if ("readonly".equals(attributeName)) {
            return "readonly";
        }
        return attributeName;
    }

    private static String packageName(Path outputPath) {
        String normalized = outputPath.normalize().toString().replace('\\', '/');
        int marker = normalized.indexOf("src/main/java/");
        if (marker < 0) {
            return "";
        }

        String afterJava = normalized.substring(marker + "src/main/java/".length());
        int lastSlash = afterJava.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "";
        }
        return afterJava.substring(0, lastSlash).replace('/', '.');
    }

    private static String className(Path outputPath) {
        String fileName = outputPath.getFileName().toString();
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
            result.append('\n').append(prefix).append(lines[index]);
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
        return quoted.append('"').toString();
    }

    private static String spaces(int count) {
        return " ".repeat(count);
    }
}
