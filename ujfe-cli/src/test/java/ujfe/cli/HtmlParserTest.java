package ujfe.cli;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class HtmlParserTest {
    @Test
    void parsesElementsAttributesVoidTagsAndText() {
        HtmlParseResult result = new HtmlParser().parse(""
                + "<div class=\"p-4\" data-test=\"home\">"
                + "<h1>Hello &amp; UJFE</h1>"
                + "<input type=\"text\" required>"
                + "<img src=\"/logo.png\" alt=\"Logo\">"
                + "</div>");

        assertEquals(1, result.roots().size());
        HtmlNode div = result.roots().get(0);
        assertEquals("div", div.tagName());
        assertEquals("p-4", div.attributes().get("class"));
        assertEquals("home", div.attributes().get("data-test"));
        assertEquals(3, div.children().size());
        assertEquals("Hello & UJFE", div.children().get(0).children().get(0).text());
        assertEquals("", div.children().get(1).attributes().get("required"));
    }
}
