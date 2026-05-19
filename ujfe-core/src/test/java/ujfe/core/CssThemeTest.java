package ujfe.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class CssThemeTest {
    @Test
    void createsColorVariablesFromLongAndShortHexValues() {
        CssTheme theme = CssTheme.of("#2563eb", "#0f0");

        assertEquals("#2563eb", theme.primary(500));
        assertEquals("#00ff00", theme.secondary(500));
        assertTrue(theme.renderVariables().contains("--ujfe-primary-500:#2563eb;"));
        assertTrue(theme.renderVariables().contains("--ujfe-secondary-500:#00ff00;"));
    }

    @Test
    void rejectsUnsupportedPalettesStepsAndInvalidColors() {
        CssTheme theme = CssTheme.defaultTheme();

        assertThrows(IllegalArgumentException.class, () -> theme.color("accent", 500));
        assertThrows(IllegalArgumentException.class, () -> theme.primary(25));
        assertThrows(NullPointerException.class, () -> theme.color(null, 500));
        assertThrows(NullPointerException.class, () -> CssTheme.of(null, "#fff"));
        assertThrows(IllegalArgumentException.class, () -> CssTheme.of("not-a-color", "#fff"));
    }
}
