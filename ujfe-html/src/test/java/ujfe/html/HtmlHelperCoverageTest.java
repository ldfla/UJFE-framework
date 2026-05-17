package ujfe.html;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class HtmlHelperCoverageTest {
    private static final List<String> VOID_HELPERS = List.of(
            "area", "base", "br", "col", "embed", "hr", "img",
            "input", "link", "meta", "param", "source", "track"
    );

    private static final Map<String, String> REQUIRED_HELPERS = requiredHelpers();
    private static final Map<String, String> TEXT_HELPERS = textHelpers();

    @Test
    void everyRequiredHelperMethodExistsAndRendersExpectedTag() throws Exception {
        for (var entry : REQUIRED_HELPERS.entrySet()) {
            Element element = invokeHelper(entry.getKey());

            assertEquals(entry.getValue(), element.tagName());
            assertEquals(Element.of(entry.getValue()).tagName(), element.tagName());
        }
    }

    @Test
    void voidHelpersRenderWithoutClosingTags() throws Exception {
        for (String helperName : VOID_HELPERS) {
            Element element = invokeHelper(helperName);

            assertTrue(HtmlElementMetadata.isVoidElement(element.namespace(), element.tagName()));
            assertEquals("<" + element.tagName() + ">", element.render());
        }
    }

    @Test
    void normalHelpersRenderWithClosingTags() throws Exception {
        for (var entry : REQUIRED_HELPERS.entrySet()) {
            if (VOID_HELPERS.contains(entry.getKey())) {
                continue;
            }

            Element element = invokeHelper(entry.getKey());

            assertFalse(HtmlElementMetadata.isVoidElement(element.namespace(), element.tagName()));
            assertEquals("<" + element.tagName() + "></" + element.tagName() + ">", element.render());
        }
    }

    @Test
    void textOverloadsEscapeTextContent() throws Exception {
        for (var entry : TEXT_HELPERS.entrySet()) {
            Element element = invokeTextHelper(entry.getKey(), "<script>");

            assertEquals("<" + entry.getValue() + ">&lt;script&gt;</" + entry.getValue() + ">", element.render());
        }
    }

    @Test
    void helpersRenderNestedNodes() {
        assertEquals("<section><p>Nested</p></section>", UI.section().child(UI.p("Nested")).render());
        assertEquals("<html><head></head><body></body></html>", UI.html(UI.head(), UI.body()).render());
        assertEquals("<form><label>Name</label><input></form>", UI.form(UI.label("Name"), UI.input()).render());
    }

    private static Element invokeHelper(String methodName) throws Exception {
        Method method;
        Object result;
        try {
            method = UI.class.getMethod(methodName);
            result = method.invoke(null);
        } catch (NoSuchMethodException exception) {
            method = UI.class.getMethod(methodName, Node[].class);
            result = method.invoke(null, (Object) new Node[0]);
        }
        return (Element) result;
    }

    private static Element invokeTextHelper(String methodName, String text) throws Exception {
        Method method = UI.class.getMethod(methodName, String.class);
        Object result = method.invoke(null, text);
        return (Element) result;
    }

    private static Map<String, String> requiredHelpers() {
        var helpers = new LinkedHashMap<String, String>();
        helperGroup(helpers, "html", "head", "body", "title", "meta", "link", "style", "script", "base");
        helperGroup(helpers, "main", "section", "article", "aside", "header", "footer", "nav", "address");
        helperGroup(helpers, "h1", "h2", "h3", "h4", "h5", "h6", "p", "span", "strong", "em", "small", "mark",
                "abbr", "cite", "code", "pre", "blockquote", "q", "br", "hr");
        helperGroup(helpers, "div", "figure", "figcaption", "details", "summary", "dialog");
        helperGroup(helpers, "ul", "ol", "li", "dl", "dt", "dd");
        helperGroup(helpers, "a");
        helperGroup(helpers, "img", "picture", "source", "audio", "video", "track", "canvas", "svg", "map", "area",
                "iframe", "embed", "object", "param");
        helperGroup(helpers, "table", "thead", "tbody", "tfoot", "tr", "td", "th", "caption", "colgroup", "col");
        helperGroup(helpers, "form", "input", "textarea", "button", "select", "option", "optgroup", "label",
                "fieldset", "legend", "datalist", "output", "progress", "meter");
        helperGroup(helpers, "template", "slot");
        assertFalse(helpers.isEmpty());
        return helpers;
    }

    private static Map<String, String> textHelpers() {
        var helpers = new LinkedHashMap<String, String>();
        helperGroup(helpers, "title", "figcaption", "summary", "address", "h1", "h2", "h3", "h4", "h5", "h6",
                "em", "strong", "small", "mark", "abbr", "cite", "p", "code", "blockquote", "q", "td", "th",
                "caption", "label", "button", "a", "option", "textarea", "legend", "output", "dt", "dd", "span");
        assertTrue(helpers.size() > 20);
        return helpers;
    }

    private static void helperGroup(Map<String, String> helpers, String... names) {
        for (String name : names) {
            helpers.put(name, name);
        }
    }
}
