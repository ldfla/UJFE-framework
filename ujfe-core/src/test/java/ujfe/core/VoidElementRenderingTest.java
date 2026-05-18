package ujfe.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static ujfe.core.UI.*;

final class VoidElementRenderingTest {
    @Test
    void rendersVoidElementWithoutClosingTag() {
        Element element = img().src("/logo.png").alt("Logo");

        assertTrue(HtmlElementMetadata.isVoidElement(element.namespace(), element.tagName()));
        assertTrue(HtmlElementMetadata.voidElements().contains("wbr"));
        assertEquals("<img src=\"/logo.png\" alt=\"Logo\">", element.render());
        assertEquals("<wbr>", Element.of("wbr").render());
    }

    @Test
    void voidElementWithChildFailsImmediately() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> img().child("Logo"));

        assertTrue(exception.getMessage().contains("HTML void element <img> cannot have children"));
    }

    @Test
    void normalElementWithChildrenRendersClosingTag() {
        assertEquals("<div><p>Hello</p></div>", div().child(p("Hello")).render());
    }

    @Test
    void normalElementWithoutChildrenRendersClosingTag() {
        assertEquals("<div></div>", div().render());
    }

    @Test
    void unknownFutureElementDefaultsToNormalElementBehavior() {
        Element empty = Element.of("future-html-element").attr("data-ready", true);
        Element withChild = Element.of("future-html-element").child("Future");

        assertFalse(HtmlElementMetadata.isVoidElement(empty.namespace(), empty.tagName()));
        assertEquals("<future-html-element data-ready></future-html-element>", empty.render());
        assertEquals("<future-html-element>Future</future-html-element>", withChild.render());
    }

    @Test
    void svgAndMathMlNamesDoNotUseHtmlVoidRules() {
        Element svg = Element.svg("source").child(Element.svg("title").child("Vector"));
        Element math = Element.mathMl("source").child(Element.mathMl("mi").child("x"));

        assertFalse(HtmlElementMetadata.isVoidElement(svg.namespace(), svg.tagName()));
        assertFalse(HtmlElementMetadata.isVoidElement(math.namespace(), math.tagName()));
        assertEquals("<source><title>Vector</title></source>", svg.render());
        assertEquals("<source><mi>x</mi></source>", math.render());
    }
}
