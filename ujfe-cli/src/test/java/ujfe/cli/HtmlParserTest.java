package ujfe.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class HtmlParserTest {
    @Test
    void parsesElementsAttributesVoidTagsAndText() {
        HtmlParseResult result = new HtmlParser().parse("<div class=\"p-4\" data-test=\"home\">"
            + "<h1>Hello &amp; UJFE</h1>"
            + "<input type=\"text\" required>"
            + "<img src=\"/logo.png\" alt=\"Logo\">"
            + "<wbr>"
            + "</div>");

        assertEquals(1, result.roots()
            .size());
        HtmlNode div = result.roots()
            .get(0);
        assertEquals("div", div.tagName());
        assertEquals("p-4", div.attributes()
            .get("class"));
        assertEquals("home", div.attributes()
            .get("data-test"));
        assertEquals(4, div.children()
            .size());
        assertEquals("Hello & UJFE", div.children()
            .get(0)
            .children()
            .get(0)
            .text());
        assertEquals("", div.children()
            .get(1)
            .attributes()
            .get("required"));
        assertEquals("wbr", div.children()
            .get(3)
            .tagName());
    }

    @Test
    void handlesDoctypeSelfClosingSyntaxAndNumericEntities() {
        HtmlParseResult result = new HtmlParser().parse("<!DOCTYPE html><main><h1>Tom &#38; Jerry</h1><input /></main>");

        assertEquals(1, result.roots()
            .size());
        HtmlNode main = result.roots()
            .get(0);
        assertEquals("main", main.tagName());
        assertEquals("Tom & Jerry", main.children()
            .get(0)
            .children()
            .get(0)
            .text());
        assertEquals("input", main.children()
            .get(1)
            .tagName());
    }

    @Test
    void commentsAreDroppedByDefaultOrUnsafeWhenConfigured() {
        HtmlParseResult dropped = new HtmlParser().parse("<main><!-- secret --><h1>Title</h1></main>");
        assertEquals(1, dropped.roots()
            .get(0)
            .children()
            .size());

        HtmlParseResult unsafe = new HtmlParser().parse(
            "<main><!-- TODO --><h1>Title</h1></main>",
            CommentPolicy.UNSAFE_FALLBACK,
            false
        );
        assertTrue(unsafe.roots()
            .get(0)
            .children()
            .get(0)
            .unsafeNode());
        assertFalse(unsafe.warnings()
            .isEmpty());
    }

    @Test
    void preserveCommentsFailBecauseThereIsNoSafeCommentNode() {
        HtmlConversionException exception = assertThrows(HtmlConversionException.class, () ->
            new HtmlParser().parse("<main><!-- TODO --></main>", CommentPolicy.PRESERVE, false)
        );

        assertTrue(exception.getMessage()
            .contains("no safe comment node API"));
    }

    @Test
    void malformedHtmlFailsOrUsesUnsafeFallback() {
        HtmlConversionException exception = assertThrows(HtmlConversionException.class, () ->
            new HtmlParser().parse("<div><span>Text</div></span>")
        );
        assertTrue(exception.getMessage()
            .contains("Unexpected closing tag"));

        HtmlParseResult unsafe = new HtmlParser().parse(
            "<div><span>Text</div></span>",
            CommentPolicy.DROP,
            true
        );
        assertEquals(1, unsafe.roots()
            .size());
        assertTrue(unsafe.roots()
            .get(0)
            .unsafeNode());
        assertTrue(unsafe.warnings()
            .get(0)
            .contains("unsafeHtml"));
    }

    @Test
    void brokenAttributeSyntaxFailsActionably() {
        HtmlConversionException exception = assertThrows(HtmlConversionException.class, () ->
            new HtmlParser().parse("<div @bad></div>")
        );

        assertTrue(exception.getMessage()
            .contains("Broken attribute syntax"));
    }
}
