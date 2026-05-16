package ujfe.html;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Reusable metadata for standard HTML element behavior.
 */
public final class HtmlElementMetadata {
    private static final Set<String> VOID_ELEMENTS = Set.of(
            "area",
            "base",
            "br",
            "col",
            "embed",
            "hr",
            "img",
            "input",
            "link",
            "meta",
            "param",
            "source",
            "track",
            "wbr"
    );

    private HtmlElementMetadata() {
    }

    /**
     * Returns the centralized set of standard HTML void element names.
     */
    public static Set<String> voidElements() {
        return VOID_ELEMENTS;
    }

    /**
     * Returns true when the tag name is a standard HTML void element.
     */
    public static boolean isVoidElement(String tagName) {
        Objects.requireNonNull(tagName, "tagName");
        return VOID_ELEMENTS.contains(tagName.toLowerCase(Locale.ROOT));
    }

    /**
     * Returns true only when the element is in the HTML namespace and its tag
     * name is a standard HTML void element.
     */
    public static boolean isVoidElement(ElementNamespace namespace, String tagName) {
        return namespace == ElementNamespace.HTML && isVoidElement(tagName);
    }
}
