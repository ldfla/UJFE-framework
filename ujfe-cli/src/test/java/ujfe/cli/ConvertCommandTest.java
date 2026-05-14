package ujfe.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertTrue(java.contains(".css(\"p-4\")"));
        assertTrue(java.contains("h1()"));
        assertTrue(java.contains("button()"));
    }

    @Test
    void rejectsReactForNow() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
                new ConvertCommand().run(new String[]{"page.jsx", "--out", "Page.java", "--type", "react"})
        );

        assertTrue(exception.getMessage().contains("Unsupported convert type"));
    }
}
