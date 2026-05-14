package ujfe.cli;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

final class HtmlParseResult {
    private final List<HtmlNode> roots;
    private final List<String> warnings;

    HtmlParseResult(List<HtmlNode> roots, List<String> warnings) {
        this.roots = List.copyOf(Objects.requireNonNull(roots, "roots"));
        this.warnings = List.copyOf(Objects.requireNonNull(warnings, "warnings"));
    }

    List<HtmlNode> roots() {
        return Collections.unmodifiableList(roots);
    }

    List<String> warnings() {
        return Collections.unmodifiableList(warnings);
    }
}
