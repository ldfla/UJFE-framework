package ujfe.core;

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

    @Test
    void rendersResponsiveAndStateVariants() {
        String css = UtilityCssRenderer.render(Set.of(
            "lg:grid-cols-4",
            "hover:bg-slate-800",
            "focus:ring-indigo-500/50",
            "placeholder:text-slate-400",
            "active:scale-[0.98]",
            "bg-white/80"
        ));

        assertTrue(css.contains("@media(min-width:1024px){.lg\\3a grid-cols-4{grid-template-columns:repeat(4,minmax(0,1fr));}}"));
        assertTrue(css.contains(".hover\\3a bg-slate-800:hover{background-color:#1e293b;}"));
        assertTrue(css.contains(".focus\\3a ring-indigo-500\\2f 50:focus{--ujfe-ring-color:rgba(99,102,241,0.5);}"));
        assertTrue(css.contains(".placeholder\\3a text-slate-400::placeholder{color:#94a3b8;}"));
        assertTrue(css.contains(".active\\3a scale-\\5b 0\\2e 98\\5d :active{transform:scale(0.98);}"));
        assertTrue(css.contains(".bg-white\\2f 80{background-color:rgba(255,255,255,0.8);}"));
    }
}
