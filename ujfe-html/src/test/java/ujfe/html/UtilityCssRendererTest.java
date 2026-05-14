package ujfe.html;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class UtilityCssRendererTest {
    @Test
    void rendersThemeVariablesAndThemeUtilities() {
        String css = UtilityCssRenderer.render(
                Set.of("bg-primary-500", "text-secondary-700", "border-primary-200"),
                CssTheme.of("#336699", "#cc3300")
        );

        assertTrue(css.contains("--ujfe-primary-500:#336699;"));
        assertTrue(css.contains("--ujfe-secondary-500:#cc3300;"));
        assertTrue(css.contains(".bg-primary-500{background-color:var(--ujfe-primary-500);}"));
        assertTrue(css.contains(".text-secondary-700{color:var(--ujfe-secondary-700);}"));
        assertTrue(css.contains(".border-primary-200{border-color:var(--ujfe-primary-200);}"));
    }

    @Test
    void rendersDynamicSpacingUtilities() {
        String css = UtilityCssRenderer.render(Set.of(
                "p-7",
                "px-9",
                "py-1.5",
                "pt-11",
                "gap-10",
                "gap-x-3",
                "gap-y-5",
                "mt-12",
                "-mb-6",
                "mx-px"
        ));

        assertTrue(css.contains(".p-7{padding:1.75rem;}"));
        assertTrue(css.contains(".px-9{padding-left:2.25rem;padding-right:2.25rem;}"));
        assertTrue(css.contains(".py-1\\2e 5{padding-top:0.375rem;padding-bottom:0.375rem;}"));
        assertTrue(css.contains(".pt-11{padding-top:2.75rem;}"));
        assertTrue(css.contains(".gap-10{gap:2.5rem;}"));
        assertTrue(css.contains(".gap-x-3{column-gap:0.75rem;}"));
        assertTrue(css.contains(".gap-y-5{row-gap:1.25rem;}"));
        assertTrue(css.contains(".mt-12{margin-top:3rem;}"));
        assertTrue(css.contains(".-mb-6{margin-bottom:-1.5rem;}"));
        assertTrue(css.contains(".mx-px{margin-left:1px;margin-right:1px;}"));
    }
}
