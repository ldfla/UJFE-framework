package ujfe.live;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class LiveClientScriptTest {
    @Test
    void scriptReadsOnlyPolicyAllowedClientState() {
        String script = LiveClientScript.script();

        assertTrue(script.contains("function policyKeys(attributeName)"));
        assertTrue(script.contains("data-ujfe-client-state-cookies"));
        assertTrue(script.contains("data-ujfe-local-storage-keys"));
        assertTrue(script.contains("data-ujfe-session-storage-keys"));
        assertTrue(script.contains("function readCookies()"));
        assertTrue(script.contains("readStorage('localStorage'"));
        assertTrue(script.contains("readStorage('sessionStorage'"));
        assertTrue(script.contains("cookies: readCookies()"));
    }

    @Test
    void scriptCapturesTypedLiveEventValues() {
        String script = LiveClientScript.script();

        assertTrue(script.contains("function controlValue(element)"));
        assertTrue(script.contains("type === 'checkbox'"));
        assertTrue(script.contains("type === 'radio'"));
        assertTrue(script.contains("element.multiple"));
        assertTrue(script.contains("selected.join('\\n')"));
        assertTrue(script.contains("function formValue(form)"));
        assertTrue(script.contains("encodeURIComponent(element.name)"));
        assertTrue(script.contains("value: value == null ? '' : String(value)"));
    }

    @Test
    void scriptPreventsNativeSubmitForLiveSubmitHandlers() {
        String script = LiveClientScript.script();

        assertTrue(script.contains("document.addEventListener('submit'"));
        assertTrue(script.contains("event.preventDefault();"));
        assertTrue(script.contains("data-ujfe-event-submit"));
    }
}
