package ujfe.cli;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

final class GeneratedJava {
    private final String pageJava;
    private final String cssJava;
    private final Path cssOutput;
    private final ConversionStats stats;
    private final List<String> warnings;

    GeneratedJava(String pageJava, String cssJava, Path cssOutput, ConversionStats stats, List<String> warnings) {
        this.pageJava = Objects.requireNonNull(pageJava, "pageJava");
        this.cssJava = cssJava;
        this.cssOutput = cssOutput;
        this.stats = Objects.requireNonNull(stats, "stats");
        this.warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings"));
    }

    String pageJava() {
        return pageJava;
    }

    boolean hasCssJava() {
        return cssJava != null;
    }

    String cssJava() {
        return cssJava;
    }

    Path cssOutput() {
        return cssOutput;
    }

    ConversionStats stats() {
        return stats;
    }

    List<String> warnings() {
        return warnings;
    }
}
