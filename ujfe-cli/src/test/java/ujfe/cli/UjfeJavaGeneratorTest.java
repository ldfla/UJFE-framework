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
                + "<dialog open><p>Modal</p></dialog>"
                + "</section>");

        String java = new UjfeJavaGenerator().generate(
                result,
                Path.of("src/main/java/app/pages/Page.java")
        );

        assertTrue(java.contains("package app.pages;"));
        assertTrue(java.contains("@Page(\"/\")"));
        assertTrue(java.contains("public final class Page"));
        assertTrue(java.contains("section()"));
        assertTrue(java.contains(".attr(\"class\", \"p-4 flex gap-2\")"));
        assertTrue(java.contains(".attr(\"title\", \"Hero\")"));
        assertTrue(java.contains(".attr(\"href\", \"/docs\")"));
        assertTrue(java.contains(".attr(\"required\", true)"));
        assertTrue(java.contains("dialog()"));
        assertTrue(java.contains(".attr(\"open\", true)"));
    }
}
