package ujfe.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class ConvertCommandTest {
    @TempDir
    Path tempDir;

    @Test
    void convertsHtmlFileToUjfePage() throws Exception {
        Path input = tempDir.resolve("page.html");
        Path output = tempDir.resolve("src/main/java/app/pages/Page.java");
        Files.writeString(input, "<div class=\"p-4\"><h1>Hello</h1><button>Salvar</button></div>", StandardCharsets.UTF_8);

        int exitCode = new ConvertCommand().run(new String[]{
            input.toString(),
            "--out",
            output.toString(),
            "--type",
            "html"
        });

        String java = Files.readString(output, StandardCharsets.UTF_8);
        assertEquals(0, exitCode);
        assertTrue(java.contains("package app.pages;"));
        assertTrue(java.contains("div()"));
        assertTrue(java.contains(".attr(\"class\", \"p-4\")"));
        assertTrue(java.contains("h1()"));
        assertTrue(java.contains("button()"));
    }

    @Test
    void supportsOutputClassPackageCommentsCssEncodingAndComponentizeOptions() throws Exception {
        Path input = tempDir.resolve("page.html");
        Path output = tempDir.resolve("generated/GeneratedPage.java");
        Files.writeString(input, "<style>.card{padding:1rem;}</style>"
            + "<main><header><h1 class=\"card\">Hello</h1></header><section><p>Body</p></section><footer>Bye</footer></main>",
            StandardCharsets.UTF_8);

        int exitCode = new ConvertCommand().run(new String[]{
            input.toString(),
            "--output",
            output.toString(),
            "--class-name",
            "GeneratedPage",
            "--package",
            "app.generated",
            "--comments",
            "drop",
            "--css",
            "extract",
            "--css-class-name",
            "GeneratedStyles",
            "--componentize",
            "--encoding",
            "UTF-8"
        });

        String java = Files.readString(output, StandardCharsets.UTF_8);
        String css = Files.readString(output.resolveSibling("GeneratedStyles.java"), StandardCharsets.UTF_8);
        assertEquals(0, exitCode);
        assertTrue(java.contains("package app.generated;"));
        assertTrue(java.contains("public final class GeneratedPage"));
        assertTrue(java.contains("GeneratedStyles.css()"));
        assertTrue(java.contains("private Node renderHeader()"));
        assertTrue(css.contains(".card{padding:1rem;}"));
    }

    @Test
    void reportsActionableFileAndEncodingErrors() throws Exception {
        HtmlConversionException missing = assertThrows(HtmlConversionException.class, () ->
            new ConvertCommand().run(new String[]{
                tempDir.resolve("missing.html")
                    .toString(),
                "--output",
                tempDir.resolve("Page.java")
                    .toString()
            })
        );
        assertTrue(missing.getMessage()
            .contains("Input file not found"));

        HtmlConversionException directory = assertThrows(HtmlConversionException.class, () ->
            new ConvertCommand().run(new String[]{
                tempDir.toString(),
                "--output",
                tempDir.resolve("Page.java")
                    .toString()
            })
        );
        assertTrue(directory.getMessage()
            .contains("Input path is a directory"));

        Path empty = tempDir.resolve("empty.html");
        Files.writeString(empty, "", StandardCharsets.UTF_8);
        HtmlConversionException emptyFailure = assertThrows(HtmlConversionException.class, () ->
            new ConvertCommand().run(new String[]{empty.toString(), "--output", tempDir.resolve("Page.java")
                .toString()})
        );
        assertTrue(emptyFailure.getMessage()
            .contains("Input file is empty"));

        HtmlConversionException encoding = assertThrows(HtmlConversionException.class, () ->
            new ConvertCommand().run(new String[]{"page.html", "--output", "Page.java", "--encoding", "NO_SUCH_ENCODING"})
        );
        assertTrue(encoding.getMessage()
            .contains("Unsupported encoding"));

        Path input = tempDir.resolve("page.html");
        Files.writeString(input, "<main></main>", StandardCharsets.UTF_8);
        Path outputDir = tempDir.resolve("output-dir");
        Files.createDirectories(outputDir);
        HtmlConversionException outputDirectory = assertThrows(HtmlConversionException.class, () ->
            new ConvertCommand().run(new String[]{input.toString(), "--output", outputDir.toString()})
        );
        assertTrue(outputDirectory.getMessage()
            .contains("Output path is a directory"));
    }

    @Test
    void malformedInputFailsUnlessUnsafeFallbackIsEnabled() throws Exception {
        Path input = tempDir.resolve("broken.html");
        Path output = tempDir.resolve("Page.java");
        Files.writeString(input, "<div><span>Broken</div></span>", StandardCharsets.UTF_8);

        HtmlConversionException failure = assertThrows(HtmlConversionException.class, () ->
            new ConvertCommand().run(new String[]{input.toString(), "--output", output.toString()})
        );
        assertTrue(failure.getMessage()
            .contains("Malformed HTML"));

        assertEquals(0, new ConvertCommand().run(new String[]{
            input.toString(),
            "--output",
            output.toString(),
            "--unsafe-fallback"
        }));
        assertTrue(Files.readString(output)
            .contains("unsafeHtml"));
    }

    @Test
    void rejectsReactForNow() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            new ConvertCommand().run(new String[]{"page.jsx", "--out", "Page.java", "--type", "react"})
        );

        assertTrue(exception.getMessage()
            .contains("Unsupported convert type"));
    }
}
