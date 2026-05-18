package ujfe.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class UjfeJavaGeneratorTest {
    @TempDir
    Path tempDir;

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

    @Test
    void preservesCommonStructuresAttributesBooleansTablesFormsAndCustomElements() {
        HtmlParseResult result = new HtmlParser().parse("<!doctype html>"
            + "<main id=\"home\" class=\"container mx-auto p-4\" data-page=\"home\" aria-label=\"Home\" style=\"display:block\">"
            + "<img src=\"/poster.png\" alt=\"Poster\">"
            + "<form method=\"post\" action=\"/signup\"><label for=\"email\">Email</label>"
            + "<input id=\"email\" type=\"email\" name=\"email\" required disabled autocomplete=\"email\">"
            + "<textarea name=\"bio\">Tom &amp; Jerry</textarea>"
            + "<select name=\"country\"><option value=\"br\" selected>Brazil</option></select>"
            + "<button type=\"submit\">Save</button></form>"
            + "<table class=\"prices\"><caption>Prices</caption><thead><tr><th scope=\"col\">Plan</th></tr></thead>"
            + "<tbody><tr><td colspan=\"2\" rowspan=\"1\">Pro</td></tr></tbody></table>"
            + "<video controls autoplay muted><source src=\"/movie.mp4\" type=\"video/mp4\"></video>"
            + "<my-widget data-id=\"123\"><span>Custom</span></my-widget>"
            + "</main>");

        GeneratedJava generated = new UjfeJavaGenerator().generate(result, options("GeneratedPage.java"));
        String java = generated.pageJava();

        assertTrue(java.contains(".attr(\"id\", \"home\")"));
        assertTrue(java.contains(".attr(\"class\", \"container mx-auto p-4\")"));
        assertTrue(java.contains(".attr(\"data-page\", \"home\")"));
        assertTrue(java.contains(".attr(\"aria-label\", \"Home\")"));
        assertTrue(java.contains(".attr(\"style\", \"display:block\")"));
        assertTrue(java.contains(".attr(\"required\", true)"));
        assertTrue(java.contains(".attr(\"disabled\", true)"));
        assertTrue(java.contains(".attr(\"selected\", true)"));
        assertTrue(java.contains(".attr(\"controls\", true)"));
        assertTrue(java.contains(".attr(\"autoplay\", true)"));
        assertTrue(java.contains(".attr(\"muted\", true)"));
        assertTrue(java.contains("table()"));
        assertTrue(java.contains(".attr(\"scope\", \"col\")"));
        assertTrue(java.contains(".attr(\"colspan\", \"2\")"));
        assertTrue(java.contains(".attr(\"rowspan\", \"1\")"));
        assertTrue(java.contains("element(\"my-widget\")"));
        assertTrue(java.contains("text(\"Tom & Jerry\")"));
    }

    @Test
    void unsafeFallbackIsExplicitInGeneratedJava() {
        HtmlParseResult result = new HtmlParser().parse(
            "<div><span>Broken</div></span>",
            CommentPolicy.DROP,
            true
        );

        String java = new UjfeJavaGenerator().generate(result, options("GeneratedPage.java"))
            .pageJava();

        assertTrue(java.contains("unsafeHtml(\"<div><span>Broken</div></span>\")"));
    }

    @Test
    void componentizationExtractsDeterministicRenderMethodsForLargePages() {
        HtmlParseResult result = new HtmlParser().parse("<main>"
            + "<header><h1>Title</h1></header>"
            + "<section><p>Hero</p></section>"
            + "<form><button>Submit</button></form>"
            + "<table><tr><td>Cell</td></tr></table>"
            + "<footer><p>Footer</p></footer>"
            + "</main>");

        ConversionOptions options = ConversionOptions.builder(Path.of("input.html"), Path.of("src/main/java/app/pages/GeneratedPage.java"))
            .componentize(true)
            .build();
        GeneratedJava generated = new UjfeJavaGenerator().generate(result, options);

        assertTrue(generated.pageJava()
            .contains("private Node renderHeader()"));
        assertTrue(generated.pageJava()
            .contains("private Node renderSection()"));
        assertTrue(generated.pageJava()
            .contains("private Node renderForm()"));
        assertTrue(generated.pageJava()
            .contains("private Node renderTable()"));
        assertTrue(generated.pageJava()
            .contains("private Node renderFooter()"));
        assertEquals(5, generated.stats()
            .componentMethods());
    }

    @Test
    void extractsInlineCssIntoSingleJavaClassAndReferencesIt() throws Exception {
        Path input = tempDir.resolve("page.html");
        Files.writeString(input, "<style>.card{color:red;}</style><main><h1 class=\"card\">Title</h1></main>", StandardCharsets.UTF_8);
        HtmlParseResult result = new HtmlParser().parse(Files.readString(input));
        ConversionOptions options = ConversionOptions.builder(input, tempDir.resolve("src/main/java/app/pages/GeneratedPage.java"))
            .cssMigrationMode(CssMigrationMode.EXTRACT)
            .cssClassName("GeneratedStyles")
            .build();

        GeneratedJava generated = new UjfeJavaGenerator().generate(result, options);

        assertTrue(generated.hasCssJava());
        assertTrue(generated.cssJava()
            .contains(".card{color:red;}"));
        assertTrue(generated.pageJava()
            .contains("GeneratedStyles.css()"));
        assertFalse(generated.pageJava()
            .contains("style()"));
    }

    @Test
    void generatedJavaCompilesForCommonPageAndCss() throws Exception {
        Path input = tempDir.resolve("page.html");
        Files.writeString(input, "<style>.card{color:red;}</style><main><h1 class=\"card\">Title</h1>"
            + "<form><label for=\"email\">Email</label><input id=\"email\" required></form>"
            + "<table><tr><td>Cell</td></tr></table></main>", StandardCharsets.UTF_8);
        HtmlParseResult result = new HtmlParser().parse(Files.readString(input));
        ConversionOptions options = ConversionOptions.builder(input, tempDir.resolve("src/main/java/app/pages/GeneratedPage.java"))
            .cssMigrationMode(CssMigrationMode.EXTRACT)
            .componentize(true)
            .build();
        GeneratedJava generated = new UjfeJavaGenerator().generate(result, options);

        assertCompiles(generated);
    }

    @Test
    void validatesClassNamesAndSupportsSafeFallback() {
        HtmlConversionException invalid = assertThrows(HtmlConversionException.class, () ->
            ConversionOptions.builder(Path.of("input.html"), Path.of("123-page.java"))
                .build()
        );
        assertTrue(invalid.getMessage()
            .contains("Invalid Java class name"));

        ConversionOptions safe = ConversionOptions.builder(Path.of("input.html"), Path.of("123-page.java"))
            .safeClassName(true)
            .build();
        assertEquals("Page", safe.className());
    }

    private ConversionOptions options(String outputName) {
        return ConversionOptions.builder(Path.of("input.html"), Path.of("src/main/java/app/pages/" + outputName))
            .build();
    }

    private void assertCompiles(GeneratedJava generated) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "Tests require a JDK with javac");
        Path sourceRoot = tempDir.resolve("compile");
        Path packageDir = sourceRoot.resolve("app/pages");
        Files.createDirectories(packageDir);
        Path page = packageDir.resolve("GeneratedPage.java");
        Files.writeString(page, generated.pageJava(), StandardCharsets.UTF_8);
        Path styles = packageDir.resolve("GeneratedStyles.java");
        Files.writeString(styles, generated.cssJava(), StandardCharsets.UTF_8);

        int exitCode = compiler.run(
            null,
            null,
            null,
            "-classpath",
            System.getProperty("java.class.path"),
            page.toString(),
            styles.toString()
        );
        assertEquals(0, exitCode);
    }
}
