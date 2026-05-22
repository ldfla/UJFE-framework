package ujfe.cli;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

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
            "li", "ul", "ol", "dt", "dd", "dl", "span", "template", "slot", "math");
    private static final Set<String> BOOLEAN_ATTRIBUTES = Set.of("autofocus", "autoplay", "checked", "controls",
            "disabled", "formnovalidate", "hidden", "ismap", "itemscope", "loop", "multiple", "muted",
            "novalidate", "open", "playsinline", "popover", "readonly", "required", "reversed", "selected");

    String generate(HtmlParseResult parseResult, Path outputPath) {
        ConversionOptions options = ConversionOptions.builder(Path.of("input.html"), outputPath)
                .build();
        return generate(parseResult, options).pageJava();
    }

    GeneratedJava generate(HtmlParseResult parseResult, ConversionOptions options) {
        Objects.requireNonNull(parseResult, "parseResult");
        Objects.requireNonNull(options, "options");

        List<String> warnings = new ArrayList<>(parseResult.warnings());
        CssExtraction cssExtraction = extractCss(parseResult.roots(), options, warnings);
        ComponentPlan componentPlan = ComponentPlan.create(parseResult.roots(), cssExtraction.skippedNodes(), options);
        String expression = renderRoots(parseResult.roots(), 8, cssExtraction.skippedNodes(), componentPlan);
        ConversionStats stats = stats(parseResult.roots(), cssExtraction.skippedNodes(), componentPlan.methodCount(),
                cssExtraction.blockCount());

        String pageJava = pageJava(options, expression, componentPlan, cssExtraction.hasCss());
        String cssJava = cssExtraction.hasCss() ? cssJava(options, cssExtraction.css()) : null;
        Path cssOutput = cssJava == null
                ? null
                : options.output()
                        .resolveSibling(options.cssClassName() + ".java");
        return new GeneratedJava(pageJava, cssJava, cssOutput, stats, warnings);
    }

    private static String pageJava(
            ConversionOptions options,
            String expression,
            ComponentPlan componentPlan,
            boolean referencesCss) {
        StringBuilder java = new StringBuilder();
        if (!options.packageName()
                .isBlank()) {
            java.append("package ")
                    .append(options.packageName())
                    .append(";\n\n");
        }
        java.append("import static ujfe.core.UI.*;\n\n")
                .append("import ujfe.core.Node;\n")
                .append("import ujfe.router.Page;\n\n")
                .append("@Page(\"/\")\n")
                .append("public final class ")
                .append(options.className())
                .append(" {\n\n")
                .append("    public Node render() {\n")
                .append("        return ")
                .append(indentContinuation(expression, 8))
                .append(";\n")
                .append("    }\n");

        if (referencesCss) {
            java.append("\n")
                    .append("    public String styles() {\n")
                    .append("        return ")
                    .append(options.cssClassName())
                    .append(".css();\n")
                    .append("    }\n");
        }

        for (ComponentMethod method : componentPlan.methods()) {
            java.append("\n")
                    .append("    private Node ")
                    .append(method.name())
                    .append("() {\n")
                    .append("        return ")
                    .append(indentContinuation(
                            renderNode(method.node(), 8, componentPlan.skippedNodes(), ComponentPlan.empty()), 8))
                    .append(";\n")
                    .append("    }\n");
        }

        java.append("}\n");
        return java.toString();
    }

    private static String cssJava(ConversionOptions options, String css) {
        StringBuilder java = new StringBuilder();
        if (!options.packageName()
                .isBlank()) {
            java.append("package ")
                    .append(options.packageName())
                    .append(";\n\n");
        }
        java.append("public final class ")
                .append(options.cssClassName())
                .append(" {\n")
                .append("    private ")
                .append(options.cssClassName())
                .append("() {\n")
                .append("    }\n\n")
                .append("    public static String css() {\n")
                .append("        return ")
                .append(quote(css))
                .append(";\n")
                .append("    }\n")
                .append("}\n");
        return java.toString();
    }

    private static String renderRoots(
            List<HtmlNode> roots,
            int indent,
            Set<HtmlNode> skippedNodes,
            ComponentPlan componentPlan) {
        List<HtmlNode> renderableRoots = withoutSkipped(roots, skippedNodes);
        if (renderableRoots.isEmpty()) {
            return "div()";
        }
        if (renderableRoots.size() == 1) {
            return renderNode(renderableRoots.get(0), indent, skippedNodes, componentPlan);
        }

        StringBuilder expression = new StringBuilder("div()");
        for (HtmlNode root : renderableRoots) {
            expression.append("\n                .child(")
                    .append(indentContinuation(renderNode(root, 16, skippedNodes, componentPlan), 16))
                    .append(")");
        }
        return expression.toString();
    }

    private static String renderNode(
            HtmlNode node,
            int indent,
            Set<HtmlNode> skippedNodes,
            ComponentPlan componentPlan) {
        if (node.textNode()) {
            return "text(" + quote(node.text()) + ")";
        }
        if (node.unsafeNode()) {
            return "unsafeHtml(" + quote(node.unsafeHtml()) + ")";
        }

        String componentMethod = componentPlan.methodName(node);
        if (componentMethod != null) {
            return componentMethod + "()";
        }

        StringBuilder expression = new StringBuilder(factory(node.tagName()));
        appendAttributes(expression, node, indent);
        appendChildren(expression, node, indent, skippedNodes, componentPlan);
        return expression.toString();
    }

    private static void appendAttributes(StringBuilder expression, HtmlNode node, int indent) {
        for (Map.Entry<String, String> attribute : node.attributes()
                .entrySet()) {
            String name = attribute.getKey();
            String value = attribute.getValue();
            expression.append("\n")
                    .append(spaces(indent + 8))
                    .append(".attr(")
                    .append(quote(name))
                    .append(", ");
            if (BOOLEAN_ATTRIBUTES.contains(name.toLowerCase(Locale.ROOT)) && value.isBlank()) {
                expression.append("true");
            } else {
                expression.append(quote(value));
            }
            expression.append(")");
        }
    }

    private static void appendChildren(
            StringBuilder expression,
            HtmlNode node,
            int indent,
            Set<HtmlNode> skippedNodes,
            ComponentPlan componentPlan) {
        for (HtmlNode child : withoutSkipped(node.children(), skippedNodes)) {
            expression.append("\n")
                    .append(spaces(indent + 8))
                    .append(".child(")
                    .append(indentContinuation(renderNode(child, indent + 16, skippedNodes, componentPlan),
                            indent + 16))
                    .append(")");
        }
    }

    private static List<HtmlNode> withoutSkipped(List<HtmlNode> nodes, Set<HtmlNode> skippedNodes) {
        List<HtmlNode> filtered = new ArrayList<>();
        for (HtmlNode node : nodes) {
            if (!skippedNodes.contains(node)) {
                filtered.add(node);
            }
        }
        return filtered;
    }

    private static String factory(String tagName) {
        if (UI_FACTORIES.contains(tagName)) {
            return tagName + "()";
        }
        return "element(" + quote(tagName) + ")";
    }

    private static CssExtraction extractCss(
            List<HtmlNode> roots,
            ConversionOptions options,
            List<String> warnings) {
        if (options.cssMigrationMode() != CssMigrationMode.EXTRACT) {
            return CssExtraction.empty();
        }

        Set<HtmlNode> skippedNodes = Collections.newSetFromMap(new IdentityHashMap<>());
        StringBuilder css = new StringBuilder();
        int[] blockCount = { 0 };
        Path inputDirectory = options.input()
                .toAbsolutePath()
                .normalize()
                .getParent();
        collectCss(roots, options, inputDirectory, skippedNodes, css, blockCount, warnings);
        return new CssExtraction(css.toString(), skippedNodes, blockCount[0]);
    }

    private static void collectCss(
            List<HtmlNode> nodes,
            ConversionOptions options,
            Path inputDirectory,
            Set<HtmlNode> skippedNodes,
            StringBuilder css,
            int[] blockCount,
            List<String> warnings) {
        for (HtmlNode node : nodes) {
            if (!node.elementNode()) {
                continue;
            }
            if ("style".equals(node.tagName())) {
                blockCount[0]++;
                appendCssBlock(css, "inline style block " + blockCount[0], textContent(node));
                skippedNodes.add(node);
                continue;
            }
            if (isStylesheetLink(node)) {
                if (tryExtractStylesheetLink(node, options, inputDirectory, css, blockCount, warnings)) {
                    skippedNodes.add(node);
                }
                continue;
            }
            collectCss(node.children(), options, inputDirectory, skippedNodes, css, blockCount, warnings);
        }
    }

    private static boolean tryExtractStylesheetLink(
            HtmlNode node,
            ConversionOptions options,
            Path inputDirectory,
            StringBuilder css,
            int[] blockCount,
            List<String> warnings) {
        String href = node.attributes()
                .get("href");
        if (href == null || href.isBlank()) {
            warnings.add("Stylesheet link without href was preserved.");
            return false;
        }
        if (href.contains("://") || href.startsWith("//") || href.startsWith("/")) {
            warnings.add(
                    "Stylesheet link was preserved because only relative local stylesheets are extracted: " + href);
            return false;
        }
        Path stylesheet = inputDirectory == null
                ? Path.of(href)
                : inputDirectory.resolve(href)
                        .normalize();
        if (inputDirectory != null && !stylesheet.startsWith(inputDirectory)) {
            warnings.add("Stylesheet link was preserved because it resolves outside the input directory: " + href);
            return false;
        }
        if (!Files.isRegularFile(stylesheet) || !Files.isReadable(stylesheet)) {
            warnings.add("Stylesheet link was preserved because the stylesheet is not readable: " + href);
            return false;
        }
        try {
            blockCount[0]++;
            appendCssBlock(css, "stylesheet " + href, Files.readString(stylesheet, options.encoding()));
            return true;
        } catch (IOException exception) {
            warnings.add("Stylesheet link was preserved because the stylesheet could not be read: " + href);
            return false;
        }
    }

    private static boolean isStylesheetLink(HtmlNode node) {
        if (!"link".equals(node.tagName())) {
            return false;
        }
        String rel = node.attributes()
                .getOrDefault("rel", "");
        for (String token : rel.toLowerCase(Locale.ROOT)
                .split("\\s+")) {
            if ("stylesheet".equals(token)) {
                return true;
            }
        }
        return false;
    }

    private static void appendCssBlock(StringBuilder css, String label, String body) {
        if (css.length() > 0) {
            css.append("\n\n");
        }
        css.append("/* ")
                .append(label.replace("*/", "* /"))
                .append(" */\n")
                .append(body.trim())
                .append('\n');
    }

    private static String textContent(HtmlNode node) {
        StringBuilder text = new StringBuilder();
        for (HtmlNode child : node.children()) {
            if (child.textNode()) {
                text.append(child.text());
            } else if (child.elementNode()) {
                text.append(textContent(child));
            }
        }
        return text.toString();
    }

    private static ConversionStats stats(
            List<HtmlNode> roots,
            Set<HtmlNode> skippedNodes,
            int componentMethods,
            int cssBlocks) {
        int[] counts = new int[3];
        count(roots, skippedNodes, counts);
        return new ConversionStats(counts[0], counts[1], counts[2], componentMethods, cssBlocks);
    }

    private static void count(List<HtmlNode> nodes, Set<HtmlNode> skippedNodes, int[] counts) {
        for (HtmlNode node : nodes) {
            if (skippedNodes.contains(node)) {
                continue;
            }
            if (node.elementNode()) {
                counts[0]++;
                counts[1] += node.attributes()
                        .size();
                count(node.children(), skippedNodes, counts);
            } else if (node.unsafeNode()) {
                counts[2]++;
            }
        }
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

    private static final class CssExtraction {
        private final String css;
        private final Set<HtmlNode> skippedNodes;
        private final int blockCount;

        private CssExtraction(String css, Set<HtmlNode> skippedNodes, int blockCount) {
            this.css = css;
            this.skippedNodes = skippedNodes;
            this.blockCount = blockCount;
        }

        static CssExtraction empty() {
            return new CssExtraction("", Set.of(), 0);
        }

        String css() {
            return css;
        }

        boolean hasCss() {
            return !css.isBlank();
        }

        Set<HtmlNode> skippedNodes() {
            return skippedNodes;
        }

        int blockCount() {
            return blockCount;
        }
    }

    private static final class ComponentPlan {
        private final Map<HtmlNode, String> methodNames;
        private final List<ComponentMethod> methods;
        private final Set<HtmlNode> skippedNodes;

        private ComponentPlan(Map<HtmlNode, String> methodNames, List<ComponentMethod> methods,
                Set<HtmlNode> skippedNodes) {
            this.methodNames = methodNames;
            this.methods = methods;
            this.skippedNodes = skippedNodes;
        }

        static ComponentPlan empty() {
            return new ComponentPlan(Map.of(), List.of(), Set.of());
        }

        static ComponentPlan create(List<HtmlNode> roots, Set<HtmlNode> skippedNodes, ConversionOptions options) {
            if (!options.componentize()) {
                return empty();
            }
            List<HtmlNode> candidates = candidates(roots, skippedNodes);
            if (candidates.size() < 3) {
                return empty();
            }
            Map<HtmlNode, String> names = new IdentityHashMap<>();
            List<ComponentMethod> methods = new ArrayList<>();
            Set<String> usedNames = new LinkedHashSet<>();
            for (HtmlNode candidate : candidates) {
                String methodName = JavaNames.safeMethodName(methodBase(candidate), usedNames);
                names.put(candidate, methodName);
                methods.add(new ComponentMethod(methodName, candidate));
            }
            return new ComponentPlan(names, List.copyOf(methods), skippedNodes);
        }

        private static List<HtmlNode> candidates(List<HtmlNode> roots, Set<HtmlNode> skippedNodes) {
            List<HtmlNode> filteredRoots = withoutSkipped(roots, skippedNodes);
            if (filteredRoots.size() > 1) {
                return filteredRoots;
            }
            if (filteredRoots.size() == 1) {
                HtmlNode root = filteredRoots.get(0);
                if (root.elementNode()) {
                    List<HtmlNode> children = new ArrayList<>();
                    for (HtmlNode child : withoutSkipped(root.children(), skippedNodes)) {
                        if (child.elementNode()) {
                            children.add(child);
                        }
                    }
                    return children;
                }
            }
            return List.of();
        }

        private static String methodBase(HtmlNode node) {
            switch (node.tagName()) {
                case "header":
                    return "renderHeader";
                case "main":
                    return "renderMainContent";
                case "footer":
                    return "renderFooter";
                case "nav":
                    return "renderNavigation";
                case "form":
                    return "renderForm";
                case "table":
                    return "renderTable";
                case "section":
                    return "renderSection";
                case "article":
                    return "renderArticle";
                default:
                    return "render" + Character.toUpperCase(node.tagName()
                            .charAt(0)) + node.tagName()
                                    .substring(1);
            }
        }

        String methodName(HtmlNode node) {
            return methodNames.get(node);
        }

        List<ComponentMethod> methods() {
            return methods;
        }

        Set<HtmlNode> skippedNodes() {
            return skippedNodes;
        }

        int methodCount() {
            return methods.size();
        }
    }

    private static final class ComponentMethod {
        private final String name;
        private final HtmlNode node;

        private ComponentMethod(String name, HtmlNode node) {
            this.name = name;
            this.node = node;
        }

        String name() {
            return name;
        }

        HtmlNode node() {
            return node;
        }
    }
}
