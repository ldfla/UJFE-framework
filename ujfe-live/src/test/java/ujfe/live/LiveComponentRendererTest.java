package ujfe.live;

import org.junit.jupiter.api.Test;
import ujfe.core.ClientState;
import ujfe.core.ElementIdGenerator;
import ujfe.html.CssTheme;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ujfe.html.UI.button;
import static ujfe.html.UI.p;

final class LiveComponentRendererTest {
    @Test
    void constructorWithEventRegistryRendersNode() {
        LiveComponentRenderer renderer = new LiveComponentRenderer(new LiveEventRegistry());

        LiveRenderResult result = renderer.render(() -> p("Hello"), ClientState.empty());

        assertEquals("<p>Hello</p>", result.html());
    }

    @Test
    void constructorWithElementIdGeneratorUsesProvidedGenerator() {
        LiveComponentRenderer renderer = new LiveComponentRenderer(
                new LiveEventRegistry(),
                prefix -> "custom-" + prefix
        );

        LiveRenderResult result = renderer.render(
                () -> button("Go").onClick(() -> {
                }),
                ClientState.empty()
        );

        assertTrue(result.html().contains("id=\"custom-ujfe\""));
    }

    @Test
    void constructorWithThemeSupplierUsesProvidedTheme() {
        LiveComponentRenderer renderer = new LiveComponentRenderer(
                new LiveEventRegistry(),
                ElementIdGenerator.sequential(),
                () -> CssTheme.of("#ff0000", "#00ff00")
        );

        String css = renderer.renderCss(List.of("bg-primary"));

        assertTrue(css.contains("--ujfe-primary-500:#ff0000"));
        assertTrue(css.contains(".bg-primary{background-color:var(--ujfe-primary-500);}"));
    }
}
