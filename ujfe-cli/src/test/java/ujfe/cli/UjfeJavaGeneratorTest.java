package ujfe.cli;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class UjfeJavaGeneratorTest {
    @Test
    void generatesUjfePageFromHtml() {
        HtmlParseResult result = new HtmlParser().parse(""
                + "<section class=\"p-4 flex gap-2\">"
                + "<h1 title=\"Hero\">Hello</h1>"
                + "<a href=\"/docs\">Docs</a>"
                + "<input type=\"text\" required>"
                + "</section>");

        String java = new UjfeJavaGenerator().generate(
                result,
                Path.of("src/main/java/app/pages/Page.java")
        );

        assertTrue(java.contains("package app.pages;"));
        assertTrue(java.contains("@Page(\"/\")"));
        assertTrue(java.contains("public final class Page"));
        assertTrue(java.contains("section()"));
        assertTrue(java.contains(".css(\"p-4 flex gap-2\")"));
        assertTrue(java.contains(".title(\"Hero\")"));
        assertTrue(java.contains(".href(\"/docs\")"));
        assertTrue(java.contains(".required(true)"));
    }
}
