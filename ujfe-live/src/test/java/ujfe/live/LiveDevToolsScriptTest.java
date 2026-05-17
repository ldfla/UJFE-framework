package ujfe.live;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class LiveDevToolsScriptTest {
    @Test
    void scriptBootsFromFeatureToggleMarker() {
        String script = LiveDevToolsScript.script();

        assertTrue(script.contains("data-ujfe-dev-preview=\"true\""));
        assertTrue(script.contains("MutationObserver(sync)"));
        assertTrue(script.contains("data-ujfe-dev-control"));
    }
}
