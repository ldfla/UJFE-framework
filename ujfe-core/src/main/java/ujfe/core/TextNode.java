package ujfe.core;

import java.util.Objects;
import java.util.function.Supplier;

public final class TextNode implements Node {
    private final Supplier<String> valueSupplier;

    public TextNode(String value) {
        this(() -> value);
    }

    public TextNode(Supplier<String> valueSupplier) {
        this.valueSupplier = Objects.requireNonNull(valueSupplier, "valueSupplier");
    }

    @Override
    public String render(UjfeContext context) {
        return HtmlEscaper.escape(valueSupplier.get());
    }
}
