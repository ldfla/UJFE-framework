package ujfe.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static ujfe.core.UI.*;

final class AttributeValidationTest {

    // --- Valid platform-style attributes render normally ---

    @Test
    void acceptsStandardAttribute() {
        String html = input().attr("placeholder", "Name")
            .render();
        assertTrue(html.contains("placeholder=\"Name\""));
    }

    @Test
    void acceptsBooleanAttribute() {
        String html = input().attr("required", true)
            .render();
        assertTrue(html.contains("required"));
    }

    @Test
    void acceptsAriaAttribute() {
        String html = button("Close").attr("aria-label", "Close")
            .render();
        assertTrue(html.contains("aria-label=\"Close\""));
    }

    @Test
    void acceptsDataAttribute() {
        String html = div().attr("data-id", "123")
            .render();
        assertTrue(html.contains("data-id=\"123\""));
    }

    @Test
    void acceptsHtmxAttribute() {
        String html = div().attr("hx-get", "/fragment")
            .render();
        assertTrue(html.contains("hx-get=\"/fragment\""));
    }

    @Test
    void acceptsHtmxTriggerAttribute() {
        String html = button("Load").attr("hx-trigger", "click")
            .render();
        assertTrue(html.contains("hx-trigger=\"click\""));
    }

    @Test
    void acceptsHtmxTargetAttribute() {
        String html = div().attr("hx-target", "#container")
            .render();
        assertTrue(html.contains("hx-target=\"#container\""));
    }

    // --- Inline event handler attributes fail by default ---

    @Test
    void rejectsOnclick() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> div().attr("onclick", "alert(1)"));
        assertTrue(exception.getMessage()
            .contains("onclick"));
        assertTrue(exception.getMessage()
            .contains("blocked by default"));
    }

    @Test
    void rejectsOnload() {
        assertThrows(
            IllegalArgumentException.class,
            () -> img().attr("onload", "stealCookies()"));
    }

    @Test
    void rejectsOnerror() {
        assertThrows(
            IllegalArgumentException.class,
            () -> img().attr("onerror", "alert(1)"));
    }

    @Test
    void rejectsOnmouseover() {
        assertThrows(
            IllegalArgumentException.class,
            () -> div().attr("onmouseover", "track()"));
    }

    @Test
    void rejectsOnfocus() {
        assertThrows(
            IllegalArgumentException.class,
            () -> input().attr("onfocus", "steal()"));
    }

    @Test
    void rejectsOnclickCaseInsensitive() {
        assertThrows(
            IllegalArgumentException.class,
            () -> div().attr("onClick", "alert(1)"));
        assertThrows(
            IllegalArgumentException.class,
            () -> div().attr("ONCLICK", "alert(1)"));
        assertThrows(
            IllegalArgumentException.class,
            () -> div().attr("OnLoad", "alert(1)"));
    }

    // --- Malformed attribute names fail before rendering ---

    @Test
    void rejectsAttributeNameContainingWhitespace() {
        assertThrows(
            IllegalArgumentException.class,
            () -> div().attr("my attr", "value"));
    }

    @Test
    void rejectsAttributeNameContainingLessThan() {
        assertThrows(
            IllegalArgumentException.class,
            () -> div().attr("attr<name", "value"));
    }

    @Test
    void rejectsAttributeNameContainingEquals() {
        assertThrows(
            IllegalArgumentException.class,
            () -> div().attr("attr=name", "value"));
    }

    @Test
    void rejectsAttributeNameContainingQuote() {
        assertThrows(
            IllegalArgumentException.class,
            () -> div().attr("attr\"name", "value"));
    }

    @Test
    void rejectsEmptyAttributeName() {
        assertThrows(
            IllegalArgumentException.class,
            () -> div().attr("", "value"));
    }

    @Test
    void rejectsNullAttributeName() {
        assertThrows(
            NullPointerException.class,
            () -> div().attr(null, "value"));
    }

    // --- Generic attributes remain the primary API ---

    @Test
    void genericAttrRendersNormally() {
        String html = Element.of("section")
            .attr("data-ready", true)
            .attr("aria-expanded", "false")
            .attr("hx-swap", "innerHTML")
            .child("Content")
            .render();

        assertTrue(html.contains("data-ready"));
        assertTrue(html.contains("aria-expanded=\"false\""));
        assertTrue(html.contains("hx-swap=\"innerHTML\""));
    }

    @Test
    void typedAriaHelperRendersNormally() {
        String html = div().aria("label", "Close")
            .render();
        assertEquals("<div aria-label=\"Close\"></div>", html);
    }

    @Test
    void typedDataHelperRendersNormally() {
        String html = div().data("id", "123")
            .render();
        assertEquals("<div data-id=\"123\"></div>", html);
    }
}
