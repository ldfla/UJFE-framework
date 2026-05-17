package ujfe.html;

import ujfe.core.AttributeEscaper;
import ujfe.core.UjfeContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class Element implements Node {
    private static final Set<String> URL_ATTRIBUTES = Set.of("action", "background", "cite", "data",
            "formaction", "href", "poster", "src");

    private final String tagName;
    private final ElementNamespace namespace;
    private final Map<String, String> attributes;
    private final Map<String, Supplier<String>> dynamicAttributes;
    private final Map<String, Supplier<Boolean>> booleanAttributes;
    private final Map<String, Runnable> eventHandlers;
    private final List<ujfe.core.Node> children;
    private String cssClasses;

    public static Element of(String tagName) {
        return new Element(resolveNamespace(tagName), tagName);
    }

    public static Element html(String tagName) {
        return new Element(ElementNamespace.HTML, tagName);
    }

    public static Element custom(String tagName) {
        return new Element(ElementNamespace.HTML, HtmlNames.validateCustomElementName(tagName));
    }

    public static Element svg(String tagName) {
        return new Element(ElementNamespace.SVG, tagName);
    }

    public static Element mathMl(String tagName) {
        return new Element(ElementNamespace.MATHML, tagName);
    }

    public Element(String tagName) {
        this(resolveNamespace(tagName), tagName);
    }

    private Element(ElementNamespace namespace, String tagName) {
        this.namespace = Objects.requireNonNull(namespace, "namespace");
        this.tagName = HtmlNames.validateElementName(tagName);
        this.attributes = new LinkedHashMap<>();
        this.dynamicAttributes = new LinkedHashMap<>();
        this.booleanAttributes = new LinkedHashMap<>();
        this.eventHandlers = new LinkedHashMap<>();
        this.children = new ArrayList<>();
    }

    public Element css(String classes) {
        if (classes == null || classes.trim().isEmpty()) {
            return this;
        }

        if (cssClasses == null || cssClasses.isEmpty()) {
            cssClasses = classes.trim();
        } else {
            cssClasses = cssClasses + " " + classes.trim();
        }
        return this;
    }

    public Element attr(String name, String value) {
        String validName = HtmlNames.validateAttributeName(name);
        if (value == null) {
            attributes.remove(validName);
        } else {
            attributes.put(validName, sanitizeAttributeValue(validName, value));
        }
        dynamicAttributes.remove(validName);
        booleanAttributes.remove(validName);
        return this;
    }

    public Element attr(String name, boolean enabled) {
        return boolAttr(name, enabled);
    }

    public Element attr(String name, BooleanSupplier enabledSupplier) {
        return boolAttr(name, enabledSupplier);
    }

    public Element attr(String name, Supplier<String> valueSupplier) {
        String validName = HtmlNames.validateAttributeName(name);
        Objects.requireNonNull(valueSupplier, "valueSupplier");
        dynamicAttributes.put(validName, () -> {
            String value = valueSupplier.get();
            if (value == null) {
                return null;
            }
            return sanitizeAttributeValue(validName, value);
        });
        attributes.remove(validName);
        booleanAttributes.remove(validName);
        return this;
    }

    public Element boolAttr(String name, boolean enabled) {
        String validName = HtmlNames.validateAttributeName(name);
        if (enabled) {
            booleanAttributes.put(validName, () -> true);
            attributes.remove(validName);
            dynamicAttributes.remove(validName);
        } else {
            booleanAttributes.remove(validName);
        }
        return this;
    }

    public Element boolAttr(String name, BooleanSupplier enabledSupplier) {
        String validName = HtmlNames.validateAttributeName(name);
        Objects.requireNonNull(enabledSupplier, "enabledSupplier");
        booleanAttributes.put(validName, enabledSupplier::getAsBoolean);
        attributes.remove(validName);
        dynamicAttributes.remove(validName);
        return this;
    }

    public Element boolAttr(String name, Supplier<Boolean> enabledSupplier) {
        String validName = HtmlNames.validateAttributeName(name);
        booleanAttributes.put(validName, Objects.requireNonNull(enabledSupplier, "enabledSupplier"));
        attributes.remove(validName);
        dynamicAttributes.remove(validName);
        return this;
    }

    public Element child(Node child) {
        return child((ujfe.core.Node) child);
    }

    public Element child(ujfe.core.Node child) {
        Objects.requireNonNull(child, "child");
        requireChildrenAllowed();
        children.add(child);
        return this;
    }

    public Element child(String text) {
        return child(new TextNode(text));
    }

    public Element child(Supplier<String> textSupplier) {
        return child(new TextNode(textSupplier));
    }

    public Element children(Node... nodes) {
        Arrays.stream(nodes).forEach(this::child);
        return this;
    }

    public Element children(ujfe.core.Node... nodes) {
        Arrays.stream(nodes).forEach(this::child);
        return this;
    }

    public Element children(Collection<? extends ujfe.core.Node> nodes) {
        nodes.forEach(this::child);
        return this;
    }

    public Element onClick(Runnable handler) {
        return on("click", handler);
    }

    public Element onChange(Runnable handler) {
        return on("change", handler);
    }

    public Element onInput(Runnable handler) {
        return on("input", handler);
    }

    public Element onSubmit(Runnable handler) {
        return on("submit", handler);
    }

    public Element on(String eventName, Runnable handler) {
        eventHandlers.put(HtmlNames.validateEventName(eventName), Objects.requireNonNull(handler, "handler"));
        return this;
    }

    public Element id(String id) {
        return attr("id", id);
    }

    public Element title(String title) {
        return attr("title", title);
    }

    public Element lang(String lang) {
        return attr("lang", lang);
    }

    public Element dir(String dir) {
        return attr("dir", dir);
    }

    public Element role(String role) {
        return attr("role", role);
    }

    public Element tabindex(int tabindex) {
        return attr("tabindex", Integer.toString(tabindex));
    }

    public Element accessKey(String accessKey) {
        return attr("accesskey", accessKey);
    }

    public Element contentEditable(boolean contentEditable) {
        return attr("contenteditable", Boolean.toString(contentEditable));
    }

    public Element draggable(boolean draggable) {
        return attr("draggable", Boolean.toString(draggable));
    }

    public Element hidden(boolean hidden) {
        return boolAttr("hidden", hidden);
    }

    public Element open(boolean open) {
        return boolAttr("open", open);
    }

    public Element spellcheck(boolean spellcheck) {
        return attr("spellcheck", Boolean.toString(spellcheck));
    }

    public Element translate(boolean translate) {
        return attr("translate", translate ? "yes" : "no");
    }

    public Element slot(String slot) {
        return attr("slot", slot);
    }

    public Element part(String part) {
        return attr("part", part);
    }

    public Element aria(String name, String value) {
        return attr("aria-" + HtmlNames.validateAttributeSuffix(name), value);
    }

    public Element ariaLabel(String label) {
        return aria("label", label);
    }

    public Element data(String name, String value) {
        return attr("data-" + HtmlNames.validateAttributeSuffix(name), value);
    }

    public Element type(String type) {
        return attr("type", type);
    }

    public Element name(String name) {
        return attr("name", name);
    }

    public Element value(String value) {
        return attr("value", value);
    }

    public Element value(Supplier<String> valueSupplier) {
        return attr("value", valueSupplier);
    }

    public Element placeholder(String placeholder) {
        return attr("placeholder", placeholder);
    }

    public Element src(String src) {
        return attr("src", src);
    }

    public Element href(String href) {
        return attr("href", href);
    }

    public Element alt(String alt) {
        return attr("alt", alt);
    }

    public Element width(int width) {
        return attr("width", Integer.toString(width));
    }

    public Element height(int height) {
        return attr("height", Integer.toString(height));
    }

    public Element label(String label) {
        return attr("label", label);
    }

    public Element action(String action) {
        return attr("action", action);
    }

    public Element method(String method) {
        return attr("method", method);
    }

    public Element forId(String id) {
        return attr("for", id);
    }

    public Element checked(boolean checked) {
        return boolAttr("checked", checked);
    }

    public Element checked(Supplier<Boolean> checkedSupplier) {
        return boolAttr("checked", checkedSupplier);
    }

    public Element selected(boolean selected) {
        return boolAttr("selected", selected);
    }

    public Element selected(Supplier<Boolean> selectedSupplier) {
        return boolAttr("selected", selectedSupplier);
    }

    public Element disabled(boolean disabled) {
        return boolAttr("disabled", disabled);
    }

    public Element enabled(boolean enabled) {
        return disabled(!enabled);
    }

    public Element required(boolean required) {
        return boolAttr("required", required);
    }

    public Element readonly(boolean readonly) {
        return boolAttr("readonly", readonly);
    }

    public Element multiple(boolean multiple) {
        return boolAttr("multiple", multiple);
    }

    public Element autofocus(boolean autofocus) {
        return boolAttr("autofocus", autofocus);
    }

    public Element controls(boolean controls) {
        return boolAttr("controls", controls);
    }

    public Element autoplay(boolean autoplay) {
        return boolAttr("autoplay", autoplay);
    }

    public Element loop(boolean loop) {
        return boolAttr("loop", loop);
    }

    public Element muted(boolean muted) {
        return boolAttr("muted", muted);
    }

    public Element playsInline(boolean playsInline) {
        return boolAttr("playsinline", playsInline);
    }

    public Element min(String min) {
        return attr("min", min);
    }

    public Element max(String max) {
        return attr("max", max);
    }

    public Element step(String step) {
        return attr("step", step);
    }

    public Element rows(int rows) {
        return attr("rows", Integer.toString(rows));
    }

    public Element cols(int cols) {
        return attr("cols", Integer.toString(cols));
    }

    public Element size(int size) {
        return attr("size", Integer.toString(size));
    }

    public Element maxlength(int maxlength) {
        return attr("maxlength", Integer.toString(maxlength));
    }

    public Element minlength(int minlength) {
        return attr("minlength", Integer.toString(minlength));
    }

    public Element autocomplete(String autocomplete) {
        return attr("autocomplete", autocomplete);
    }

    public Element inputMode(String inputMode) {
        return attr("inputmode", inputMode);
    }

    public Element pattern(String pattern) {
        return attr("pattern", pattern);
    }

    public Element preload(String preload) {
        return attr("preload", preload);
    }

    public Element poster(String poster) {
        return attr("poster", poster);
    }

    public Element crossorigin(String crossorigin) {
        return attr("crossorigin", crossorigin);
    }

    public Element referrerPolicy(String referrerPolicy) {
        return attr("referrerpolicy", referrerPolicy);
    }

    public Element loading(String loading) {
        return attr("loading", loading);
    }

    public Element popover(String popover) {
        return attr("popover", popover);
    }

    public Element popover(boolean enabled) {
        return boolAttr("popover", enabled);
    }

    public String tagName() {
        return tagName;
    }

    public ElementNamespace namespace() {
        return namespace;
    }

    @Override
    public String render(UjfeContext context) {
        Objects.requireNonNull(context, "context");

        Map<String, String> renderedAttributes = new LinkedHashMap<>(attributes);
        dynamicAttributes.forEach((name, valueSupplier) -> {
            String value = valueSupplier.get();
            if (value != null) {
                renderedAttributes.put(name, value);
            }
        });
        booleanAttributes.forEach((name, enabledSupplier) -> {
            if (Boolean.TRUE.equals(enabledSupplier.get())) {
                renderedAttributes.put(name, null);
            } else {
                renderedAttributes.remove(name);
            }
        });

        if (cssClasses != null && !cssClasses.isBlank()) {
            String currentClass = renderedAttributes.get("class");
            String mergedClass = currentClass == null || currentClass.isBlank()
                    ? cssClasses
                    : currentClass + " " + cssClasses;
            renderedAttributes.put("class", mergedClass);
            context.registerCssClasses(mergedClass);
        }

        if (!eventHandlers.isEmpty()) {
            eventHandlers.forEach((eventName, handler) -> {
                Optional<String> eventId = context.registerEvent(handler);
                eventId.ifPresent(id -> {
                    if ("click".equals(eventName)) {
                        renderedAttributes.put("data-ujfe-event", id);
                    }
                    renderedAttributes.put("data-ujfe-event-" + eventName, id);
                });
            });
            renderedAttributes.computeIfAbsent("id", key -> context.nextElementId("ujfe"));
        }

        StringBuilder html = new StringBuilder();
        html.append('<').append(tagName);
        renderedAttributes.forEach((name, value) -> {
            html.append(' ').append(name);
            if (value != null) {
                html.append("=\"")
                        .append(AttributeEscaper.escape(value))
                        .append('"');
            }
        });
        html.append('>');

        if (isVoidElement()) {
            if (!children.isEmpty()) {
                throw voidElementChildrenException();
            }
            return html.toString();
        }

        for (ujfe.core.Node child : children) {
            html.append(child.render(context));
        }
        html.append("</").append(tagName).append('>');
        return html.toString();
    }

    private boolean isVoidElement() {
        return HtmlElementMetadata.isVoidElement(namespace, tagName);
    }

    private void requireChildrenAllowed() {
        if (isVoidElement()) {
            throw voidElementChildrenException();
        }
    }

    private IllegalStateException voidElementChildrenException() {
        return new IllegalStateException("HTML void element <" + tagName + "> cannot have children");
    }

    private static String sanitizeAttributeValue(String name, String value) {
        if (URL_ATTRIBUTES.contains(name.toLowerCase(Locale.ROOT))) {
            return SafeUrl.sanitize(value);
        }
        return value;
    }

    private static ElementNamespace resolveNamespace(String tagName) {
        String validTagName = HtmlNames.validateElementName(tagName);
        String normalizedTagName = validTagName.toLowerCase(Locale.ROOT);
        if ("svg".equals(normalizedTagName)) {
            return ElementNamespace.SVG;
        }
        if ("math".equals(normalizedTagName)) {
            return ElementNamespace.MATHML;
        }
        return ElementNamespace.HTML;
    }
}
